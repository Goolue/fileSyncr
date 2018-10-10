import actors.ActorsContainerBuilder
import better.files.File
import extensions.AddressExtension
import org.fourthline.cling.model.message.header.STAllHeader
import org.fourthline.cling.support.model.PortMapping
import ui.CliUIHandler
import utils.NetworkUtils

object Main extends App {

  override def main(args: Array[String]): Unit = {
//    val upnpService = NetworkUtils.getUPnPService

//    upnpService.getControlPoint.search(new STAllHeader)


    val file = {
      if (args.length == 0) File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles"
      else File.apply(args(0))
    }

//    val user = System.getProperty("user.name")
//    val nf = file.createChild("someFile.txt")
//    val perm = nf.permissions
//    val own = nf.owner
//    val ownName = nf.ownerName
//    val prov = file.fileSystem.provider()
//    nf.delete()

    val container = ActorsContainerBuilder.getInstanceWithIPs
      .withDirectory(file)
      //      .withDirectory(File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles")
      .build()


//    NetworkUtils.bindPort(upnpService, AddressExtension(container.system).address.port.get, PortMapping.Protocol.TCP)

    println(s"hostname: ${container.system.settings.config.getAnyRef("akka.remote.netty.tcp.hostname")} " +
      s"bind-hostname: ${container.system.settings.config.getAnyRef("akka.remote.netty.tcp.bind-hostname")} " +
      s"port: ${container.system.settings.config.getAnyRef("akka.remote.netty.tcp.port")} " +
      s"bind-port: ${container.system.settings.config.getAnyRef("akka.remote.netty.tcp.bind-port")} " +
      s"system port: ${AddressExtension(container.system).address.port.get}")

    val ui = new CliUIHandler(container)
    ui.displayMainScreen()
    ui.displayConnectToSomeoneScreen()

    //        // create the actor system
    //        val system = ActorSystem("actorSystem", config)
    //        println(s"Actor system ${system.name} created. hostname: ${AddressExtension(system).address.host.getOrElse("NO HOST!")}, " +
    //          s"port: ${AddressExtension(system).address.port.getOrElse("NO PORT!")}")
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
