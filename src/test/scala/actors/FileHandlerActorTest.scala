package actors

import java.util.concurrent.TimeUnit

import actors.FileHandlerActor.{MapContainsKey, MapContainsValue}
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import actors.Messages.GetterMsg.{GetLinesMsg, OldLinesMsg}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
import utils.FileUtils

import scala.collection.JavaConverters._
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
      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend))
      fileHandler ! msg
      expectMsg(msg)
    }

    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(fileToSend)), testActor)
      expectMsg(false)
    }

    "update it's map when receiving a FileModifiedMsg" in {
      val msg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(file)), testActor)
      expectMsg(true)
    }
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
      "receiving a FileModifiedMsg for the first time" in {
      val msg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! msg

      val expectedNewLines = Traversable[String](TEXT_IN_FILE)
      val expectedOldLines = None
      val expecedPath = FileUtils.getFileAsRelativeStr(file)

      expectMsg(ModificationDataMsg(expecedPath, expectedNewLines, expectedOldLines))
    }

    "NOT have the path of the file to it's map when receiving a FileDeletedMsg" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileDeletedMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(file)), testActor)
      expectMsg(false)
    }

    "send the same FileDeletedMsg to the commActor when receiving a FileDeletedMsg" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! msg

      expectMsg(msg)
    }

    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val patch: Patch[String] = DiffUtils.diff(List.empty[String].asJava, List("bla").asJava)
      val getLinesMsg = GetLinesMsg(FileUtils.getFileAsRelativeStr(file), patch)
      fileHandler ! getLinesMsg

      expectMsg(OldLinesMsg(List(TEXT_IN_FILE), FileUtils.getFileAsRelativeStr(file), patch))
    }

    "update it's map when receiving an UpdateFileMsg for the first time" in {
      //check that the map does not contain the path
      fileHandler ! MapContainsKey(FileUtils.getFileAsRelativeStr(file))
      expectMsg(false)

      //send the msg
      val lines = List("some new lines")
      val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(file), lines)
      fileHandler ! updateMsg

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      //check if the map was updated
      fileHandler ! MapContainsKey(FileUtils.getFileAsRelativeStr(file))
      expectMsg(true)
      fileHandler ! MapContainsValue(Some(lines))
      expectMsg(true)
    }

    "update it's map when receiving an UpdateFileMsg NOT for the first time" in {
      //check that the map does not contain the path
      val path = FileUtils.getFileAsRelativeStr(file)
      fileHandler ! MapContainsKey(path)
      expectMsg(false)

      //send first msg
      val lines = List("some new lines")
      val firstUpdateMsg = UpdateFileMsg(path, lines)
      fileHandler ! firstUpdateMsg

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      //check if the map was updated
      fileHandler ! MapContainsKey(path)
      expectMsg(true)
      fileHandler ! MapContainsValue(Some(lines))
      expectMsg(true)

      //send the 2nd msg
      val newLines = List("some new lines")
      val updateMsg = UpdateFileMsg(path, newLines)
      fileHandler ! updateMsg

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      //check if the map was updated
      fileHandler ! MapContainsKey(path)
      expectMsg(true)
      fileHandler ! MapContainsValue(Some(newLines))
      expectMsg(true)
    }

    "NOT do anything when receiving an UpdateFileMsg for a dir" in {
      val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(File.home), null)
      fileHandler ! updateMsg

      expectNoMessage
    }

    "create the file(path) with lines when receiving an UpdateFileMsg(path, lines) for the first time" in {
      val newFile = File.currentWorkingDirectory / "src" / "test" / "resources" / "someOtherFile.txt"
      val newLines = List("lines for the new file", "and another one")
      fileHandler ! UpdateFileMsg(FileUtils.getFileAsRelativeStr(newFile), newLines)

      expectNoMessage(Duration.apply(3, TimeUnit.SECONDS))

      assert(newFile.exists)
      assert(newFile.lines == newLines)

      newFile.deleteOnExit()
    }

  }
}

