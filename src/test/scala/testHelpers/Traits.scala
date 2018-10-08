package testHelpers

import actors.Messages.Message
import ui.{Input, Output}

object Traits {

  trait MockOutput extends Output {
    var outMsgs: Seq[String] = Seq()

    override def print(s: String): Unit = {
      super.print(s)
      outMsgs = outMsgs :+ s
    }
  }

  trait MockInput extends Input {
    var inMsgs: Seq[String] = Seq()
    private[this] var currIndex: Int = 0

    override def read(): String = {
      val res = inMsgs(currIndex)
      println(s"read $res")
      currIndex += 1
      res
    }
  }

  trait MockSendingMsgsToActor {
    def sendMsgsToCommActor(msg: Message): Unit
  }

}
