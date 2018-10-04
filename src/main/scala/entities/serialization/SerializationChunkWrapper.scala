package entities.serialization

import com.github.difflib.patch.Chunk
import scala.collection.JavaConverters._

class SerializationChunkWrapper(val position: Int, val lines: List[String]) extends Serializable {

  def this(chunk: Chunk[String]) {
    this(chunk.getPosition, chunk.getLines.asScala.toList)
  }

  def toChunk: Chunk[String] = {
    new Chunk[String](position, lines.asJava)
  }

  override def toString: String = s"[position: $position, lines: $lines]"
}
