package actors

import java.nio.file.Path

import actors.Messages.FileEventMessage._
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable
import FileHandlerActor._
import actors.Messages.EventDataMessage.ModificationDataMsg

class FileHandlerActor(val diffActor: ActorRef, val commActor: ActorRef) extends Actor{

//  def mapContains(path: Path): Boolean = pathToLines.contains(path)
//
//  def mapContainsValue(lines: LinesOption): Boolean = {
//    for ((_, value) <- pathToLines) if (value == lines) return true
//    false
//  }
//
//  def clearMap(): Unit = pathToLines.clear()


  //  private val pathToLines: mutable.Map[Path, LinesOption] = mutable.Map.empty

  def receive: Receive = handleMessages(mutable.Map.empty)

  def handleMessages(pathToLines: mutable.Map[Path, LinesOption]): Receive = {
    //MapQueryMsg
    case MapContainsKey(path) =>
      sender() ! pathToLines.contains(path)

    case MapContainsValue(lines) =>
      for ((_, value) <- pathToLines) if (value == lines) sender() ! true
        sender() ! false

    case ClearMap => pathToLines.clear()

    //other msgs
    case fileCreateMsg: FileCreatedMsg =>
      println(s"actors.FileHandlerActor got a FileCreatedMsd for path ${fileCreateMsg.file.path}")
      commActor ! fileCreateMsg

    case FileModifiedMsg(file) =>
      println(s"actors.FileHandlerActor got a FileModifiedMsg for path ${file.path}")
      val oldLines = pathToLines.getOrElse[LinesOption](file.path, None)
      val newLines = file.lines
      diffActor ! ModificationDataMsg(file.path, newLines, oldLines)
      pathToLines(file.path) = Some(newLines)

    case fileDeletedMsg: FileDeletedMsg =>
      println(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.file.path}")
      commActor ! fileDeletedMsg
      pathToLines -= fileDeletedMsg.file.path

    case GetLinesMsg(path, patch) =>
      println(s"actors.FileHandlerActor got a GetLinesMsg for path $path")
      val lines = pathToLines.getOrElse(path, None)
      diffActor ! OldLinesMsg(lines.getOrElse(None), path, patch)
  }
}

object FileHandlerActor {
  type LinesOption = Option[Traversable[String]]

  sealed trait MapQueryMsg
  case class MapContainsKey(path: Path) extends MapQueryMsg
  case class MapContainsValue(lines: LinesOption) extends MapQueryMsg
  case class ClearMap() extends MapQueryMsg
}
