package actors

import actors.FileHandlerActor.MapContainsKey
import actors.Messages.EventDataMessage.ModificationDataMsg
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.github.difflib.patch.Patch
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

class FileHandlerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter{

  val TEXT_IN_FILE = "some text here"

  var fileHandler: ActorRef = _

  before {
    this.fileHandler = system.actorOf(Props(new FileHandlerActor(testActor, testActor)))
  }

  after {
    fileHandler = null
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A FileHandlerActor" must {
    "send a FileCreatedMsg unchanged when receiving a FileCreatedMsg" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler ! msg
      expectMsg(msg)
    }
  }

  it must {
    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
//      val (fileHandler: TestActorRef[actors.FileHandlerActor], actor: actors.FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]

//      actor.mapContains(fileToSend.path) should be (false)
      fileHandler.tell(MapContainsKey(fileToSend.path), testActor)
      expectMsg(false)
    }
  }

  it must {
    "update it's map when receiving a FileModifiedMsg" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val msg = FileModifiedMsg(fileToSend)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      //      actor.mapContains(fileToSend.path) should be (true)
      fileHandler.tell(MapContainsKey(fileToSend.path), testActor)
      expectMsg(true)
    }
  }

  it must {
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
    "receiving a FileModifiedMsg for the first time" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val msg = FileModifiedMsg(fileToSend)
      fileHandler ! msg

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
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(fileToSend)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileDeletedMsg]

      fileHandler.tell(MapContainsKey(fileToSend.path), testActor)
      expectMsg(false)
    }
  }

  it must {
    "send the same FileDeletedMsg to the commActor when receiving a FileDeletedMsg" in {
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(fileToSend)
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(fileToSend)
      fileHandler ! msg

      expectMsg(msg)
    }
  }

  it must {
    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {

      //send a file mod msg so the path will be in the map
      val fileToSend = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"
      val modMsg = FileModifiedMsg(fileToSend)
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val patch: Patch[String] = new Patch()
      val getLinesMsg = GetLinesMsg(fileToSend.path, patch)
      fileHandler ! getLinesMsg

      expectMsg(OldLinesMsg(List(TEXT_IN_FILE), fileToSend.path, patch))
    }
  }

//  it must {
//    "update it's map when receiving an UpdateFileMsg for the first time" in {
//
//    }
//  }

}

