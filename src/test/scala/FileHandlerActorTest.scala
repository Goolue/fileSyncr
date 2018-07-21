import Messages.FileEventMessage.{FileCreatedMsg, FileModifiedMsg}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActorRef, TestActors, TestKit, TestProbe}
import better.files.File
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class FileHandlerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

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
      val (fileHandler: TestActorRef[FileHandlerActor], actor: FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe

      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)
      expectMsg(msg)
    }
  }

  it must {
    "NOT add the path of the file to it's map when receiving a FileCreatedMsg" in {
      val (fileHandler: TestActorRef[FileHandlerActor], actor: FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe
      val fileToSend = File.home
      val msg = FileCreatedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      actor.mapContains(fileToSend.path) should be (false)
    }
  }

  it must {
    "update it's map when receiving a FileModifiedMsg" in {
      val (fileHandler: TestActorRef[FileHandlerActor], actor: FileHandlerActor, probe: TestProbe) = createFileHandlerActorAndProbe
      val fileToSend = File.newTemporaryFile("someFile", ".txt", Some(File.currentWorkingDirectory))
        .append("some text here")
        .deleteOnExit()
      val msg = FileModifiedMsg(fileToSend)
      fileHandler.tell(msg, probe.ref)

      actor.mapContains(fileToSend.path) should be (true)

    }
  }

}

