package actors

import java.util.concurrent.TimeUnit

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}
import actors.Messages.FileEventMessage.FileDeletedMsg
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import better.files.File
import com.sun.jndi.cosnaming.IiopUrl.Address
import org.scalatest._

import scala.concurrent.duration.Duration

class CommActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  var commActor: ActorRef = _
//  val DEFAULT_PORT = 1000
  val DEFAULT_PORT = 2552

  before {
    commActor = system.actorOf(Props(new CommActor()), "commActor")
  }

  after {
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

    "forward the msg when receiving a FileDeletedMsg" in {
      commActor ! AddRemoteConnectionMsg("127.0.0.1", DEFAULT_PORT, testActor.path.toStringWithoutAddress)

      val msg = FileDeletedMsg(File.currentWorkingDirectory)
      commActor ! msg
      expectMsg(msg)
    }
  }

}
