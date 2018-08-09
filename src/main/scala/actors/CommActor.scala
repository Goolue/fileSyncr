package actors

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}
import actors.Messages.FileEventMessage.FileDeletedMsg
import akka.actor.ActorSelection
import akka.routing.{BroadcastRoutingLogic, Routee, Router}

class CommActor extends BasicActor {
  def receive: Receive = handleMassages(Map.empty)

  private val protocol = "akka.tcp"

  private var router: Router = Router(BroadcastRoutingLogic(), Vector.empty[Routee])

  def handleMassages(connections: Map[String, ActorSelection]): Receive = {
    case HasConnectionQuery(url) =>
      log.info(s"$getClassName got an HasConnectionQuery for url $url")
      sender() ! connections.contains(url)

    case AddRemoteConnectionMsg(url, port, actorPathStr) =>
      log.info(s"$getClassName got an AddRemoteConnectionMsg for url $url, port $port, actor $actorPathStr")
      if (port <= 0) log.warning(s"port $port <= 0 !")
      else {
        url match {
          case null => log.info(s"$getClassName got a null url")
          case "" => log.info(s"$getClassName got an empty url")
          case _ =>
            if (!connections.contains(url) && isValidUrl(url)) {
              val selection = context.actorSelection(createRemotePath(url, port, actorPathStr))
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

    case fileDeletedMsg: FileDeletedMsg =>
      val file = fileDeletedMsg.file
      log.info(s"$getClassName got an FileDeletedMsg for file $file, routing to ${router.routees.size} routees")
      router.route(fileDeletedMsg, context.self)

    //TODO more msgs

    case msg => log.warning(s"$getClassName got an unidentified msg $msg")

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
