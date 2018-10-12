package ui

import actors.CommActor.HasConnectionQuery
import actors.{ActorsContainer, ActorsContainerBuilder}
import actors.Messages.Message
import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.typesafe.config.Config
import org.scalatest._
import testHelpers.Traits.{MockInput, MockOutput, MockSendingMsgsToActor}
import utils.NetworkUtils

class CliUIHandlerTest extends TestKit(ActorSystem("system1")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  private var ui: CliUIHandler with MockOutput with MockInput= _

  private val localIp = NetworkUtils.getLocalIp.get
  private val externalIp = NetworkUtils.getExternalIp.get

  private implicit val config: Config = ActorsContainerBuilder.buildConfigWithIPs(localIp, externalIp)
  private implicit val dir: File = File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles"
  private val container = new ActorsContainer(NetworkUtils.getLocalIp.get, "testSystem") with MockSendingMsgsToActor {
    override def sendMsgsToCommActor(msg: Message): Unit = {
      system.actorSelection(system / ActorsContainer.COMM_ACTOR_NAME) ! msg
    }
  }

  before {
    ui = new CliUIHandler(container) with MockOutput with MockInput
  }

  after {
    ui = null
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "CliUIHandler" must {
    "print Hello when displayMainScreen is called" in {
      ui.displayMainScreen()
      ui.outMsgs should contain("Hello!")
    }

    "ask for an IP when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "1000", "123456789")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter an IP")
    }

    "ask for a port when receiving a valid IP after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "1000", "123456789")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter a port")
    }

    def testInvalidIP = {
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Invalid IP, please try again")
    }

    "ask again for IP when entering an invalid IP (with ' ') when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("some ip","127.0.0.1", "1000", "123456789")
      testInvalidIP
    }

    "ask again for IP when entering an invalid IP (too long number) when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0000.1", "127.0.0.1", "1000", "123456789")
      testInvalidIP
    }

    "ask again for IP when entering an invalid IP (with '..') when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0..1", "127.0.0.1", "1000", "123456789")
      testInvalidIP
    }


    "ask again for IP when entering an invalid IP (too many numbers) when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1.3", "127.0.0.1", "1000", "123456789")
      testInvalidIP
    }

    def testInvalidPort = {
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Invalid port, please try again")
    }

    "ask again for a port when receiving an invalid port (not number) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "a", "1000", "123456789")
      testInvalidPort
    }


    "ask again for a port when receiving an invalid port (neg num) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "-1000", "1000", "123456789")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (0) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "0", "1000", "123456789")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (float) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "12.5", "1000", "123456789")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (too big num) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "100000", "1000", "123456789")
      testInvalidPort
    }

    // TODO does not pass when running all tests (not just this file) together
    "add the remote connection for the IP when displayConnectToSomeoneScreen is called" in {
      val ip = "127.0.0.1"
      ui.inMsgs = Seq(ip, "1000", "123456789")
      ui.displayConnectToSomeoneScreen()
      container.sendMsgsToCommActor(HasConnectionQuery(ip))

      expectMsg(true)
    }
  }

}
