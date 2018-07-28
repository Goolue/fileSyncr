package actors

import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg, ModificationDataMsg, UpdateFileMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{Actor, ActorRef}
import com.github.difflib.DiffUtils

import scala.collection.JavaConverters._

class DiffActor(commActor: => ActorRef, fileHandler : => ActorRef) extends BasicActor {
  def receive(): PartialFunction[Any, Unit] = {
    case ModificationDataMsg(path, newLines, oldLines) =>
      context.system.log.info("got a ModificationDataMsg")
      val oldLinesValue = oldLines.getOrElse(List())
      val patch = DiffUtils.diff(oldLinesValue.toList.asJava, newLines.toList.asJava)
      commActor ! DiffEventMsg(path, patch)

    case ApplyPatchMsg(path, patch) =>
      context.system.log.info("got an ApplyPatchMsg")
      fileHandler ! GetLinesMsg(path, patch)

    case OldLinesMsg(oldLines, path, patch) =>
      context.system.log.info("got an OldLinesMsg")
      fileHandler ! UpdateFileMsg(path, patch.applyTo(oldLines.toList.asJava).asScala)

    case _ => context.system.log.info("got an unexpected msg!")
  }
}
