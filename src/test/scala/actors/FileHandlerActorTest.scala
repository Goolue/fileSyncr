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

  private val tempFileDir: File = File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles"
  val file: File = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))

  var fileHandler: ActorRef = _

  before {
    this.fileHandler = system.actorOf(Props(new FileHandlerActor(testActor, testActor, tempFileDir)))
  }

  after {
    fileHandler = null
  }

  override def afterAll {
    tempFileDir.clear()
    TestKit.shutdownActorSystem(system)
  }

  "A FileHandlerActor" must {
    "send a FileCreatedMsg unchanged to commActor when receiving a FileCreatedMsg with isRemote = false" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend, File.currentWorkingDirectory))
      fileHandler ! msg
      expectMsg(msg)
    }

    "create the appropriate file when receiving a FileCreatedMsg with isRemote = true" in {
      val fileToSend = tempFileDir /  "someFile.txt"
      fileToSend.deleteOnExit()

      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), isRemote = true)
      fileHandler ! msg

      Thread sleep 1000

      (tempFileDir / "someFile.txt").exists should be (true)
    }

    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend, File.currentWorkingDirectory))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(fileToSend, File.currentWorkingDirectory)), testActor)
      expectMsg(false)
    }

    "update it's map when receiving a FileModifiedMsg" in {
      val msg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(file, tempFileDir)), testActor)
      expectMsg(true)

      //revert changes to someFile.txt
      file.write(TEXT_IN_FILE)
      expectMsgType[ModificationDataMsg]
    }
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
      "receiving a FileModifiedMsg for the first time" in {
      val msg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! msg

      val expectedNewLines = Traversable[String](TEXT_IN_FILE)
      val expectedOldLines = None
      val expectedPath = FileUtils.getFileAsRelativeStr(file, tempFileDir)

      expectMsg(ModificationDataMsg(expectedPath, expectedNewLines, expectedOldLines))

      //revert changes to someFile.txt
      tempFileDir.write(TEXT_IN_FILE)
      expectMsgType[ModificationDataMsg]
    }

    "NOT have the path of the file to it's map when receiving a FileDeletedMsg" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileDeletedMsg]

      fileHandler.tell(MapContainsKey(FileUtils.getFileAsRelativeStr(file, tempFileDir)), testActor)
      expectMsg(false)

      //revert changes to someFile.txt
      tempFileDir.write(TEXT_IN_FILE)
      expectMsgType[ModificationDataMsg]
    }

    "send the same FileDeletedMsg to the commActor when receiving a FileDeletedMsg with isRemote = false" in {
      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! msg

      expectMsg(msg)
    }

    "delete the appropriate file when receiving a FileDeletedMsg with isRemote = true" in {
      // TODO check this one

      val fileToSend = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), isRemote = true)
      fileHandler ! msg

      Thread sleep 1000

      // not using fileToSend because fileToSend.exists == true
      (File.currentWorkingDirectory / fileToSend.name).exists should be (false)
    }

    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {
      val fileToSend = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))

      //send a file mod msg so the path will be in the map
      val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
      fileHandler ! modMsg
      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      val patch: Patch[String] = DiffUtils.diff(List.empty[String].asJava, List("bla").asJava)
      val getLinesMsg = GetLinesMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), patch)
      fileHandler ! getLinesMsg

      expectMsg(OldLinesMsg(List(TEXT_IN_FILE), FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), patch))
    }

    "update it's map when receiving an UpdateFileMsg for the first time" in {
      val fileToSend = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))

      //check that the map does not contain the path
      fileHandler ! MapContainsKey(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
      expectMsg(false)

      //send the msg
      val lines = List("some new lines")
      val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), lines)
      fileHandler ! updateMsg

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      //check if the map was updated
      fileHandler ! MapContainsKey(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
      expectMsg(true)
      fileHandler ! MapContainsValue(Some(lines))
      expectMsg(true)
    }

    "update it's map when receiving an UpdateFileMsg NOT for the first time" in {
      val fileToSend = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))

      //check that the map does not contain the path
      val path = FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir)
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
      val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(File.home, File.currentWorkingDirectory), null)
      fileHandler ! updateMsg

      expectNoMessage
    }

    "create the file(path) with lines when receiving an UpdateFileMsg(path, lines) for the first time" in {
      val newFile = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))
      val newLines = List("lines for the new file", "and another one")
      fileHandler ! UpdateFileMsg(FileUtils.getFileAsRelativeStr(newFile, tempFileDir), newLines)

      expectNoMessage(Duration.apply(3, TimeUnit.SECONDS))

      assert(newFile.exists)
      assert(newFile.lines == newLines)

      newFile.deleteOnExit()
    }

  }
}

