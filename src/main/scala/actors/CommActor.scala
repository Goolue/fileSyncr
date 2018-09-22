package actors

import actors.CommActor._
import actors.Messages.EventDataMessage.{ApplyPatchMsg, CreateFileMsg, DeleteFileMsg, DiffEventMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg}
import akka.actor.{ActorRef, ActorSelection}
import akka.routing.{BroadcastRoutingLogic, Routee, Router}

class CommActor(private val url: String, private val diffActor: ActorRef,
                private val fileHandlerActor: ActorRef) extends BasicActor {
  println(s"${self.path} created with url $url")

  def receive: Receive = handleMassages(Map.empty, Router(BroadcastRoutingLogic(), Vector.empty[Routee]))

  private val protocol = "akka.tcp"

//  private var router: Router = Router(BroadcastRoutingLogic(), Vector.empty[Routee])

  def handleMassages(connections: Map[String, ActorSelection], router: Router): Receive = {
    case HasConnectionQuery(msgUrl) =>
      log.info(s"$getClassName got an HasConnectionQuery for msgUrl $msgUrl")
      sender() ! connections.contains(msgUrl)

    case AddRemoteConnectionMsg(msgUrl, port, actorPathStr, systemName) =>
      log.info(s"$getClassName got an AddRemoteConnectionMsg for msgUrl $msgUrl, port $port, actor $actorPathStr")
      if (port <= 0) log.warning(s"port $port <= 0 !")
      else {
        msgUrl match {
          case null => log.info(s"$getClassName got a null msgUrl")
          case "" => log.info(s"$getClassName got an empty msgUrl")
          case _ =>
            if (!connections.contains(msgUrl) && isValidUrl(msgUrl)) {
              val selection = context.actorSelection(createRemotePath(msgUrl, systemName, port, actorPathStr))
//              router = router.addRoutee(selection)
              context become handleMassages(connections.updated(msgUrl, selection), router.addRoutee(selection))
            }
            else log.info(s"$getClassName got an invalid msgUrl $msgUrl")
        }
      }

    case RemoveRemoteConnectionMsg(urlToRemove, msg) =>
      log.info(s"$getClassName got an RemoveRemoteConnectionMsg for url $urlToRemove with msg $msg")
      if (urlToRemove != null && connections.contains(urlToRemove)) {
        val newRouter = router.removeRoutee(connections.getOrElse[ActorSelection](urlToRemove, context.actorSelection(""))) //TODO maybe else part is not good
        context become handleMassages(connections - urlToRemove, newRouter)
      }

    case DisconnectMsg(msg) =>
      log.info(s"$getClassName got an DisconnectMsg with msg: $msg")
      router.route(RemoveRemoteConnectionMsg(url, msg), context.self)

    case fileDeletedMsg: FileDeletedMsg =>
      val path = fileDeletedMsg.path
      if (!fileDeletedMsg.isRemote) {
        log.info(s"$getClassName got an FileDeletedMsg for path $path, routing to ${router.routees.size} routees")
        router.route(fileDeletedMsg, context.self)
      }
      else {
          log.info(s"$getClassName got an FileDeletedMsg for path $path with isRemote = true, s" +
            s"ending DeleteFileMsg to fileHandler")
          fileHandlerActor ! DeleteFileMsg(path)
        }

    case FileCreatedMsg(path, isRemote) =>
      if (isRemote) {
        log.info(s"$getClassName got an FileCreatedMsg for path $path with isRemote = true, s" +
          s"ending FileCreatedMsg to fileHandler")
        fileHandlerActor ! CreateFileMsg(path)
      } else {
        log.info(s"$getClassName got an FileCreatedMsg for path $path, routing to ${router.routees.size} routees")
        router.route(FileCreatedMsg(path, isRemote = true), context.self)
      }

    case diffEventMsg: DiffEventMsg =>
      val path = diffEventMsg.path
      val isRemote = diffEventMsg.isRemote
      log.info(s"$getClassName got an DiffEventMsg for path $path, isRemote? $isRemote")
      if (isRemote) {
        diffActor ! ApplyPatchMsg(path, diffEventMsg.patch)
      } else {
        log.info(s"$getClassName routing DiffEventMsg for path $path to ${router.routees.size} routees")
        router.route(diffEventMsg, context.self)
      }


    //TODO more msgs

    case msg => log.warning(s"$getClassName got an unidentified msg $msg")

  }

  private def isValidUrl(url: String) = {
    url.forall(c => c.isLetterOrDigit || c == '.')
  }

  private def createRemotePath (url: String, systemName: Option[String], port: Int, actorClass: String): String = {
    s"$protocol://${systemName.getOrElse(context.system.name)}@$url:$port$actorClass"
  }
}

object CommActor {
  sealed class RemoteConnectionMsg
  case class AddRemoteConnectionMsg(url: String, port: Int, actorClass: String, systemName: Option[String] = None) extends RemoteConnectionMsg
  case class RemoveRemoteConnectionMsg(url: String, msg: Option[String] = None) extends RemoteConnectionMsg
  case class HasConnectionQuery(url: String) extends RemoteConnectionMsg

  case class DisconnectMsg(msg: Option[String] = None) extends RemoteConnectionMsg
}
