package actors

import java.nio.file.Path

import Messages.FileEventMessage._
import actors.Messages.EventDataMessage.ModificationDataMsg
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

class FileHandlerActor(val diffActor: ActorRef, val commActor: ActorRef) extends Actor{

  type LinesOption = Option[Traversable[String]]
  private val pathToLines: mutable.Map[Path, LinesOption] = mutable.Map.empty

  def receive() = {
    case fileCreateMsg: FileCreatedMsg =>
      println(s"actors.FileHandlerActor got a FileCreatedMsd for path ${fileCreateMsg.file.path}")
      commActor ! fileCreateMsg

    case fileModifiedMsg: FileModifiedMsg =>
      println(s"actors.FileHandlerActor got a FileModifiedMsg for path ${fileModifiedMsg.file.path}")
      val oldLines = pathToLines.getOrElse[LinesOption](fileModifiedMsg.file.path, None)
      val newLines = fileModifiedMsg.file.lines
      diffActor ! ModificationDataMsg(fileModifiedMsg.file.path, newLines, oldLines)
      pathToLines(fileModifiedMsg.file.path) = Some(newLines)

    case fileDeletedMsg: FileDeletedMsg =>
      println(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.file.path}")
      commActor ! fileDeletedMsg
      pathToLines -= fileDeletedMsg.file.path

    case GetLinesMsg(path, patch) =>
      println(s"actors.FileHandlerActor got a GetLinesMsg for path $path")
      val lines = pathToLines.getOrElse(path, None)
      diffActor ! OldLinesMsg(lines.getOrElse(None), path, patch)
  }

  def mapContains(path: Path): Boolean = pathToLines.contains(path)

  def mapContainsValue(lines: LinesOption): Boolean = {
    for ((_, value) <- pathToLines) if (value == lines) return true
    false
  }

  def clearMap(): Unit = pathToLines.clear()
}
