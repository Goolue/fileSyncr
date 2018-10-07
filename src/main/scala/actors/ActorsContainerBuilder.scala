package actors

import java.io.{FileNotFoundException, IOException}

import better.files.File
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import utils.NetworkUtils

class ActorsContainerBuilder(private val localIp: String, private val externalIp: String,
                             private val actorSysName: Option[String] = None,
                             private val dir: Option[File] = None,
                             private val config: Option[Config] = None) {

  def withActorSystemName(name: String): ActorsContainerBuilder =
    new ActorsContainerBuilder(localIp, externalIp, Some(name), dir, config)

  def withDirectory(directory: File): ActorsContainerBuilder = {
    if (!directory.exists){
      throw new FileNotFoundException(s"$directory does not exist!")
    }
    else if (!directory.isDirectory) {
      throw new Exception(s"$directory is not a directory!")
    } else {
      new ActorsContainerBuilder(localIp, externalIp, actorSysName, Some(directory), config)
    }
  }

  def withConfig(conf: Config) = new ActorsContainerBuilder(localIp, externalIp, actorSysName, dir, Some(conf))

  def build(): ActorsContainer = {
    implicit val actualSysName: String = actorSysName.getOrElse("actorSystem")
    implicit val actualDir: File = dir.filter(d => d.isDirectory).getOrElse(File.home)
    implicit val actualConfig: Config = config.getOrElse(ConfigFactory.defaultApplication()
      .withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(localIp))
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(externalIp)))

    new ActorsContainer(localIp, externalIp)
  }
}

object ActorsContainerBuilder {
  def create(): ActorsContainerBuilder = {
    val localIpOption = NetworkUtils.getLocalIp
    if (localIpOption.isEmpty) throw new IOException("cannot get local IP")
    val externalIpOption = NetworkUtils.getExternalIp
    if (externalIpOption.isEmpty) throw new IOException("cannot get external IP")

    val localIp = localIpOption.get
    val externalIp = externalIpOption.get

    new ActorsContainerBuilder(localIp, externalIp)
  }
}
