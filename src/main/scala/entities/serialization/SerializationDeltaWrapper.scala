package entities.serialization

import com.github.difflib.patch._

class SerializationDeltaWrapper(val deltaType: DeltaType, val original: SerializationChunkWrapper,
                                val revised: SerializationChunkWrapper) extends Serializable {

  def this(delta: Delta[String]) {
    this(delta.getType , new SerializationChunkWrapper(delta.getOriginal),
      new SerializationChunkWrapper(delta.getRevised))

  }

  def toDelta: Option[Delta[String]] = {
    deltaType match {
      case DeltaType.CHANGE => Some(new ChangeDelta[String](original.toChunk, revised.toChunk))
      case DeltaType.DELETE => Some(new DeleteDelta[String](original.toChunk, revised.toChunk))
      case DeltaType.INSERT => Some(new InsertDelta[String](original.toChunk, revised.toChunk))
      case _ =>
        println(s"ERROR! unidentifieable deltaType $deltaType")
        None
    }
  }

  override def toString: String = s"[type: $deltaType, original: $original, revised: $revised]"
}
