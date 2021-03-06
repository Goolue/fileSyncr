package actors

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg, ModificationDataMsg, UpdateFileMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import actors.Messages.StringPatch
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import better.files.File
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import entities.serialization.SerializationPatchWrapper
import org.scalatest._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

class DiffActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  private val NUM_SECS_TO_WAIT = 3

  private var diffActor: TestActorRef[DiffActor] = _
  private var newLines: List[String] = _
  private var path: Path = _

  before {
    val (diffActor, newLines, path) = createDiffActorPathNewLinesAndProbe
    this.diffActor = diffActor
    this.newLines = newLines
    this.path = path
  }

  after {
    diffActor = null
    newLines = null
    path = null
  }

  private def createDiffActorPathNewLinesAndProbe = {
    val diffActor = TestActorRef[DiffActor](Props(new DiffActor(testActor, testActor)))
    val newLines = List("first line", "second line", "third line")
    val path = File.home.path
    (diffActor, newLines, path)
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }


  "A DiffActor" must {
    "send a DiffEventMsg(path, newLines added) when receiving a ModificationDataMsg(path, newLines, null)" in {
      val modMsg = ModificationDataMsg(path.toString, newLines)
      diffActor ! modMsg

      val patch: Patch[String] = DiffUtils.diff(List[String]().asJava, newLines.asJava)
      val diffMsg = DiffEventMsg(path.toString, new SerializationPatchWrapper(patch))

      //needs to be done this way because Patch.equals is not good
      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath == path.toString && receivedNewLines == newLines && receivedOldLines.isEmpty
        case _ => false
      }
    }

    "send a DiffEventMsg(path, diff) when receiving a ModificationDataMsg(path, newLines, oldLines != null)" in {
      val oldLines = List("first line", "second line", "Im a different third line", "forth line")
      val modMsg = ModificationDataMsg(path.toString, newLines, Some(oldLines))
      diffActor ! modMsg

      val patch: Patch[String] = DiffUtils.diff(oldLines.asJava, newLines.asJava)
      val diffMsg = DiffEventMsg(path.toString, new SerializationPatchWrapper(patch))

      //needs to be done this way because Patch.equals is not good
      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case ModificationDataMsg(receivedPath, receivedNewLines, receivedOldLines) =>
          receivedPath == path.toString && receivedNewLines == newLines &&
            receivedOldLines.getOrElse(None) == oldLines
        case _ => false
      }
    }

    "send a GetLinesMsg(path, patch) when receiving an ApplyPatchMsg(path, patch)" in {
      val patch: StringPatch = DiffUtils.diff(List[String]().asJava, newLines.asJava)
      val applyPatchMsg = ApplyPatchMsg(path.toString, patch)

      diffActor ! applyPatchMsg

      val getterMsg = GetLinesMsg(path.toString, patch)

      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case GetLinesMsg(expectedPath, expectedPatch) =>
          expectedPath == path.toString && expectedPatch == patch
        case _ => false
      }
    }

    "send a UpdateFileMsg(path, patch(lines)) when receiving an OldLinesMsg(lines, path, patch)" in {
      val oldLines = List[String]("first line", "some other line")
      val patch: StringPatch = DiffUtils.diff(oldLines.asJava, newLines.asJava)
      val oldLinesMsg = OldLinesMsg(oldLines, path.toString, patch)

      diffActor ! oldLinesMsg

      expectMsgPF(Duration.apply(NUM_SECS_TO_WAIT, TimeUnit.SECONDS)) {
        case UpdateFileMsg(expectedPath, expectedLines) =>
          expectedPath == path.toString && expectedLines == newLines
        case _ => false
      }
    }
  }
}
