package actors

import java.nio.file.Path

import better.files.File
import com.github.difflib.patch.Patch

object Messages {
  sealed trait FileEventMessage
  object FileEventMessage {
    case class FileCreatedMsg(file: File) extends FileEventMessage
    case class FileDeletedMsg(file: File) extends FileEventMessage
    case class FileModifiedMsg(file: File) extends FileEventMessage
  }

  sealed trait EventDataMessage
  object EventDataMessage {
    case class ModificationDataMsg(path: Path, newLines: Traversable[String],
                                   implicit val oldLines: Traversable[String] = null) extends EventDataMessage
    case class DiffEventMsg(path: Path, patch: Patch[String]) extends EventDataMessage
  }
  //TODO: more msgs
}