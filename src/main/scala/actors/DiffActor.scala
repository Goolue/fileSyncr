package actors

import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg, ModificationDataMsg, UpdateFileMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import actors.Messages.StringPatch
import akka.actor.{Actor, ActorRef}
import com.github.difflib.DiffUtils

import scala.collection.JavaConverters._

class DiffActor(val commActor: ActorRef, val fileHandler : ActorRef) extends Actor{
  def receive() = {
    case ModificationDataMsg(path, newLines, oldLines) =>
      println("got a ModificationDataMsg")
      val patch: StringPatch = oldLines match {
        case null => DiffUtils.diff(List[String]().asJava, newLines.toList.asJava)
        case _ => DiffUtils.diff(oldLines.toList.asJava, newLines.toList.asJava)
      }
      commActor ! DiffEventMsg(path, patch)

    case ApplyPatchMsg(path, patch) =>
      println("got an ApplyPatchMsg")
      fileHandler ! GetLinesMsg(path, patch)

    case OldLinesMsg(oldLines, path, patch) =>
      println("got an OldLinesMsg")
      fileHandler ! UpdateFileMsg(path, patch.applyTo(oldLines.toList.asJava).asScala)

    case _ => println("got an unexpected msg!")
  }
}
