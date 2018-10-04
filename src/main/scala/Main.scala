import actors.Messages.EventDataMessage.DiffEventMsg
import actors.{CommActor, DiffActor, FileHandlerActor, FileWatcherConfigurer}
import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File
import com.github.difflib.DiffUtils
import entities.serialization.SerializationPatchWrapper
import org.nustaq.serialization.FSTConfiguration
import org.nustaq.serialization.simpleapi.DefaultCoder

import scala.collection.JavaConverters._

object Main extends App {
  override def main(args: Array[String]): Unit = {
    val lines1 = List.empty[String]
    val lines2 = List("bla bla")

    val patch = DiffUtils.diff(lines1.asJava, lines2.asJava)
    val serPatch = new SerializationPatchWrapper(patch)

    val coder = new DefaultCoder // reuse this (per thread)

    val serialized = coder.toByteArray(serPatch)

    println(serialized)
    val deserialized = coder.toObject(serialized)
  }
}
