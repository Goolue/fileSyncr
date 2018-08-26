package actors

import actors.CommActor._
import actors.Messages.EventDataMessage.{ApplyPatchMsg, CreateFileMsg, DeleteFileMsg, DiffEventMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg}
import akka.actor.{ActorRef, ActorSelection}
import akka.routing.{BroadcastRoutingLogic, Routee, Router}

class CommActor(private val url: String, private val diffActor: ActorRef,
                private val fileHandlerActor: ActorRef) extends BasicActor {
  def receive: Receive = handleMassages(Map.empty)

  private val protocol = "akka.tcp"

  private var router: Router = Router(BroadcastRoutingLogic(), Vector.empty[Routee])

  def handleMassages(connections: Map[String, ActorSelection]): Receive = {
    case HasConnectionQuery(msgUrl) =>
      log.info(s"$getClassName got an HasConnectionQuery for msgUrl $msgUrl")
      sender() ! connections.contains(msgUrl)

    case AddRemoteConnectionMsg(msgUrl, port, actorPathStr) =>
      log.info(s"$getClassName got an AddRemoteConnectionMsg for msgUrl $msgUrl, port $port, actor $actorPathStr")
      if (port <= 0) log.warning(s"port $port <= 0 !")
      else {
        msgUrl match {
          case null => log.info(s"$getClassName got a null msgUrl")
          case "" => log.info(s"$getClassName got an empty msgUrl")
          case _ =>
            if (!connections.contains(msgUrl) && isValidUrl(msgUrl)) {
              val selection = context.actorSelection(createRemotePath(msgUrl, port, actorPathStr))
              router = router.addRoutee(selection)
              context become handleMassages(connections.updated(msgUrl, selection))
            }
            else log.info(s"$getClassName got an invalid msgUrl $msgUrl")
        }
      }

    case RemoveRemoteConnectionMsg(urlToRemove, msg) =>
      log.info(s"$getClassName got an RemoveRemoteConnectionMsg for url $urlToRemove with msg $msg")
      if (urlToRemove != null && connections.contains(urlToRemove)) {
        router = router.removeRoutee(connections.getOrElse[ActorSelection](urlToRemove, context.actorSelection(""))) //TODO maybe else part is not good
        context become handleMassages(connections - urlToRemove)
      }

    case DisconnectMsg(msg) =>
      log.info(s"$getClassName got an DisconnectMsg with msg: $msg")
      router.route(RemoveRemoteConnectionMsg(url, msg), context.self)

    case fileDeletedMsg: FileDeletedMsg =>
      val file = fileDeletedMsg.file
      if (!fileDeletedMsg.isRemote) {
        log.info(s"$getClassName got an FileDeletedMsg for file $file, routing to ${router.routees.size} routees")
        router.route(fileDeletedMsg, context.self)
      }
      else {
          log.info(s"$getClassName got an FileDeletedMsg for file $file with isRemote = true, s" +
            s"ending DeleteFileMsg to fileHandler")
          fileHandlerActor ! DeleteFileMsg(file.path)
        }

    case fileCreatedMsg : FileCreatedMsg =>
      val file = fileCreatedMsg.file
      if (!fileCreatedMsg.isRemote) {
        log.info(s"$getClassName got an FileCreatedMsg for file $file, routing to ${router.routees.size} routees")
        router.route(fileCreatedMsg, context.self)
      }
      else {
          log.info(s"$getClassName got an FileCreatedMsg for file $file with isRemote = true, s" +
            s"ending DeleteFileMsg to fileHandler")
          fileHandlerActor ! CreateFileMsg(file.path)
      }

    case diffEventMsg: DiffEventMsg =>
      val path = diffEventMsg.path
      val isRemote = diffEventMsg.isRemote
      log.info(s"$getClassName got an DiffEventMsg for path $path, isRemote? $isRemote")
      if (!isRemote) {
        log.info(s"$getClassName routing DiffEventMsg for path $path to ${router.routees.size} routees")
        router.route(diffEventMsg, context.self)
      }
      else {
        //TODO ApplyPatchMsg send to DiffActor
        diffActor ! ApplyPatchMsg(path, diffEventMsg.patch)
      }


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
  case class RemoveRemoteConnectionMsg(url: String, msg: Option[String] = None) extends RemoteConnectionMsg
  case class HasConnectionQuery(url: String) extends RemoteConnectionMsg

  case class DisconnectMsg(msg: Option[String] = None) extends RemoteConnectionMsg
}
