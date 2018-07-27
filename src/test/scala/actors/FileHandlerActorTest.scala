package actors

import java.util.concurrent.TimeUnit

import actors.FileHandlerActor.{MapContainsKey, MapContainsValue}
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.github.difflib.patch.Patch
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration.Duration

class FileHandlerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter{

  val TEXT_IN_FILE = "some text here"

  var fileHandler: ActorRef = _
  val file: File = File.currentWorkingDirectory / "src" / "test" / "resources" / "someFile.txt"

  before {
    this.fileHandler = system.actorOf(Props(new FileHandlerActor(testActor, testActor)))
  }

  after {
    fileHandler = null
    //revert changes to someFile.txt
    file.write(TEXT_IN_FILE)
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
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]

      fileHandler.tell(MapContainsKey(fileToSend.path), testActor)
      expectMsg(false)
    }
  }

  it must {
    "update it's map when receiving a FileModifiedMsg" in {
      val msg = FileModifiedMsg(file)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      fileHandler.tell(MapContainsKey(file.path), testActor)
      expectMsg(true)
    }
  }

  it must {
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
    "receiving a FileModifiedMsg for the first time" in {
      val msg = FileModifiedMsg(file)
      fileHandler ! msg

      val expectedNewLines = Traversable[String](TEXT_IN_FILE)
      val expectedOldLines = None
      val expecedPath = file.path

      expectMsg(ModificationDataMsg(expecedPath, expectedNewLines, expectedOldLines))
    }
  }

  it must {
    "NOT have the path of the file to it's map when receiving a FileDeletedMsg" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(file)
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(file)
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileDeletedMsg]

      fileHandler.tell(MapContainsKey(file.path), testActor)
      expectMsg(false)
    }
  }

  it must {
    "send the same FileDeletedMsg to the commActor when receiving a FileDeletedMsg" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(file)
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(file)
      fileHandler ! msg

      expectMsg(msg)
    }
  }

  it must {
    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(file)
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val patch: Patch[String] = new Patch()
      val getLinesMsg = GetLinesMsg(file.path, patch)
      fileHandler ! getLinesMsg

      expectMsg(OldLinesMsg(List(TEXT_IN_FILE), file.path, patch))
    }
  }

  it must {
    "update it's map when receiving an UpdateFileMsg for the first time" in {
      val path = file.path
      //check that the map does not contain the path
      fileHandler ! MapContainsKey(path)
      expectMsg(false)

      //send the msg
      val lines = List("some new lines")
      val updateMsg = UpdateFileMsg(path, lines)
      fileHandler ! updateMsg

      expectNoMessage(Duration.apply(3, TimeUnit.SECONDS))

      //check if the map was updated
      fileHandler ! MapContainsKey(path)
      expectMsg(true)
      fileHandler ! MapContainsValue(Some(lines))
      expectMsg(true)
    }
  }

  it must {
    "NOT do anything when receiving an UpdateFileMsg for a dir" in {
      val path = File.home.path
      val updateMsg = UpdateFileMsg(path, null)
      fileHandler ! updateMsg

      expectNoMessage
    }
  }

  it must {
    "create the file(path) with lines when receiving an UpdateFileMsg(path, lines) for the first time" in {

    }
  }

}

