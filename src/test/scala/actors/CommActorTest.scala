package actors

import java.util.concurrent.TimeUnit

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}
import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg}
import akka.actor.{ActorRef, ActorSystem, PoisonPill, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch
import entities.serialization.SerializationPatchWrapper
import extensions.AddressExtension
import org.scalatest._

import scala.collection.JavaConverters._
import scala.concurrent.duration.Duration

class CommActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  var commActor: ActorRef = _
  var currPort: Int = _
  val localhostUrl = "127.0.0.1"

  before {
    currPort = AddressExtension(system).address.port.getOrElse(-1)
    println(s"system using port $currPort")
    val rand = scala.util.Random.nextInt()
    commActor = system.actorOf(Props(new CommActor(localhostUrl, testActor, testActor)), s"commActor$rand")
  }

  after {
    commActor ! PoisonPill
    commActor = null
  }


  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }
  "CommActor" must {
    "add url to it's map when receiving a AddRemoteConnectionMsg(url) with a valid numeric url" in {
      val url = "127.0.0.1"
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(true)
    }


    "add url to it's map when receiving a AddRemoteConnectionMsg(url, _, _) with a valid non-numeric url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(true)
    }

    "NOT add url to it's map when receiving a AddRactorClassemoteConnectionMsg(url, _, _) with an empty url" in {
      val url = ""
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(false)
    }

    "NOT add url to it's map when receiving a AddRemoteConnectionMsg(url, _, _) with an invalid url" in {
      val url = "Im an invalid url"
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(false)
    }

    "NOT add url to it's map when receiving a AddRemoteConnectionMsg(url, _, port <= 0) with an invalid port" in {
      val url = "Im an invalid url"
      commActor ! AddRemoteConnectionMsg(url, -1, "commActor")

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(false)
    }

    "remove url from it's map when receiving a RemoveRemoteConnectionMsg(url) with an existing url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      commActor ! HasConnectionQuery(url)
      expectMsg(true)

      commActor ! RemoveRemoteConnectionMsg(url)

      commActor ! HasConnectionQuery(url)
      expectMsg(false)
    }

    "NOT remove url from it's map when receiving a RemoveRemoteConnectionMsg(url) with a non-existing url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url, 1000, "commActor")

      commActor ! HasConnectionQuery(url)
      expectMsg(true)

      val someOtherUrl = "some other url"
      commActor ! RemoveRemoteConnectionMsg(someOtherUrl)

      commActor ! HasConnectionQuery(url)
      expectMsg(true)
      commActor ! HasConnectionQuery(someOtherUrl)
      expectMsg(false)
    }

    "forward the msg when receiving a FileDeletedMsg with isRemote = false" in {
      commActor ! AddRemoteConnectionMsg(localhostUrl, currPort, testActor.path.toStringWithoutAddress)

      val msg = FileDeletedMsg(File.currentWorkingDirectory.toString())
      commActor ! msg

      expectMsg(FileDeletedMsg(File.currentWorkingDirectory.toString(), true))
    }

    "Send a FileDeletedMsg msg when receiving a FileDeletedMsg with isRemote = true" in {
      commActor ! AddRemoteConnectionMsg(localhostUrl, currPort, testActor.path.toStringWithoutAddress)

      val file = File.currentWorkingDirectory
      val msg = FileDeletedMsg(file.toString(), isRemote = true)
      commActor ! msg

      expectMsg(msg)
    }

    "forward the msg when receiving a FileCreatedMsg with isRemote = false" in {
      commActor ! AddRemoteConnectionMsg(localhostUrl, currPort, testActor.path.toStringWithoutAddress)

      val path = ""
      commActor ! FileCreatedMsg(path)

      expectMsg(FileCreatedMsg(path, isRemote = true))
    }

    "Send a FileCreatedMsg msg when receiving a FileCreatedMsg with isRemote = true" in {
      commActor ! AddRemoteConnectionMsg(localhostUrl, currPort, testActor.path.toStringWithoutAddress)

      val msg = FileCreatedMsg("", isRemote = true)
      commActor ! msg

      expectMsg(msg)
    }

    "forward the msg when receiving a DiffEventMsg with isRemote = false" in {
      commActor ! AddRemoteConnectionMsg(localhostUrl, currPort, testActor.path.toStringWithoutAddress)

      val patch: Patch[String] = DiffUtils.diff(List[String]().asJava, List[String]().asJava)
      val msg = DiffEventMsg("", new SerializationPatchWrapper(patch), isRemote = false)
      commActor ! msg

      expectMsg(DiffEventMsg(msg.path, msg.patch, isRemote = true))
    }

    "send an ApplyPatchMsg msg when receiving a DiffEventMsg with isRemote = true" in {
      val patch: Patch[String] = DiffUtils.diff(List[String]().asJava, List[String]().asJava)

      val wrapper = new SerializationPatchWrapper(patch)
      val msg = DiffEventMsg("", wrapper, isRemote = true)
      commActor ! msg

      expectMsgPF(Duration.apply(3, TimeUnit.SECONDS)) {
        case ApplyPatchMsg(_, msgPatch) => msgPatch != null && msgPatch.equals(patch)
        case _ => false
      }
    }
  }

}
