import java.io.{BufferedReader, InputStreamReader}
import java.net.{NetworkInterface, SocketException, URL}

import actors.ActorsContainerBuilder
import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import extensions.AddressExtension
import utils.NetworkUtils

object Main extends App {

  override def main(args: Array[String]): Unit = {

    // get external and local IPs
//    val localIp = NetworkUtils.getLocalIp
//    val externalIp = NetworkUtils.getExternalIp
//    println(s"localIp: $localIp")
//    println(s"externalIp: $externalIp")

    // create config from application.config
//    var config = ConfigFactory.defaultApplication()
//    println(config.entrySet())
    // add bind-hostname (local hostname) and hostname (external hostname)
//    config = config.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(localIp.getOrElse("127.0.0.1")))
//      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(externalIp.getOrElse("127.0.0.1")))
//    println(s"using configurations: $config")

    ActorsContainerBuilder.create().build()

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
