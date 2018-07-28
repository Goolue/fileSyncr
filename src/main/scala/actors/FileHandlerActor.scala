package actors

import java.nio.file.Path

import actors.FileHandlerActor._
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage._
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.ActorRef
import better.files.File
import better.files.File.RandomAccessMode

class FileHandlerActor(diffActor: => ActorRef, commActor: => ActorRef) extends BasicActor {

  def receive: Receive = handleMessages(Map.empty)

  def handleMessages(pathToLines: Map[Path, LinesOption]): Receive = {
    //MapQueryMsg, mostly used for testing
    case MapContainsKey(path) =>
      sender() ! pathToLines.contains(path)

    case MapContainsValue(lines) =>
      sender() ! pathToLines.values.exists(_ == lines)

    //other msgs
    case fileCreateMsg: FileCreatedMsg =>
      log.info(s"actors.FileHandlerActor got a FileCreatedMsd for path ${fileCreateMsg.file.path}")
      commActor ! fileCreateMsg

    case FileModifiedMsg(file) =>
      log.info(s"actors.FileHandlerActor got a FileModifiedMsg for path ${file.path}")
      val oldLines = pathToLines.getOrElse[LinesOption](file.path, None)
      val newLines = file.lines
      if (oldLines != newLines) {
        diffActor ! ModificationDataMsg(file.path, newLines, oldLines)
        context become handleMessages(pathToLines.updated(file.path, Some(newLines)))
      }
      else {
        log.warning(s"$getClassName got a FileModifiedMsg with no change or file $file")
      }

    case fileDeletedMsg: FileDeletedMsg =>
      log.info(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.file.path}")
      commActor ! fileDeletedMsg
      context become handleMessages(pathToLines - fileDeletedMsg.file.path)

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
      val file = File.apply(path)
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
  case class MapContainsKey(path: Path) extends MapQueryMsg
  case class MapContainsValue(lines: LinesOption) extends MapQueryMsg
}
