package actors

import java.nio.file.Path
import java.util.concurrent.TimeUnit

import actors.FileHandlerActor.{LinesOption, MapContainsKeyMsg, MapContainsValueMsg}
import actors.Messages.EventDataMessage.{ModificationDataMsg, UpdateFileMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import actors.Messages.GetterMsg._
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
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  val TEXT_IN_FILE = "some text here"

  private val tempFileDir: File = File.currentWorkingDirectory / "src" / "test" / "resources" / "tempFiles"
  var file: File = _

  var fileHandler: ActorRef = _

  before {
    file = File.newTemporaryFile("someFile", ".txt", Some(tempFileDir))
    file.createIfNotExists()
    file.overwrite(TEXT_IN_FILE)
    this.fileHandler = system.actorOf(Props(new FileHandlerActor(testActor, testActor, tempFileDir,
      createWatchConfigurer = false)))
  }

  after {
    if (file.exists) {
      file.delete()
    }
    fileHandler = null
  }

  override def afterAll {
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
      val fileToSend = tempFileDir / "someFile.txt"
      fileToSend.deleteOnExit()

      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), isRemote = true)
      fileHandler ! msg

      Thread sleep 1000

      (tempFileDir / "someFile.txt").exists should be(true)
    }

    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
      val fileToSend = File.home
      val msg = FileCreatedMsg(FileUtils.getFileAsRelativeStr(fileToSend, File.currentWorkingDirectory))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[FileCreatedMsg]

      fileHandler ! MapContainsKeyMsg(FileUtils.getFileAsRelativeStr(fileToSend, File.currentWorkingDirectory))

      expectMsg(false)
    }

    // TODO does not pass when running all tests in system (passes when running only this file)
    "update it's map when receiving a FileModifiedMsg" in {
      val msg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      fileHandler ! msg

      //clear the message from the queue
      expectMsgType[ModificationDataMsg]

      fileHandler ! MapContainsKeyMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      expectMsg(true)

      //revert changes to someFile.txt
      file.write(TEXT_IN_FILE)

    }

    // TODO does not pass when running all tests in system (passes when running only this file)
    "send a ModificationDataMsg with the correct path, None old lines, and new lines when " +
      "receiving a FileModifiedMsg for the first time" in {
      val pathStr = FileUtils.getFileAsRelativeStr(file, tempFileDir)
      val msg = FileModifiedMsg(pathStr)
      fileHandler ! msg

      val expectedNewLines = Traversable[String](TEXT_IN_FILE)
      val expectedOldLines = None

      expectMsg(ModificationDataMsg(pathStr, expectedNewLines, expectedOldLines))

      //revert changes to someFile.txt
      file.write(TEXT_IN_FILE)
    }

    // TODO does not pass when running all tests in system (passes when running only this file)
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

      fileHandler ! MapContainsKeyMsg(FileUtils.getFileAsRelativeStr(file, tempFileDir))
      expectMsg(false)

      //revert changes to someFile.txt
      file.write(TEXT_IN_FILE)

    }

    // TODO does not pass when running all tests in system (passes when running only this file)
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

      File.usingTemporaryFile("someFile", ".txt", Some(tempFileDir)) { fileToSend =>
        //send a file mod msg so the path will be in the map
        val modMsg = FileModifiedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
        fileHandler ! modMsg
        //clear the message from the queue
        expectMsgType[ModificationDataMsg]

        val msg = FileDeletedMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), isRemote = true)
        fileHandler ! msg

        Thread sleep 1000

        // not using fileToSend because fileToSend.exists == true
        (File.currentWorkingDirectory / fileToSend.name).exists should be(false)
      }
    }

    "send an OldLinesMsg(lines, path, patch) when receiving a GetLinesMsg(path, patch)" in {
      File.usingTemporaryFile("someFile", ".txt", Some(tempFileDir)) { fileToSend =>
        val filePathStr = FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir)

        //send a file mod msg so the path will be in the map
        val modMsg = FileModifiedMsg(filePathStr)
        fileHandler ! modMsg
        //clear the message from the queue
        expectMsgType[ModificationDataMsg]

        val patch: Patch[String] = DiffUtils.diff(List.empty[String].asJava, List("bla").asJava)
        val getLinesMsg = GetLinesMsg(filePathStr, patch)
        fileHandler ! getLinesMsg

        expectMsg(OldLinesMsg(List.empty, filePathStr, patch))
      }
    }

    "update it's map when receiving an UpdateFileMsg for the first time" in {
      File.usingTemporaryFile("someFile", ".txt", Some(tempFileDir)) { fileToSend =>
        //check that the map does not contain the path
        fileHandler ! MapContainsKeyMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
        expectMsg(false)

        //send the msg
        val lines = List("some new lines")
        val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir), lines)
        fileHandler ! updateMsg

        expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

        //check if the map was updated
        fileHandler ! MapContainsKeyMsg(FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir))
        expectMsg(true)
        fileHandler ! MapContainsValueMsg(Some(lines))
        expectMsg(true)
      }
    }

    "update it's map when receiving an UpdateFileMsg NOT for the first time" in {
      File.usingTemporaryFile("someFile", ".txt", Some(tempFileDir)) { fileToSend =>
        //check that the map does not contain the path
        val path = FileUtils.getFileAsRelativeStr(fileToSend, tempFileDir)
        fileHandler ! MapContainsKeyMsg(path)
        expectMsg(false)

        //send first msg
        val lines = List("some new lines")
        val firstUpdateMsg = UpdateFileMsg(path, lines)
        fileHandler ! firstUpdateMsg

        expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

        //check if the map was updated
        fileHandler ! MapContainsKeyMsg(path)
        expectMsg(true)
        fileHandler ! MapContainsValueMsg(Some(lines))
        expectMsg(true)

        //send the 2nd msg
        val newLines = List("some new lines")
        val updateMsg = UpdateFileMsg(path, newLines)
        fileHandler ! updateMsg

        expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

        //check if the map was updated
        fileHandler ! MapContainsKeyMsg(path)
        expectMsg(true)
        fileHandler ! MapContainsValueMsg(Some(newLines))
        expectMsg(true)
      }
    }

    "NOT do anything when receiving an UpdateFileMsg for a dir" in {
      val updateMsg = UpdateFileMsg(FileUtils.getFileAsRelativeStr(File.home, File.currentWorkingDirectory), null)
      fileHandler ! updateMsg

      expectNoMessage
    }

    "create the file(path) with lines when receiving an UpdateFileMsg(path, lines) for the first time" in {
      File.usingTemporaryFile("someFile", ".txt", Some(tempFileDir)) { newFile =>
        val newLines = List("lines for the new file", "and another one")
        fileHandler ! UpdateFileMsg(FileUtils.getFileAsRelativeStr(newFile, tempFileDir), newLines)

        expectNoMessage(Duration.apply(3, TimeUnit.SECONDS))

        assert(newFile.exists)
        assert(newFile.lines == newLines)

        newFile.deleteOnExit()
      }
    }

    "return an empty StateMsg when receiving a GetStateMsg and dir is empty" in {
      tempFileDir.clear()
      fileHandler ! GetStateMsg

      expectMsg(StateMsg(Map.empty))
    }

    "return a StateMsg with correct entry (1 file, correct lines, not dir) when receiving a GetStateMsg" in {
      fileHandler ! GetStateMsg

      val stateMsg = StateMsg(Map(tempFileDir.relativize(file) -> file.lines))
      expectMsg(stateMsg)
    }

    "return a StateMsg with correct entry (2 files, correct lines, not dir) when receiving a GetStateMsg" in {
      File.usingTemporaryFile("SomeOtherFile", ".txt", Some(tempFileDir)) { otherFile =>
        fileHandler ! GetStateMsg

        val stateMsg = StateMsg(Map(tempFileDir.relativize(file) -> file.lines, tempFileDir.relativize(otherFile) -> otherFile.lines))
        expectMsg(stateMsg)
      }
    }

    "return a StateMsg with correct entry (2 files, correct lines, 1 empty dir) when receiving a GetStateMsg" in {
      for {
        otherFile: File <- File.temporaryFile("SomeOtherFile", ".txt", Some(tempFileDir))
        tempDir: File <- File.temporaryDirectory("tempDir", Some(tempFileDir))
      } {
        fileHandler ! GetStateMsg

        val stateMsg = StateMsg(Map(tempFileDir.relativize(file) -> file.lines, tempFileDir.relativize(otherFile) -> otherFile.lines))
        expectMsg(stateMsg)
      }
    }

    "return a StateMsg with correct entry (2 files, correct lines, 1 dir with 1 file in it) when receiving a GetStateMsg" in {
      for {
        otherFile: File <- File.temporaryFile("SomeOtherFile", ".txt", Some(tempFileDir))
        tempDir: File <- File.temporaryDirectory("tempDir", Some(tempFileDir))
        otherFileInTempDir: File <- File.temporaryFile("otherFileInTempDir", ".txt", Some(tempDir))
      } {
        fileHandler ! GetStateMsg

        val stateMsg = StateMsg(Map(tempFileDir.relativize(file) -> file.lines,
          tempFileDir.relativize(otherFile) -> otherFile.lines,
          tempFileDir.relativize(otherFileInTempDir) -> Traversable.empty[String]))
        expectMsg(stateMsg)
      }
    }

    "return a StateMsg with correct entry (2 files, correct lines, 2 dirs with 1 dir with 1 file in it) when receiving a GetStateMsg" in {
      for {
        otherFile: File <- File.temporaryFile("SomeOtherFile", ".txt", Some(tempFileDir))
        tempDir1: File <- File.temporaryDirectory("tempDir1", Some(tempFileDir))
        tempDir2: File <- File.temporaryDirectory("tempDir2", Some(tempFileDir))
        otherFileInTempDir1: File <- File.temporaryFile("otherFileInTempDir1", ".txt", Some(tempDir1))
        otherFileInTempDir2: File <- File.temporaryFile("otherFileInTempDir2", ".txt", Some(tempDir2))
      } {
        val txtInOtherFIleInDir1 = "Yep, some text!!"
        otherFileInTempDir1.overwrite(txtInOtherFIleInDir1)

        fileHandler ! GetStateMsg

        val mapExpected: Map[Path, LinesOption] = Map(tempFileDir.relativize(file) -> Some(file.lines),
          tempFileDir.relativize(otherFile) -> Some(otherFile.lines),
          tempFileDir.relativize(otherFileInTempDir1) -> Some(Traversable.empty[String]),
          tempFileDir.relativize(otherFileInTempDir2) -> Some(Traversable(txtInOtherFIleInDir1)))
        expectMsgPF(Duration.apply(3, TimeUnit.SECONDS)) {
          case StateMsg(mapInMsg) =>
            mapInMsg.forall(entry => mapExpected.contains(entry._1) && entry._2 == mapExpected(entry._1)) &&
              mapExpected.size == mapInMsg.size
          case _ => false
        }
      }
    }

    "clear the directory when receiving an empty ApplyStateMsg with clearFiles = true" in {
      tempFileDir.isEmpty should be (false)

      fileHandler ! ApplyStateMsg(Map.empty, clearFiles = true)

      Thread.sleep(1000)

      tempFileDir.isEmpty should be (true)
    }

    "not do anything when receiving an empty ApplyStateMsg with clearFiles = false" in {
      val childrenPrev = tempFileDir.children

      fileHandler ! ApplyStateMsg(Map.empty)

      Thread.sleep(1000)

      val childrenPost = tempFileDir.children

      childrenPrev sameElements childrenPost should be (true)
    }

    "update content of file when receiving a non-empty ApplyStateMsg with clearFiles = false" in {
      file.lines should be (List(TEXT_IN_FILE))

      val newTxt = "different text!"
      fileHandler ! ApplyStateMsg(Map(tempFileDir.relativize(file) -> Seq(newTxt)))

      Thread sleep 1000

      file.lines should be (List(newTxt))
    }

    "update content of file when receiving a non-empty ApplyStateMsg with clearFiles = true" in {
      file.lines should be (List(TEXT_IN_FILE))

      val newTxt = "different text!"
      fileHandler ! ApplyStateMsg(Map(tempFileDir.relativize(file) -> Seq(newTxt)), clearFiles = true)

      Thread sleep 1000

      file.lines should be (List(newTxt))
    }

    "create a missing file (non-dir) and delete existing file when receiving an ApplyStateMsg with clearFiles = true" in {
      val missingFile = tempFileDir / "missingFile.txt"
      missingFile.deleteOnExit()
      val missingPath = tempFileDir.relativize(missingFile)
      fileHandler ! ApplyStateMsg(Map(missingPath -> Traversable.empty), clearFiles = true)

      Thread sleep 1000

      missingFile.exists should be (true)
      file.exists should be (false)

    }

    "create a missing file (empty file inside missing dir) and not delete existing file when receiving an " +
      "ApplyStateMsg with clearFiles = false" in {
      val missingFile = tempFileDir / "missingDir" / "missingFile.txt"
      missingFile.deleteOnExit()

      val missingPath = tempFileDir.relativize(missingFile)
      fileHandler ! ApplyStateMsg(Map(missingPath -> Traversable.empty))

      Thread sleep 1000

      missingFile.exists should be (true)
      file.exists should be (true)
    }

    "create a missing file (empty file inside missing dir) and not delete existing file when receiving an " +
      "ApplyStateMsg with clearFiles = true" in {
      val missingFile = tempFileDir / "missingDir" / "missingFile.txt"
      missingFile.deleteOnExit()

      val missingPath = tempFileDir.relativize(missingFile)
      fileHandler ! ApplyStateMsg(Map(missingPath -> Traversable.empty), clearFiles = true)

      Thread sleep 1000

      missingFile.exists should be (true)
      file.exists should not be true
    }

  }
}

