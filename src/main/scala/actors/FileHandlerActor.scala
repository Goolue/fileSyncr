package actors

import actors.FileHandlerActor._
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage._
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.ActorRef
import akka.event.LoggingReceive
import better.files.File
import better.files.File.RandomAccessMode

/**
  * The Actor responsible for actual file manipulation (creation, deletion and modification).
  * @param diffActor an ActorRef of a DiffActor
  * @param commActor an ActorRef of a CommActor
  * @param dir the directory to handle files in, defaults to the current working directory of the app
  * @param createWatchConfigurer <b>a flag used for testing</b>. If false, no watcher will be created to alert this
  *                              Actor to file changes.
  */
class FileHandlerActor(diffActor: => ActorRef, commActor: => ActorRef, dir: File = File.currentWorkingDirectory,
                       createWatchConfigurer: Boolean = true) extends BasicActor {

  log.info(s"handling dir $dir")

  if (!dir.isDirectory) {
    throw new Exception(s"dir $dir is NOT a directory!")
  }

  // create a watcher to notify this FileHandlerActor when files are created, deleted or modified
  // for unit tests, we don't create the watcher, in that case createWatchConfigurer is false
  if (createWatchConfigurer) {
    FileWatcherConfigurer.watch(context.system, context.self, dir)
  }

  def receive: Receive = handleMessages(Map.empty, Set.empty)

  private def handleFileCreateMsg(fileCreateMsg: FileCreatedMsg, filesMonitorMap: Set[String]) : Set[String]
  = {
    def handleRemote(path: String) = {
      if (filesMonitorMap.contains(path)) {
        log.warning(s"map already contains path " +
          s"$path but received a remote FileCreatedMsg for it!")
      }
      val fileToCreate = File(dir.path.toString + s"/$path")
      if (fileToCreate.exists) {
        log.warning(s"got a FileCreatedMsg for path $path but path already exists!")
      }
      else {
        fileToCreate.createIfNotExists(createParents = true)
      }
    }

    def handleNonRemote(path: String) : Set[String] = {
      if (filesMonitorMap.contains(path)) {
        // This msg was triggered by a file creation initiated by the app.
        // remove the path from the map and ignore the msg
        return filesMonitorMap - path
      }
      else {
        commActor ! fileCreateMsg
      }
      return filesMonitorMap + path
    }

    val isRemote = fileCreateMsg.isRemote
    val path = fileCreateMsg.path
    log.info(s"actors.FileHandlerActor got a FileCreatedMsd for path $path, " +
    s"isRemote? $isRemote")

    if (isRemote) {
      handleRemote(path)
      return filesMonitorMap
    } else {
      return handleNonRemote(path)
    }
  }

  private def handleFileDeleteMsg(fileDeletedMsg: FileDeletedMsg) = {
    val isRemote = fileDeletedMsg.isRemote
    log.info(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.path}" +
      s" isRemote? $isRemote")
    if (isRemote) {
      val fileToDelete = dir / fileDeletedMsg.path
      if (!fileToDelete.exists) {
        log.warning(s"got a FileCreatedMsg for path ${fileToDelete.path} but path does not exist!")
      }
      else {
        fileToDelete.delete()
      }
    }
    else {
      commActor ! fileDeletedMsg
    }
  }

  private def handleFileModMsg(pathToLines: Map[String, LinesOption], path: String, isRemote: Boolean,
                               filesMonitorMap: Set[String]): Unit = {
    log.info(s"actors.FileHandlerActor got a FileModifiedMsg for path $path, isRemote? $isRemote")
    if (isRemote) {
      log.warning(s"got a FileModifiedMsg with isRemote = true for path $path")
    } else {
      val oldLines = pathToLines.getOrElse[LinesOption](path, None)
      val newLines = (dir / path).lines
      if (oldLines.isEmpty || oldLines.get != newLines) {
        diffActor ! ModificationDataMsg(path, newLines, oldLines)
        context become handleMessages(pathToLines.updated(path, Some(newLines)), filesMonitorMap)
      }
      else {
        log.warning(s"$getClassName got a FileModifiedMsg with no change or path $path")
      }
    }
  }

  def handleMessages(pathToLines: Map[String, LinesOption], filesMonitorMap: Set[String]): Receive = LoggingReceive {

    //MapQueryMsg, mostly used for testing
    case MapContainsKeyMsg(path) =>
      sender() ! pathToLines.contains(path)

    case MapContainsValueMsg(lines) =>
      sender() ! pathToLines.values.exists(_ == lines)

    //other msgs
    case fileCreateMsg: FileCreatedMsg =>
      handleFileCreateMsg(fileCreateMsg, filesMonitorMap)
      context become handleMessages(pathToLines, filesMonitorMap + fileCreateMsg.path)

    case FileModifiedMsg(path, isRemote) =>
      handleFileModMsg(pathToLines, path, isRemote, filesMonitorMap)

    case fileDeletedMsg: FileDeletedMsg =>
      handleFileDeleteMsg(fileDeletedMsg)
      context become handleMessages(pathToLines - fileDeletedMsg.path, filesMonitorMap)

    case GetLinesMsg(path, patch) =>
      log.info(s"actors.FileHandlerActor got a GetLinesMsg for path $path")
      if (!patch.getDeltas.isEmpty) {
        val lines = pathToLines.getOrElse(path, None)
        diffActor ! OldLinesMsg(lines.getOrElse(None), path, patch)
      }
      else {
        log.warning(s"$getClassName got an empty patch in a GetLinesMsg for path $path")
      }

    case UpdateFileMsg(path, lines) =>
      log.info(s"actors.FileHandlerActor got a UpdateFileMsg for path $path")
      //update the file
      val file = dir / path
      if (file.isDirectory) log.info(s"FileHandlerActor got an UpdateFileMsg for dir in path $path")
      else {
        file.createIfNotExists()
        file.usingLock(RandomAccessMode.readWrite)(fileChan => {
          val lock = fileChan.lock()
          file.clear()
          file.appendLines(lines.toArray: _*)
          lock.release()
        })
        //update the map
        context become handleMessages(pathToLines.updated(path, Some(lines)), filesMonitorMap)
      }

    // default case
    case msg =>
      log.warning(s"Got an unidentified msg $msg")
  }

}

object FileHandlerActor {
  type LinesOption = Option[Traversable[String]]

  sealed trait MapQueryMsg
  case class MapContainsKeyMsg(path: String) extends MapQueryMsg
  case class MapContainsValueMsg(lines: LinesOption) extends MapQueryMsg
}
