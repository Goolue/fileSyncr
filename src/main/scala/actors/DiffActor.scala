package actors

import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg, ModificationDataMsg, UpdateFileMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.ActorRef
import better.files.File
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
      context.system.log.info(s"got an ApplyPatchMsg for path $path")
      if (!patch.getDeltas.isEmpty) {
        fileHandler ! GetLinesMsg(path, patch)
      }
      else {
        log.warning(s"$getClassName got an empty patch in an ApplyPatchMsg for path $path")
      }

    case OldLinesMsg(oldLines, path, patch) =>
      context.system.log.info("got an OldLinesMsg")
      if (!patch.getDeltas.isEmpty) {
        fileHandler ! UpdateFileMsg(path.toString, patch.applyTo(oldLines.toList.asJava).asScala)
      }
      else {
        log.warning(s"$getClassName got an empty patch in an OldLinesMsg for path $path")
      }

    case _ => context.system.log.info("got an unexpected msg!")
  }
}
