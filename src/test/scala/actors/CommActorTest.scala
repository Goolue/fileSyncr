package actors

import java.util.concurrent.TimeUnit

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}
import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest._

import scala.concurrent.duration.Duration

class CommActorTest extends TestKit(ActorSystem("MySpec")) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  var commActor: ActorRef = _

  before {
    commActor = system.actorOf(Props(new CommActor()))
  }

  after {
    commActor = null
  }

  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "CommActor" must {
    "add url to it's set when receiving a AddRemoteConnectionMsg(url) with a valid numeric url" in {
      val url = "127.0.0.1"
      commActor ! AddRemoteConnectionMsg(url)

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(true)
    }

    "add url to it's set when receiving a AddRemoteConnectionMsg(url) with a valid non-numeric url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url)

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(true)
    }

    "NOT add url to it's set when receiving a AddRemoteConnectionMsg(url) with an empty url" in {
      val url = ""
      commActor ! AddRemoteConnectionMsg(url)

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(false)
    }

    "NOT add url to it's set when receiving a AddRemoteConnectionMsg(url) with an invalid url" in {
      val url = "Im an invalid url"
      commActor ! AddRemoteConnectionMsg(url)

      expectNoMessage(Duration.apply(1, TimeUnit.SECONDS))

      commActor ! HasConnectionQuery(url)

      expectMsg(false)
    }

    "remove url from it's set when receiving a RemoveRemoteConnectionMsg(url) with an existing url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url)

      commActor ! HasConnectionQuery(url)
      expectMsg(true)

      commActor ! RemoveRemoteConnectionMsg(url)

      commActor ! HasConnectionQuery(url)
      expectMsg(false)
    }

    "NOT remove url from it's set when receiving a RemoveRemoteConnectionMsg(url) with a non-existing url" in {
      val url = "localhost"
      commActor ! AddRemoteConnectionMsg(url)

      commActor ! HasConnectionQuery(url)
      expectMsg(true)

      val someOtherUrl = "some other url"
      commActor ! RemoveRemoteConnectionMsg(someOtherUrl)

      commActor ! HasConnectionQuery(url)
      expectMsg(true)
      commActor ! HasConnectionQuery(someOtherUrl)
      expectMsg(false)
    }
  }

}
