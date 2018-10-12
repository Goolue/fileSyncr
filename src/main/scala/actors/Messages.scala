package actors

import java.nio.file.Path

import better.files.File
import com.github.difflib.patch.Patch
import entities.serialization.SerializationPatchWrapper


object Messages {

  type StringPatch = Patch[String]
  type FileLines = Traversable[String]

  trait Message

  sealed trait FileEventMessage extends Message
  object FileEventMessage {
    case class FileCreatedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
    case class FileDeletedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
    case class FileModifiedMsg(path: String, isRemote: Boolean = false) extends FileEventMessage
  }

  sealed trait EventDataMessage extends Message
  object EventDataMessage {
    case class ModificationDataMsg(path: String, newLines: Traversable[String],
                                   implicit val oldLines: Option[Traversable[String]] = None) extends EventDataMessage
    case class DiffEventMsg(path: String, patch: SerializationPatchWrapper, isRemote: Boolean = false) extends EventDataMessage
    case class ApplyPatchMsg(path: String, patch: StringPatch) extends EventDataMessage
    case class UpdateFileMsg(path: String, lines: Traversable[String]) extends EventDataMessage
  }

  sealed trait GetterMsg extends Message
  object GetterMsg {
    case class GetLinesMsg(path: String, patch: StringPatch) extends GetterMsg
    case class OldLinesMsg(lines: Traversable[String], path: String, patch: StringPatch) extends GetterMsg

    case class GetStateMsg(url: String, clearFiles: Boolean = false) extends GetterMsg
    case class StateMsg(states: Map[Path, FileLines], clearFiles: Boolean = false) extends GetterMsg
    case class ApplyStateMsg(states: Map[Path, FileLines], clearFiles: Boolean = false) extends GetterMsg
  }
  //TODO: more msgs
}