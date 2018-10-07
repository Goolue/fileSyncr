package actors

import java.io.FileNotFoundException
import java.net.MalformedURLException

import actors.CommActor.AddRemoteConnectionMsg
import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File
import com.typesafe.config.Config
import extensions.AddressExtension
import utils.NetworkUtils

/**
  * A container to create and build the `ActorSystem` and `Actor`s.
  *
  * @param system           the `ActorSystem` to use.
  * @param localIp          the local (internal) IP to be used behind a NAT, Load Balancers or Docker containers.
  * @param watchedDirectory the directory to watch files in. If watchedDirectory does not exist or is not a directory,
  *                         an exception will be thrown!
  */
class ActorsContainer(val system: ActorSystem, val localIp: String)(implicit val watchedDirectory: File) {

  /**
    * A container to create and build the `ActorSystem` and `Actor`s.
    *
    * @param localIp          the local (internal) IP to be used behind a NAT, Load Balancers or Docker containers.
    *                         If the IP is invalid, an exception will be thrown.
    * @param actorSystemName  the name to give to the `ActorSystem`, defaults to `ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME`.
    * @param watchedDirectory the directory to watch files in. If watchedDirectory does not exist or is not a directory,
    *                         an exception will be thrown!
    * @param config           the `Config` to use for the `ActorSystem`.
    */
  def this(localIp: String, actorSystemName: String)(implicit config: Config, watchedDirectory: File) {
    this ({
      val system = ActorSystem(actorSystemName, config)

      val port: Option[Int] = AddressExtension(system).address.port
      val externalIp = AddressExtension(system).address.host
      if (port.isEmpty) throw new Exception(s"no port for system $system")
      system.log.info(s"Actor system '${system.name}' created. local ip: $localIp, " +
        s"port: ${port.get}, external ip: $externalIp")

      system
    }, localIp)

  }

  val externalIp: String = AddressExtension(system).address.host.get
  val systemPort: Int = AddressExtension(system).address.port.get
  private val actorSystemName = system.name

  if (!NetworkUtils.isValidIp(localIp)) throw new MalformedURLException(s"local IP $localIp is not valid!")
  if (!NetworkUtils.isValidIp(externalIp)) throw new MalformedURLException(s"external IP $externalIp is not valid!")
  if (!watchedDirectory.exists) throw new FileNotFoundException(s"$watchedDirectory does not exist!")
  if (!watchedDirectory.isDirectory) throw new Exception(s"$watchedDirectory is not a directory!")

  private val commActor = system.actorOf(Props(new CommActor(externalIp, diffActor, fileHandler)), ActorsContainer.COMM_ACTOR_NAME)
  private lazy val fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(diffActor, commActor, watchedDirectory)))
  private lazy val diffActor: ActorRef = system.actorOf(Props(new DiffActor(commActor, fileHandler)))

  def addRemoteConnection(ip: String, port: Int): Unit = {
    commActor ! AddRemoteConnectionMsg(ip, port, ActorsContainer.COMM_ACTOR_NAME, Some(actorSystemName))
  }

}


object ActorsContainer {
  // default commActor name
  val COMM_ACTOR_NAME = "commActor"
}
