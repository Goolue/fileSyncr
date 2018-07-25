package actors

import actors.Messages.EventDataMessage.ModificationDataMsg
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import better.files.File
import com.github.difflib.patch.Patch
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

class FileHandlerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter{

  val TEXT_IN_FILE = "some text here"

  var fileHandler: TestActorRef[FileHandlerActor] = _
  var actor: FileHandlerActor = _
  var probe: TestProbe = _

  before {
    val (fileHandler: TestActorRef[FileHandlerActor], actor: FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe
    this.fileHandler = fileHandler
    this.actor = actor
    this.probe = probe
  }

  after {
    fileHandler = null
    actor.clearMap()
    actor = null
    probe = null
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private def createFileHandlerActorAndProbe: (TestActorRef[FileHandlerActor], FileHandlerActor, TestProbe) = {
    val fileHandler = TestActorRef[FileHandlerActor](Props(new FileHandlerActor(testActor, testActor)))
    val actor = fileHandler.underlyingActor
    val probe = new TestProbe(system)
    (fileHandler, actor, probe)
  }

  "A FileHandlerActor" must {
    "send a FileCreatedMsg unchanged when receiving a FileCreatedMsg" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)
      expectMsg(msg)
    }
  }

  it must {
    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
//      val (fileHandler: TestActorRef[actors.FileHandlerActor], actor: actors.FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      actor.mapContains(fileToSend.path) should be (false)

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]
    }
  }

  it must {
    "update it's map when receiving a FileModifiedMsg" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val msg = FileModifiedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      actor.mapContains(fileToSend.path) should be (true)

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

    }
  }

  it must {
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
    "receiving a FileModifiedMsg for the first time" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val msg = FileModifiedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      val expectedNewLines = Traversable[String](TEXT_IN_FILE)
      val expectedOldLines = None
      val expecedPath = fileToSend.path

      expectMsg(ModificationDataMsg(expecedPath, expectedNewLines, expectedOldLines))
    }
  }

  it must {
    "NOT have the path of the file to it's map when receiving a FileDeletedMsg" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(fileToSend)
      fileHandler.tell(modMsg, probe.ref)
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      actor.mapContains(fileToSend.path) should be (false)

      //clear the message from the queue
      expectMsgType[FileDeletedMsg]
    }
  }

  it must {
    "send the same FileDeletedMsg to the commActor when receiving a FileDeletedMsg" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(fileToSend)
      fileHandler.tell(modMsg, probe.ref)
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      expectMsg(msg)
    }
  }

  it must {
    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {

      //send a file mod msg so the path will be in the map
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val modMsg = FileModifiedMsg(fileToSend)
      fileHandler.tell(modMsg, probe.ref)
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val patch: Patch[String] = new Patch()
      val getLinesMsg = GetLinesMsg(fileToSend.path, patch)
      fileHandler.tell(getLinesMsg, probe.ref)

      expectMsg(OldLinesMsg(List(TEXT_IN_FILE), fileToSend.path, patch))
    }
  }

}

