import java.io.{BufferedReader, InputStreamReader}
import java.net.{NetworkInterface, SocketException, URL}

import actors.ActorsContainerBuilder
import akka.actor.ActorSystem
import better.files.File
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import extensions.AddressExtension
import utils.NetworkUtils

object Main extends App {

  override def main(args: Array[String]): Unit = {

    val container = ActorsContainerBuilder.getInstanceWithIPs()
      .withDirectory(File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles")
      .build()

//    // create the actor system
//    val system = ActorSystem("actorSystem", config)
//    println(s"Actor system ${system.name} created. hostname: ${AddressExtension(system).address.host.getOrElse("NO HOST!")}, " +
//      s"port: ${AddressExtension(system).address.port.getOrElse("NO PORT!")}")
//
//    // TODO start ui here
//
//    //    val lines1 = List.empty[String]
//    //    val lines2 = List("bla bla")
//    //
//    //    val patch = DiffUtils.diff(lines1.asJava, lines2.asJava)
//    //    val serPatch = new SerializationPatchWrapper(patch)
//    //
//    //    val coder = new DefaultCoder // reuse this (per thread)
//    //
//    //    val serialized = coder.toByteArray(serPatch)
//    //
//    //    println(serialized)
//    //    val deserialized = coder.toObject(serialized)
  }
}
