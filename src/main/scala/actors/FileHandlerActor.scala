package actors

import java.nio.file.Path

import Messages.FileEventMessage._
import actors.Messages.EventDataMessage.ModificationDataMsg
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

class FileHandlerActor(val diffActor: ActorRef, val commActor: ActorRef) extends Actor{

  private val pathToLines: mutable.Map[Path, Traversable[String]] = mutable.Map.empty

  def receive() = {
    case fileCreateMsg: FileCreatedMsg =>
      println(s"actors.FileHandlerActor got a FileCreatedMsd for path ${fileCreateMsg.file.path}")
      commActor ! fileCreateMsg

    case fileModifiedMsg: FileModifiedMsg =>
      println(s"actors.FileHandlerActor got a FileModifiedMsg for path ${fileModifiedMsg.file.path}")
      val oldLines = pathToLines.getOrElse(fileModifiedMsg.file.path, null)
      val newLines = fileModifiedMsg.file.lines
      diffActor ! ModificationDataMsg(fileModifiedMsg.file.path, newLines, oldLines)
      pathToLines(fileModifiedMsg.file.path) = newLines

    case fileDeletedMsg: FileDeletedMsg =>
      println(s"actors.FileHandlerActor got a FileDeletedMsg for path ${fileDeletedMsg.file.path}")
      commActor ! fileDeletedMsg
      pathToLines -= fileDeletedMsg.file.path
  }

  def mapContains(path: Path): Boolean = pathToLines.contains(path)

  def mapContainsValue(lines: Traversable[String]): Boolean = {
    for ((_, value) <- pathToLines) if (value.equals(lines)) return true
    false
  }

  def clearMap(): Unit = pathToLines.clear()
}
