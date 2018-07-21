import Messages.FileEventMessage.FileCreatedMsg
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestActors, TestKit}
import better.files.File
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class FileHandlerActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "A FileHandlerActor" must {
    "send a FileCreatedMsg unchanged when receiving a FileCreatedMsg" in {
      val (msg, _) = sendFileCreatedMsgToFileHandler
      expectMsg(msg)
    }
  }

  private def sendFileCreatedMsgToFileHandler(implicit fileToSend: File = File.home): (FileCreatedMsg, ActorRef) = {
    val fileHandler = system.actorOf(Props(new FileHandlerActor(testActor)))
    val msg = FileCreatedMsg(fileToSend)
    fileHandler ! msg
    (msg, fileHandler)
  }
}

