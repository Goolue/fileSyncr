package actors

import java.nio.file.Path

import actors.FileHandlerActor._
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage._
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{Actor, ActorRef}
import better.files.File
import better.files.File.RandomAccessMode

class FileHandlerActor(diffActor: => ActorRef, commActor: => ActorRef) extends Actor{

  def receive: Receive = handleMessages(Map.empty)

  def handleMessages(pathToLines: Map[Path, LinesOption]): Receive = {
    //MapQueryMsg
    case MapContainsKey(path) =>
      sender() ! pathToLines.contains(path)

    case MapContainsValue(lines) =>
      sender() ! pathToLines.values.exists(_ == lines)

    //other msgs
    case fileCreateMsg: FileCreatedMsg =>
      context.system.log.info(s"actors.FileHandlerActor got a FileCreatedMsd for path ${fileCreateMsg.file.path}")
      commActor ! fileCreateMsg

    case FileModifiedMsg(file) =>
      context.system.log.info(s"actors.FileHandlerActor got a FileModifiedMsg for path ${file.path}")
      val oldLines = pathToLines.getOrElse[LinesOption](file.path, None)
      val newLines = file.lines
      diffActor ! ModificationDataMsg(file.path, newLines, oldLines)
      context become handleMessages(pathToLines.updated(file.path, Some(newLines)))

    case fileDeletedMsg: FileDeletedMsg =>
      context.system.log.info(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.file.path}")
      commActor ! fileDeletedMsg
      context become handleMessages(pathToLines - fileDeletedMsg.file.path)

    case GetLinesMsg(path, patch) =>
      context.system.log.info(s"actors.FileHandlerActor got a GetLinesMsg for path $path")
      val lines = pathToLines.getOrElse(path, None)
      diffActor ! OldLinesMsg(lines.getOrElse(None), path, patch)

    case UpdateFileMsg(path, lines) =>
      context.system.log.info(s"actors.FileHandlerActor got a UpdateFileMsg for path $path")
      //update the file
      val file = File.apply(path)
      if (file.isDirectory) context.system.log.info(s"FileHandlerActor got an UpdateFileMsg for dir in path $path")
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


  def nanoToMilli(nano: Long): Long = nano / 1000000

  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    context.system.log.info("Elapsed time: " + nanoToMilli(t1 - t0) + "ms")
    result
  }
}

object FileHandlerActor {
  type LinesOption = Option[Traversable[String]]

  sealed trait MapQueryMsg
  case class MapContainsKey(path: Path) extends MapQueryMsg
  case class MapContainsValue(lines: LinesOption) extends MapQueryMsg
}
