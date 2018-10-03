package actors

import java.nio.file.Path

import better.files.File
import com.github.difflib.patch.Patch


object Messages {

  type StringPatch = Patch[String]

  sealed trait FileEventMessage
  object FileEventMessage {
    case class FileCreatedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
    case class FileDeletedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
    case class FileModifiedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
  }

  sealed trait EventDataMessage
  object EventDataMessage {
    case class ModificationDataMsg(path: String, newLines: Traversable[String],
                                   implicit val oldLines: Option[Traversable[String]] = None) extends EventDataMessage
    case class DiffEventMsg(path: String, patch: StringPatch, isRemote: Boolean = false) extends EventDataMessage
    case class ApplyPatchMsg(path: String, patch: StringPatch) extends EventDataMessage
    case class DeleteFileMsg(path: String) extends EventDataMessage
    case class UpdateFileMsg(path: String, lines: Traversable[String]) extends EventDataMessage
  }

  sealed trait GetterMsg
  object GetterMsg {
    case class GetLinesMsg(path: String, patch: StringPatch) extends GetterMsg
    case class OldLinesMsg(lines: Traversable[String], path: String, patch: StringPatch) extends GetterMsg
  }
  //TODO: more msgs
}