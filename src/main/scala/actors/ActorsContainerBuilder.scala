package actors

import java.io.{FileNotFoundException, IOException}

import better.files.File
import com.typesafe.config.{Config, ConfigFactory, ConfigValueFactory}
import utils.NetworkUtils

class ActorsContainerBuilder(private val localIp: String, private val externalIp: String,
                             private val actorSysName: Option[String] = None,
                             private val dir: Option[File] = None,
                             private val config: Option[Config] = None) {

  /**
    * get a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but actorSysName = Some(`name`).
    * @param name the name to give to the `ActorSystem` that will be created when build() is called.
    * @return a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but actorSysName = Some(`name`).
    */
  def withActorSystemName(name: String): ActorsContainerBuilder =
    new ActorsContainerBuilder(localIp, externalIp, Some(name), dir, config)

  /**
    * get a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but dir = Some(`directory`).
    * @param directory the directory the application will watch for changes in.
    *                  If directory doesn't exist or is not a directory, an exception will be thrown
    * @return a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but dir = Some(`directory`).
    */
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

  /**
    * get a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but config = Some(`conf`).
    * @param conf the `Config` to use for the `ActorSystem`
    * @return a <b>new</b> instance of `ActorsContainerBuilder` with all fields equal to this one, but config = Some(`conf`).
    */
  def withConfig(conf: Config) = new ActorsContainerBuilder(localIp, externalIp, actorSysName, dir, Some(conf))

  /**
    * Create an instance of `ActorsContainer` with parameters equal to this'.
    * @return an instance of `ActorsContainer` with parameters equal to this'.
    *         If no `actorSysName` was supplied, ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME will be used.
    *         If no `dir` was supplied, File.home will be used.
    *         If no `config` was supplied, application.conf with the addition of
    *         akka.remote.netty.tcp.bind-hostname = localIp, akka.remote.netty.tcp.hostname = externalIp will be used.
    */
  def build(): ActorsContainer = {
    implicit val actualSysName: String = actorSysName.getOrElse(ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME)
    implicit val actualDir: File = dir.filter(d => d.isDirectory).getOrElse(File.home)
    implicit val actualConfig: Config = config.getOrElse(ConfigFactory.defaultApplication()
      .withValue("akka.remote.netty.tcp.bind-hostname", ConfigValueFactory.fromAnyRef(localIp))
      .withValue("akka.remote.netty.tcp.hostname", ConfigValueFactory.fromAnyRef(externalIp)))

    new ActorsContainer(localIp, externalIp)
  }
}

object ActorsContainerBuilder {
  val DEFAULT_ACTOR_SYSTEM_NAME = "actorSystem"

  /**
    * Create the initial `ActorsContainerBuilder` with local and external IPs.
    * If cannot get the local or the external IP, an exception will be thrown.
    * @return an Instance of ActorsContainerBuilder with internal and external IPs configured.
    */
  def getInstanceWithIPs(): ActorsContainerBuilder = {
    val localIpOption = NetworkUtils.getLocalIp
    if (localIpOption.isEmpty) throw new IOException("cannot get local IP")
    val externalIpOption = NetworkUtils.getExternalIp
    if (externalIpOption.isEmpty) throw new IOException("cannot get external IP")

    val localIp = localIpOption.get
    val externalIp = externalIpOption.get

    new ActorsContainerBuilder(localIp, externalIp)
  }
}
