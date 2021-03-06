package systemTests

import actors.CommActor.AddRemoteConnectionMsg
import actors._
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.typesafe.config.ConfigFactory
import extensions.AddressExtension
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import testHelpers.Traits.MockSendingMsgsToActor
import utils.NetworkUtils

class SystemTest extends TestKit(ActorSystem("system_1",
  ActorsContainerBuilder.buildConfigWithIPs(NetworkUtils.getLocalIp.getOrElse("127.0.0.1"),
    NetworkUtils.getExternalIp.getOrElse("127.0.0.1")))) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter{

  val localhostUrl = "localhost"

  // 2nd ActorSystem
  private val system1 = system
  private val sys1Port = getPortOfSystem(system1)
  private val localIp: String = NetworkUtils.getLocalIp.get
  private val externalIp: String = NetworkUtils.getExternalIp.get
  private val config = ActorsContainerBuilder.buildConfigWithIPs(localIp, externalIp)
  private val system2 = ActorSystem("system_2", config)
  private val sys2Port = getPortOfSystem(system2)
  private val sys2Host = getHostOfSystem(system2)

  private val tempFilesDir = File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles"

  // directory for the 1st system
  private val firstDir = File.newTemporaryDirectory(parent = Some(tempFilesDir))
  val fileName = "someFile.txt"
  firstDir.createIfNotExists(asDirectory = true)
  // directory for the 2nd system
  private val secondDir = File.newTemporaryDirectory(parent = Some(tempFilesDir))
  secondDir.createIfNotExists(asDirectory = true)

  private val container1 = new ActorsContainer(system1, localhostUrl)(firstDir) with MockSendingMsgsToActor {
    override def sendMsgsToCommActor(msg: Messages.Message): Unit = {
      system1.actorSelection(system1 / ActorsContainer.COMM_ACTOR_NAME) ! msg
    }
  }
  private val container2 = new ActorsContainer(system2, localhostUrl)(secondDir) with MockSendingMsgsToActor {
    override def sendMsgsToCommActor(msg: Messages.Message): Unit = {
      system2.actorSelection(system2 / ActorsContainer.COMM_ACTOR_NAME) ! msg
    }
  }

  override def beforeAll {
    println(s"system1 running on port ${AddressExtension.portOf(system)}")
    println(s"system2 running on port ${AddressExtension.portOf(system2)}")
    println(s"firstDir is: ${firstDir.path}")
    println(s"secondDir is: ${secondDir.path}")

    firstDir.deleteOnExit()
    secondDir.deleteOnExit()

    container1.sendMsgsToCommActor(AddRemoteConnectionMsg(localIp, sys2Port, ActorsContainer.COMM_ACTOR_NAME,
      Some(system2.name)))

    Thread sleep 1000
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
    TestKit.shutdownActorSystem(system2)
    tempFilesDir.clear()
  }

  private def getPortOfSystem(sys: ActorSystem): Int = {
    AddressExtension(sys).address.port.getOrElse(-1)
  }

  private def getHostOfSystem(sys: ActorSystem): String = {
    AddressExtension(sys).address.host.getOrElse("no a valid host")
  }

  "The other file" must {
    "be created when the first file is created" in {
      val file = firstDir.createChild(fileName)
      file.deleteOnExit()

      Thread sleep 1000

      (secondDir / fileName).exists should be (true)
    }

    "be deleted when the first file is deleted" in {
      // create the file to be deleted
      val file = firstDir.createChild(fileName)

      file.delete()

      Thread sleep 2000

      (secondDir / fileName).exists should be (false)
    }

    "be modified when the first file is modified - 1 line" in {
      val file = firstDir.createChild(fileName)
      file.deleteOnExit()

      val line = "I am just a lonely line, check me out!"
      file.appendLine(line)

      val secondFile = secondDir / fileName

      Thread sleep 1000

      secondFile.lines should be (List(line))

    }
  }
}
