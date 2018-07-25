package actors

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg, ModificationDataMsg, UpdateFileMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import actors.Messages.StringPatch
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

  private val NUM_SECS_TO_WAIT = 3

  private var diffActor: TestActorRef[DiffActor] = _
  private var newLines: List[String] = _
  private var path: Path = _
  private var probe: TestProbe = _

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
      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath == path && receivedNewLines == newLines.asJava && receivedOldLines.isEmpty
        case _ => false
      }
    }
  }

  it must {
    "send a DiffEventMsg(path, diff) when receiving a ModificationDataMsg(path, newLines, oldLines != null)" in {
      val oldLines = List("first line", "second line", "Im a different third line", "forth line")
      val modMsg = ModificationDataMsg(path, newLines, Some(oldLines))
      diffActor.tell(modMsg, probe.ref)

      val patch: Patch[String] = DiffUtils.diff(oldLines.asJava, newLines.asJava)
      val diffMsg = DiffEventMsg(path, patch)

      //needs to be done this way because Patch.equals is not good
      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath == path && receivedNewLines == newLines.asJava &&
            receivedOldLines.getOrElse(None) == oldLines
        case _ => false
      }
    }
  }

  it must {
    "send a GetLinesMsg(path, patch) when receiving an ApplyPatchMsg(path, patch)" in {
      val patch: StringPatch = DiffUtils.diff(List[String]().asJava, newLines.asJava)
      val applyPatchMsg = ApplyPatchMsg(path, patch)

      diffActor.tell(applyPatchMsg, probe.ref)

      val getterMsg = GetLinesMsg(path, patch)

      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case GetLinesMsg(expectedPath, expectedPatch) =>
          expectedPath == path && expectedPatch == patch
        case _ => false
      }
    }
  }

  it must {
    "send a UpdateFileMsg(path, patch(lines)) when receiving an OldLinesMsg(lines, path, patch)" in {
      val oldLines = List[String]("first line", "some other line")
      val patch: StringPatch = DiffUtils.diff(oldLines.asJava, newLines.asJava)
      val oldLinesMsg = OldLinesMsg(oldLines, path, patch)

      diffActor.tell(oldLinesMsg, probe.ref)

      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case UpdateFileMsg(expectedPath, expectedLines) =>
          expectedPath == path && expectedLines == newLines
        case _ => false
      }
    }
  }

  //TODO continue here
}
