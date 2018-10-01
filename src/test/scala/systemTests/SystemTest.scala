package systemTests

import actors.CommActor.AddRemoteConnectionMsg
import actors.{CommActor, DiffActor, FileHandlerActor}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import extensions.AddressExtension
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

class SystemTest extends TestKit(ActorSystem("system1")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter{

  val localhostUrl = "127.0.0.1"

  // 2nd ActorSystem
  private val system1 = system
  private val sys1Port = getPortOfSystem(system1)
  private val system2 = ActorSystem("system2")
  private val sys2Port = getPortOfSystem(system2)

  // directory for the 1st system
  private val firstDir = File.newTemporaryDirectory(parent = Some(File.currentWorkingDirectory))
  firstDir.createIfNotExists(asDirectory = true)
  // directory for the 2nd system
  private val secondDir = File.newTemporaryDirectory(parent = Some(File.currentWorkingDirectory))
  secondDir.createIfNotExists(asDirectory = true)

  // system1 actors
  private val commActor1 = system1.actorOf(Props(new CommActor(localhostUrl, diffActor1, fileHandler1)), "commActor1")
  private lazy val fileHandler1: ActorRef = system1.actorOf(Props(new FileHandlerActor(diffActor1, commActor1, firstDir)))
  private lazy val diffActor1: ActorRef = system1.actorOf(Props(new DiffActor(commActor1, fileHandler1)))


  // system2 actors
  private val commActor2 =  system2.actorOf(Props(new CommActor(localhostUrl, diffActor2, fileHandler2)), "commActor2")
  private lazy val fileHandler2: ActorRef = system2.actorOf(Props(new FileHandlerActor(diffActor2, commActor2, secondDir)), "fileHandler2")
  private lazy val diffActor2: ActorRef = system2.actorOf(Props(new DiffActor(commActor1, fileHandler2)), "diffActor2")

  override def beforeAll {
    println(s"system1 running on port ${AddressExtension.portOf(system)}")
    println(s"system2 running on port ${AddressExtension.portOf(system2)}")
    println(s"firstDir is: ${firstDir.path}")
    println(s"secondDir is: ${secondDir.path}")

    firstDir.deleteOnExit()
    secondDir.deleteOnExit()
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
    TestKit.shutdownActorSystem(system2)
  }

  private def getPortOfSystem(sys: ActorSystem): Int = {
    AddressExtension(sys).address.port.getOrElse(-1)
  }

  "The other file" must {
    "be created when the first file is created" in {
      commActor1 ! AddRemoteConnectionMsg(localhostUrl, sys2Port, commActor2.path.toStringWithoutAddress,
        Some(system2.name))

      val fileName = "someFile.txt"
      val file = firstDir.createChild(fileName)

      file.deleteOnExit()
      (secondDir / fileName).deleteOnExit()

      Thread sleep 2000

      (secondDir / fileName).exists should be (true)
    }
  }
}
