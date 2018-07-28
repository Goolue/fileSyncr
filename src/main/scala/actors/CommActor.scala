package actors

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}
import akka.actor.ActorSelection
import akka.routing.{BroadcastRoutingLogic, Router}

class CommActor extends BasicActor {
  def receive: Receive = handleMassages(Map.empty)

  private val protocol = "akka.tcp"

  private var router = {
    Router(BroadcastRoutingLogic(), Vector.empty)
  }

  def handleMassages(connections: Map[String, ActorSelection]): Receive = {
    case HasConnectionQuery(url) =>
      log.info(s"$getClassName got an HasConnectionQuery for url $url")
      sender() ! connections.contains(url)

    case AddRemoteConnectionMsg(url, port, actorClass) =>
      log.info(s"$getClassName got an AddRemoteConnectionMsg for url $url, port $port, actor $actorClass")
      if (port <= 0) log.warning(s"port $port <= 0 !")
      else {
        url match {
          case null => log.info(s"$getClassName got a null url")
          case "" => log.info(s"$getClassName got an empty url")
          case _ =>
            if (!connections.contains(url) && isValidUrl(url)) {
              val selection = context.actorSelection(createRemotePath(url, port, actorClass))
              router = router.addRoutee(selection)
              context become handleMassages(connections.updated(url, selection))
            }
            else log.info(s"$getClassName got an invalid url $url")
        }
      }

    case RemoveRemoteConnectionMsg(url) =>
      log.info(s"$getClassName got an RemoveRemoteConnectionMsg for url $url")
      if (url != null && connections.contains(url)) {
        router = router.removeRoutee(connections.getOrElse[ActorSelection](url, context.actorSelection(""))) //TODO maybe else part is not good
        context become handleMassages(connections - url)
      }

    case msg => log.warning(s"$getClassName got an unidentified msg $msg")

      //TODO more msgs
  }

  private def isValidUrl(url: String) = {
    url.forall(c => c.isLetterOrDigit || c == '.')
  }

  private def createRemotePath (url: String, port: Int, actorClass: String): String = {
    s"$protocol://${context.system.name}@$url:$port/$actorClass"
  }
}

object CommActor {
  sealed class RemoteConnectionMsg
  case class AddRemoteConnectionMsg(url: String, port: Int, actorClass: String) extends RemoteConnectionMsg
  case class RemoveRemoteConnectionMsg(url: String) extends RemoteConnectionMsg
  case class HasConnectionQuery(url: String) extends RemoteConnectionMsg
}
