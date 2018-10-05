import java.io.{BufferedReader, InputStreamReader}
import java.net.{NetworkInterface, SocketException, URL}

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import extensions.AddressExtension
import utils.NetworkUtils

object Main extends App {

  override def main(args: Array[String]): Unit = {

    val localIp = NetworkUtils.getLocalIp
    println(s"localIp: $localIp")
    val externalIp = NetworkUtils.getExternalIp
    println(s"externalIp: $externalIp")


    //    val localhost: InetAddress = InetAddress.getLocalHost
    //    val localIpAddress: String = localhost.getHostAddress
    //
    //    println(s"localIpAddress = $localIpAddress")
    //
    //    println(ipAddress())

    var conf = ConfigFactory.defaultApplication()
    println(conf.entrySet())

    //    conf = conf.withValue("akka.remote.netty.tcp.bind-port", ConfigValueFactory.fromAnyRef(0))
    conf = conf.withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(localIp.getOrElse("127.0.0.1")))
    conf = conf.withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(externalIp.getOrElse("127.0.0.1")))
    println(conf.entrySet())

    val system = ActorSystem("sys", conf)
    println(AddressExtension(system).address.port.getOrElse(-1))
    println(AddressExtension(system).address.host.getOrElse(-1))


    //    val lines1 = List.empty[String]
    //    val lines2 = List("bla bla")
    //
    //    val patch = DiffUtils.diff(lines1.asJava, lines2.asJava)
    //    val serPatch = new SerializationPatchWrapper(patch)
    //
    //    val coder = new DefaultCoder // reuse this (per thread)
    //
    //    val serialized = coder.toByteArray(serPatch)
    //
    //    println(serialized)
    //    val deserialized = coder.toObject(serialized)
  }
}
