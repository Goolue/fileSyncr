package ui

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
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

  before {
    ui = new CliUIHandler(system) with MockOutput with MockInput
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

    "print ask for an IP when displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "1000")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter an IP")
    }

    "ask for a port when receiving a valid IP after displayConnectToSomeoneScreen is called" in {
      ui.inMsgs = Seq("127.0.0.1", "1000")
      ui.displayConnectToSomeoneScreen()
      ui.outMsgs should contain("Please enter a port")
    }
  }

}
