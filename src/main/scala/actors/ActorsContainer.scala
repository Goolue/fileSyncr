package actors

import java.io.FileNotFoundException
import java.net.MalformedURLException

import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File
import com.typesafe.config.Config
import extensions.AddressExtension
import utils.NetworkUtils

/**
  * A container to create and build the `ActorSystem` and `Actor`s.
  * @param localIp the local (internal) IP to be used behind a NAT, Load Balancers or Docker containers.
  *                If the IP is invalid, an exception will be thrown.
  * @param externalIp the external IP
  *                If the IP is invalid, an exception will be thrown.
  * @param actorSystemName the name to give to the `ActorSystem`, defaults to `ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME`.
  * @param watchedDirectory the directory to watch files in. If watchedDirectory does not exist or is not a directory,
  *                         an exception will be thrown!
  * @param config the `Config` to use for the `ActorSystem`.
  */
class ActorsContainer(val localIp: String, val externalIp: String)(implicit private val actorSystemName: String,
                                                                   implicit val watchedDirectory: File,
                                                                   implicit private val config: Config) {

  if (!NetworkUtils.isValidIp(localIp)) throw new MalformedURLException(s"local IP $localIp is not valid!")
  if (!NetworkUtils.isValidIp(externalIp)) throw new MalformedURLException(s"external IP $externalIp is not valid!")
  if (!watchedDirectory.exists) throw new FileNotFoundException(s"$watchedDirectory does not exist!")
  if (!watchedDirectory.isDirectory) throw new Exception(s"$watchedDirectory is not a directory!")

  val system: ActorSystem = createActorSystem
  val systemPort: Int = AddressExtension(system).address.port.get

  private val commActor = system.actorOf(Props(new CommActor(externalIp, diffActor, fileHandler)), ActorsContainer.COMM_ACTOR_NAME)
  private lazy val fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(diffActor, commActor, watchedDirectory)))
  private lazy val diffActor: ActorRef = system.actorOf(Props(new DiffActor(commActor, fileHandler)))

  private def createActorSystem: ActorSystem = {
    val system = ActorSystem(actorSystemName, config)

    val port: Option[Int] = AddressExtension(system).address.port
    if (port.isEmpty) throw new Exception(s"no port for system $system")
    system.log.info(s"Actor system '${system.name}' created. local ip: $localIp, " +
      s"port: ${port.get}, external ip: $externalIp")

    system
  }

}


object ActorsContainer {
  // default commActor name
  val COMM_ACTOR_NAME = "commActor"
}
