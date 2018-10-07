package actors

import java.io.FileNotFoundException
import java.net.MalformedURLException

import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File
import com.typesafe.config.Config
import extensions.AddressExtension
import utils.NetworkUtils

//class ActorsContainer(val localIp: String, val externalIp: String, private val actorSystemName: String = "actorSystem",
//                      val watchedDirectory: File = File.currentWorkingDirectory,
//                      private val config: Config = ConfigFactory.defaultApplication()){
 class ActorsContainer(val localIp: String, val externalIp: String)(implicit private val actorSystemName: String,
                                                                    implicit val watchedDirectory: File,
                                                                    implicit private val config: Config) {

  if (!NetworkUtils.isValidIp(localIp)) throw new MalformedURLException(s"local IP $localIp is not valid!")
  if (!NetworkUtils.isValidIp(externalIp)) throw new MalformedURLException(s"external IP $externalIp is not valid!")
  if (!watchedDirectory.exists) throw new FileNotFoundException(s"$watchedDirectory does not exist!")
  if (!watchedDirectory.isDirectory) throw new Exception(s"$watchedDirectory is not a directory!")

  val system: ActorSystem = createActorSystem
  val port: Int = AddressExtension(system).address.port.get
  val COMM_ACTOR_NAME = "commActor"
  private val commActor = system.actorOf(Props(new CommActor(externalIp, diffActor, fileHandler)), COMM_ACTOR_NAME)
  private lazy val fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(diffActor, commActor, watchedDirectory)))
  private lazy val diffActor: ActorRef = system.actorOf(Props(new DiffActor(commActor, fileHandler)))

  private def createActorSystem: ActorSystem = {
    // create the actor system
    val system = ActorSystem(actorSystemName, config)

    val port: Option[Int] = AddressExtension(system).address.port
    if (port.isEmpty) throw new Exception(s"no port for system $system")
    system.log.info(s"Actor system '${system.name}' created. local ip: $localIp, " +
      s"port: ${port.get}, external ip: $externalIp")

    system
  }

}


//object ActorsContainer {
//  private var localIp: String = _
//  private var externalIp: String = _
//
//  def create(): ActorsContainer = {
//    val localIpOption = NetworkUtils.getLocalIp
//    if (localIpOption.isEmpty) throw new IOException("cannot get local IP")
//    val externalIpOption = NetworkUtils.getExternalIp
//    if (externalIpOption.isEmpty) throw new IOException("cannot get external IP")
//
//    localIp = localIpOption.get
//    externalIp = externalIpOption.get
//  }
//}
