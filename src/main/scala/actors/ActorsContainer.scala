package actors

import java.io.FileNotFoundException
import java.net.MalformedURLException

import actors.CommActor.AddRemoteConnectionMsg
import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File
import com.typesafe.config.Config
import extensions.AddressExtension
import utils.NetworkUtils

import scala.util.Random

/**
  * A container to create and build the [[ActorSystem]] and [[akka.actor.Actor]]s.
  *
  * @param system           the [[ActorSystem]] to use. The system's name must be of the form <x>_<non-negative integer>
  *                           where x does not contain '_'
  * @param localIp          the local (internal) IP to be used behind a NAT, Load Balancers or Docker containers.
  * @param watchedDirectory the directory to watch files in. If watchedDirectory does not exist or is not a directory,
  *                         an exception will be thrown!
  */
class ActorsContainer(val system: ActorSystem, val localIp: String)
                     (implicit val watchedDirectory: File) {

  /**
    * A container to create and build the `ActorSystem` and `Actor`s.
    *
    * @param localIp          the local (internal) IP to be used behind a NAT, Load Balancers or Docker containers.
    *                         If the IP is invalid, an exception will be thrown.
    * @param randomNum        a random integer that will be added to the `actorSystemName` in order to create a unique name
    * @param actorSystemName  the name to give to the [[ActorSystem]], defaults to [[ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME]].
    *                         The actual name given to the system will be like <actorSystemName>_<random int>
    * @param watchedDirectory the directory to watch files in. If watchedDirectory does not exist or is not a directory,
    *                         an exception will be thrown!
    * @param config           the `Config` to use for the [[ActorSystem]].
    */
  def this(localIp: String, actorSystemName: String, randomNum: Int = Random.nextInt())
          (implicit config: Config, watchedDirectory: File) {
    this ({
      val systemNameWithRandNum = s"${actorSystemName}_${randomNum.abs}"
      val system = ActorSystem(systemNameWithRandNum , config)

      val port: Option[Int] = AddressExtension(system).address.port
      val externalIp = AddressExtension(system).address.host
      if (port.isEmpty) throw new Exception(s"no port for system $system")
      system.log.info(s"Actor system '${system.name}' created. local ip: $localIp, " +
        s"port: ${port.get}, external ip: $externalIp")

      system
    }, localIp)

  }

  // this line will throw an exception if system.name is not of the proper format
  val systemNum: Int = system.name.split("_")(1).toInt

  val externalIp: String = AddressExtension(system).address.host.get
  val systemPort: Int = AddressExtension(system).address.port.get

  if (!NetworkUtils.isValidIp(localIp)) throw new MalformedURLException(s"local IP $localIp is not valid!")
  if (!NetworkUtils.isValidIp(externalIp)) throw new MalformedURLException(s"external IP $externalIp is not valid!")
  if (!watchedDirectory.exists) throw new FileNotFoundException(s"$watchedDirectory does not exist!")
  if (!watchedDirectory.isDirectory) throw new Exception(s"$watchedDirectory is not a directory!")

  // create a DeadLettersActor to subscribe to DeadLetters
  private val deadLettersActor =  system.actorOf(Props[DeadLettersActor], "deadLettersActor")

  private val commActor = system.actorOf(Props(new CommActor(externalIp, diffActor, fileHandler)), ActorsContainer.COMM_ACTOR_NAME)
  private lazy val fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(diffActor, commActor, watchedDirectory)))
  private lazy val diffActor: ActorRef = system.actorOf(Props(new DiffActor(commActor, fileHandler)))

  def addRemoteConnection(ip: String, port: Int, systemNumber: Int): Unit = {
    commActor ! AddRemoteConnectionMsg(ip, port, ActorsContainer.COMM_ACTOR_NAME,
      Some(s"${ActorsContainerBuilder.DEFAULT_ACTOR_SYSTEM_NAME}_$systemNumber"))
  }
}


object ActorsContainer {
  val COMM_ACTOR_NAME = s"commActor"
  val FILE_HANDLER_NAME = s"fileHandler"
  val DIFF_ACTOR_NAME = s"diffActor"
}
