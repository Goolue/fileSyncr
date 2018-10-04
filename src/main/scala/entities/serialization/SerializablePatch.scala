package entities.serialization

import com.github.difflib.patch.Patch
import scala.collection.JavaConverters._

class SerializablePatch(val deltas: List[SerializableDelta]) extends Serializable {

  def this(patch: Patch[String]) {
    this(patch.getDeltas.asScala.map(d => new SerializableDelta(d)))
  }

  def toPatch: Patch[String] = {
    val patch = new Patch[String](deltas.size)
    deltas.foreach(d => patch.addDelta(d.toDelta))
    patch
  }

}
