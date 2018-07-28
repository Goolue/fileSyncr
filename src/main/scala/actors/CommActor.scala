package actors

import actors.CommActor.{AddRemoteConnectionMsg, HasConnectionQuery, RemoveRemoteConnectionMsg}

class CommActor extends BasicActor {
  def receive: Receive = handleMassages(Set.empty)

  def handleMassages(connections: Set[String]): Receive = {
    case HasConnectionQuery(url) =>
      log.info(s"$getClassName got an HasConnectionQuery for url $url")
      sender() ! connections.contains(url)

    case AddRemoteConnectionMsg(url) =>
      log.info(s"$getClassName got an AddRemoteConnectionMsg for url $url")
      url match {
        case null => log.info(s"$getClassName got a null url")
        case "" => log.info(s"$getClassName got an empty url")
        case _ =>
          if (!connections.contains(url) && isValidUrl(url)) {
            context become handleMassages(connections + url)
          }
          else log.info(s"$getClassName got an invalid url $url")
      }

    case RemoveRemoteConnectionMsg(url) =>
      log.info(s"$getClassName got an RemoveRemoteConnectionMsg for url $url")
      if (url != null && connections.contains(url)) context become handleMassages(connections - url)

    case msg => log.warning(s"$getClassName got an unidentified msg $msg")

      //TODO more msgs
  }

  private def isValidUrl(url: String) = {
    url.forall(c => c.isLetterOrDigit || c == '.')
  }
}

object CommActor {
  sealed class RemoteConnectionMsg
  case class AddRemoteConnectionMsg(url: String) extends RemoteConnectionMsg
  case class RemoveRemoteConnectionMsg(url: String) extends RemoteConnectionMsg
  case class HasConnectionQuery(url: String) extends RemoteConnectionMsg
}
