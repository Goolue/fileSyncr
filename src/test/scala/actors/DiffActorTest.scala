package actors

import actors.Messages.EventDataMessage.{DiffEventMsg, ModificationDataMsg}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import better.files.File
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import org.scalatest._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

class DiffActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  before {

  }

  after {

  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A DiffActor" must {
    "send a DiffEventMsg(path, newLines added) when receiving a ModificationDataMsg(path, newLines, null)" in {
      val diffActor = TestActorRef[DiffActor](Props(new DiffActor(testActor, testActor)))
      val newLines = List("first line", "second line", "third line")
      val path = File.home.path
      val probe = new TestProbe(system)

      val modMsg = ModificationDataMsg(path, newLines)
      diffActor.tell(modMsg, probe.ref)

      val patch: Patch[String] = DiffUtils.diff(List[String]().asJava, newLines.asJava)
      val diffMsg = DiffEventMsg(path, patch)

      //needs to be done this way because Patch.equals is not good
      expectMsgPF(Duration.Undefined) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath.equals(path) && receivedNewLines.equals(newLines.asJava) && receivedOldLines == null
        case _ => false
      }
    }
  }

}
