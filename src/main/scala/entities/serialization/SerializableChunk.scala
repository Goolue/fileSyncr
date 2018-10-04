package entities.serialization

import com.github.difflib.patch.Chunk
import scala.collection.JavaConverters._

class SerializableChunk(val position: Int, val lines: List[String]) extends Serializable {

  def this(chunk: Chunk[String]) {
    this(chunk.getPosition, chunk.getLines.asScala)
  }

  def toChunk: Chunk[String] = {
    new Chunk[String](position, lines.asJava)
  }

}
