package actors

import java.nio.file.Path

import actors.FileHandlerActor._
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage._
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.ActorRef
import better.files.File
import better.files.File.RandomAccessMode
import javax.swing.event.DocumentEvent.EventType

import scala.collection.mutable

/**
  * //TODO
  * @param diffActor
  * @param commActor
  * @param dir
  * @param createWatchConfigurer
  */
class FileHandlerActor(diffActor: => ActorRef, commActor: => ActorRef, dir: File = File.currentWorkingDirectory,
                       createWatchConfigurer: Boolean = true) extends BasicActor {

  log.info(s"handling dir $dir")

  if (!dir.isDirectory) log.error(s"dir $dir is NOT a directory!")

  // create a watcher to notify this FileHandlerActor when files are created, deleted or modified
  // for unit tests, we don't create the watcher, in that case createWatchConfigurer is false
  if (createWatchConfigurer) {
    FileWatcherConfigurer.watch(context.system, context.self, dir)
  }

  // TODO to val
  private var filesMonitorMap = mutable.Map.empty[String, EventType]

  def receive: Receive = handleMessages(Map.empty)

  private def handleFileCreateMsg(fileCreateMsg: FileCreatedMsg) = {
    def handleRemote(path: String) = {
      if (filesMonitorMap.contains(path)) {
        log.warning(s"map already contains value ${filesMonitorMap.get(path)} for path " +
          s"$path but received a remote FileCreatedMsg for it!")
      }
      log.info(s"fileToWatchMonitor key, vals: ${filesMonitorMap.toList}")
      filesMonitorMap.update(path, EventType.INSERT)
      val fileToCreate = File(dir.path.toString + s"/$path")
      if (fileToCreate.exists) {
        log.warning(s"got a FileCreatedMsg for path $path but path already exists!")
      }
      else {
        fileToCreate.createIfNotExists(createParents = true)
      }
    }

    def handleNonRemote(path: String) = {
      if (filesMonitorMap.contains(path)) {
        if (filesMonitorMap(path) == EventType.INSERT) {
          filesMonitorMap -= path
        }
        else {
          log.warning(s"got a non-remote FileCreatedMsg for path $path but map had " +
            s"value ${filesMonitorMap(path)}")
          commActor ! fileCreateMsg
        }
      }
      else {
        commActor ! fileCreateMsg
      }
    }

    val isRemote = fileCreateMsg.isRemote
    val path = fileCreateMsg.path
    log.info(s"actors.FileHandlerActor got a FileCreatedMsd for path $path, " +
    s"isRemote? $isRemote")

    if (isRemote) {
      handleRemote(path)

    } else {
      handleNonRemote(path)
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

  private def handleFileModMsg(pathToLines: Map[String, LinesOption], path: String, isRemote: Boolean): Unit = {
    log.info(s"actors.FileHandlerActor got a FileModifiedMsg for path $path, isRemote? $isRemote")
    if (isRemote) {
      log.warning(s"got a FileModifiedMsg with isRemote = true for path $path")
    } else {
      val oldLines = pathToLines.getOrElse[LinesOption](path, None)
      val newLines = (dir / path).lines
      if (oldLines.isEmpty || oldLines.get != newLines) {
        diffActor ! ModificationDataMsg(path, newLines, oldLines)
        context become handleMessages(pathToLines.updated(path, Some(newLines)))
      }
      else {
        log.warning(s"$getClassName got a FileModifiedMsg with no change or path $path")
      }
    }
  }

  def handleMessages(pathToLines: Map[String, LinesOption]): Receive = {

    //MapQueryMsg, mostly used for testing
    case MapContainsKeyMsg(path) =>
      sender() ! pathToLines.contains(path)

    case MapContainsValueMsg(lines) =>
      sender() ! pathToLines.values.exists(_ == lines)

    //other msgs
    case fileCreateMsg: FileCreatedMsg =>
      handleFileCreateMsg(fileCreateMsg)

    case FileModifiedMsg(path, isRemote) =>
      handleFileModMsg(pathToLines, path, isRemote)

    case fileDeletedMsg: FileDeletedMsg =>
      handleFileDeleteMsg(fileDeletedMsg)
      context become handleMessages(pathToLines - fileDeletedMsg.path)

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
        context become handleMessages(pathToLines.updated(path, Some(lines)))
      }
  }

}

object FileHandlerActor {
  type LinesOption = Option[Traversable[String]]

  sealed trait MapQueryMsg
  case class MapContainsKeyMsg(path: String) extends MapQueryMsg
  case class MapContainsValueMsg(lines: LinesOption) extends MapQueryMsg
}
