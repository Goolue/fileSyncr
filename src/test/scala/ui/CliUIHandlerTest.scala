package ui

import actors.ActorsContainerBuilder
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import org.scalatest._

class CliUIHandlerTest extends TestKit(ActorSystem("system1")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  private trait MockOutput extends Output {
    var outMsgs: Seq[String] = Seq()

    override def print(s: String): Unit = {
      super.print(s)
      outMsgs = outMsgs :+ s
    }
  }

  private trait MockInput extends Input {
    var inMsgs: Seq[String] = Seq()
    private[this] var currIndex: Int = 0

    override def read(): String = {
      val res = inMsgs(currIndex)
      println(s"read $res")
      currIndex += 1
      res
    }
  }

  private var ui: CliUIHandler with MockOutput with MockInput= _
  private val container = ActorsContainerBuilder.getInstanceWithIPs
    .withDirectory(File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles")
    .build()

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
      ui.inMsgs = Seq("127.0.0.1", "1000")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter an IP")
    }

    "ask for a port when receiving a valid IP after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "1000")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter a port")
    }

    def testInvalidIP = {
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Invalid IP, please try again")
    }

    "ask again for IP when entering an invalid IP (with ' ') when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("some ip","127.0.0.1", "1000")
      testInvalidIP
    }

    "ask again for IP when entering an invalid IP (too long number) when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0000.1", "127.0.0.1", "1000")
      testInvalidIP
    }

    "ask again for IP when entering an invalid IP (with '..') when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0..1", "127.0.0.1", "1000")
      testInvalidIP
    }


    "ask again for IP when entering an invalid IP (too many numbers) when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1.3", "127.0.0.1", "1000")
      testInvalidIP
    }

    def testInvalidPort = {
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Invalid port, please try again")
    }

    "ask again for a port when receiving an invalid port (not number) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "a", "1000")
      testInvalidPort
    }


    "ask again for a port when receiving an invalid port (neg num) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "-1000", "1000")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (0) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "0", "1000")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (float) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "12.5", "1000")
      testInvalidPort
    }

    "ask again for a port when receiving an invalid port (too big num) after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "100000", "1000")
      testInvalidPort
    }

//    "add the remote connection for the IP when displayConnectToSomeoneScreen is called" in {
//      ui.inMsgs = Seq("127.0.0.1", "1000")
//
//    }
  }

}
