package entities.serialization

import com.github.difflib.patch._

class SerializableDelta(val deltaType: DeltaType, val original: SerializableChunk,
                                 val revised: SerializableChunk) extends Serializable {

  def this(delta: Delta[String]) {
    this(delta match {
      case ChangeDelta => DeltaType.CHANGE
      case DeleteDelta => DeltaType.DELETE
      case InsertDelta => DeltaType.INSERT
    },
      new SerializableChunk(delta.getOriginal), new SerializableChunk(delta.getRevised))

  }

  def toDelta: Delta[String] = {
    deltaType match {
      case DeltaType.CHANGE => new ChangeDelta[String](original.toChunk, revised.toChunk)
      case DeltaType.DELETE => new DeleteDelta[String](original.toChunk, revised.toChunk)
      case DeltaType.INSERT => new InsertDelta[String](original.toChunk, revised.toChunk)
    }
  }
}
