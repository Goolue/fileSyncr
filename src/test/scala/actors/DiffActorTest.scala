package actors

import java.nio.file.Path

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

  var diffActor: TestActorRef[DiffActor] = _
  var newLines: List[String] = _
  var path: Path = _
  var probe: TestProbe = _

  before {
    val (diffActor, newLines, path, probe) = createDiffActorPathNewLinesAndProbe
    this.diffActor = diffActor
    this.newLines = newLines
    this.path = path
    this.probe = probe
  }

  after {
    diffActor = null
    newLines = null
    path = null
    probe = null
  }

  private def createDiffActorPathNewLinesAndProbe = {
    val diffActor = TestActorRef[DiffActor](Props(new DiffActor(testActor, testActor)))
    val newLines = List("first line", "second line", "third line")
    val path = File.home.path
    val probe = new TestProbe(system)
    (diffActor, newLines, path, probe)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "A DiffActor" must {
    "send a DiffEventMsg(path, newLines added) when receiving a ModificationDataMsg(path, newLines, null)" in {
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

  it must {
    "send a DiffEventMsg(path, diff) when receiving a ModificationDataMsg(path, newLines, oldLines != null)" in {
      val oldLines = List("first line", "second line", "Im a different third line", "forth line")
      val modMsg = ModificationDataMsg(path, newLines, oldLines)
      diffActor.tell(modMsg, probe.ref)

      val patch: Patch[String] = DiffUtils.diff(oldLines.asJava, newLines.asJava)
      val diffMsg = DiffEventMsg(path, patch)

      //needs to be done this way because Patch.equals is not good
      expectMsgPF(Duration.Undefined) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath.equals(path) && receivedNewLines.equals(newLines.asJava) && receivedOldLines.equals(oldLines)
        case _ => false
      }
    }
  }

  //TODO continue here
}
