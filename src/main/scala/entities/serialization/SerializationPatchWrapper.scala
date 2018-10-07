package entities.serialization

import com.github.difflib.patch.Patch
import scala.collection.JavaConverters._

class SerializationPatchWrapper(val deltas: List[SerializationDeltaWrapper]) extends Serializable {

  def this(patch: Patch[String]) {
    this(patch.getDeltas.asScala.map(d => new SerializationDeltaWrapper(d)).toList)
  }

  def toPatch: Patch[String] = {
    val patch = new Patch[String](deltas.size)
    deltas
      .map(d => d.toDelta)
      .filter(d => d.isDefined)
      .map(d => d.get)
      .foreach(d => patch.addDelta(d))
    patch
  }

  override def toString: String = s"[deltas: $deltas]"
}
