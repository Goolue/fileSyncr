package actors

import java.nio.file.Path

import better.files.File
import com.github.difflib.patch.Patch


object Messages {

  type StringPatch = Patch[String]

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
    case class DiffEventMsg(path: Path, patch: StringPatch) extends EventDataMessage
    case class ApplyPatchMsg(path: Path, patch: StringPatch) extends EventDataMessage
    case class UpdateFileMsg(path: Path, lines: Traversable[String]) extends EventDataMessage
  }

  sealed trait GetterMsg
  object GetterMsg {
    case class GetLinesMsg(path: Path, patch: StringPatch) extends GetterMsg
    case class OldLinesMsg(lines: Traversable[String], path: Path, patch: StringPatch) extends GetterMsg
  }
  //TODO: more msgs
}