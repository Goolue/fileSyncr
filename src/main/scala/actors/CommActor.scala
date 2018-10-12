package actors

import java.util.concurrent.TimeUnit

import actors.CommActor._
import actors.Messages.EventDataMessage.{ApplyPatchMsg, DiffEventMsg}
import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg}
import actors.Messages.Message
import akka.actor.{ActorRef, ActorSelection}
import akka.event.LoggingReceive
import akka.routing.{BroadcastRoutingLogic, Routee, Router}

import scala.concurrent.duration.FiniteDuration

class CommActor(private val url: String, private val diffActor: ActorRef,
                private val fileHandlerActor: ActorRef) extends BasicActor {

  log.debug(s"${self.path.toStringWithoutAddress} created with url $url")

  def receive: Receive = handleMassages(Map.empty, Router(BroadcastRoutingLogic(), Vector.empty[Routee]))

  private val protocol = "akka.tcp"

//  private var router: Router = Router(BroadcastRoutingLogic(), Vector.empty[Routee])

  def handleMassages(connections: Map[String, ActorSelection], router: Router): Receive = LoggingReceive {
    case HasConnectionQuery(msgUrl) =>
      val res = connections.contains(msgUrl)
      log.debug(s"$getClassName got an HasConnectionQuery for msgUrl $msgUrl, returning $res")
      sender() ! res

    case AddRemoteConnectionMsg(msgUrl, port, actorPathStr, systemName, verifyConnection) =>
      log.debug(s"$getClassName got an AddRemoteConnectionMsg for msgUrl $msgUrl, port $port, actor $actorPathStr, " +
        s"system $systemName")
      if (port <= 0) log.warning(s"port $port <= 0 !")
      else {
        msgUrl match {
          case null => log.warning(s"$getClassName got a null msgUrl")
          case "" => log.warning(s"$getClassName got an empty msgUrl")
          case _ =>
            if (!connections.contains(msgUrl) && isValidUrl(msgUrl)) {
              val generatedActorPath = createRemotePath(msgUrl, systemName, port, actorPathStr)
              log.debug(s"generated actor path for selection: $generatedActorPath")
              val selection = context.actorSelection(generatedActorPath)

              // see if the connection is successful, wait up to 3 seconds for the result
              if (verifyConnection) {
                selection.resolveOne(FiniteDuration.apply(5, TimeUnit.SECONDS))
                  .onComplete(t => {
                    if (t.isFailure) {
                      log.error(t.failed.get.getMessage)
                    }
                    else {
                      log.debug(s"connection successful with actor ${t.get.path}")
                      context become handleMassages(connections.updated(msgUrl, selection), router.addRoutee(selection))
                    }
                  })(context.dispatcher)
              } else {
                log.warning(s"connection added without verification for url $msgUrl")
                context become handleMassages(connections.updated(msgUrl, selection), router.addRoutee(selection))
              }
            }
            else log.warning(s"$getClassName got an invalid msgUrl $msgUrl")
        }
      }

    case RemoveRemoteConnectionMsg(urlToRemove, msg) =>
      log.debug(s"$getClassName got an RemoveRemoteConnectionMsg for url $urlToRemove with msg $msg")
      if (urlToRemove != null && connections.contains(urlToRemove)) {
        val newRouter = router.removeRoutee(connections.getOrElse[ActorSelection](urlToRemove, context.actorSelection(""))) //TODO maybe else part is not good
        context become handleMassages(connections - urlToRemove, newRouter)
      }

    case DisconnectMsg(msg) =>
      log.debug(s"$getClassName got an DisconnectMsg with msg: $msg")
      router.route(RemoveRemoteConnectionMsg(url, msg), context.self)

    case FileDeletedMsg(path, isRemote) =>
      if (isRemote) {
        log.debug(s"$getClassName got an FileDeletedMsg for path $path with isRemote = true, s" +
          s"ending FileDeletedMsg to fileHandler")
        fileHandlerActor ! FileDeletedMsg(path, isRemote = true)
      } else {
        log.debug(s"$getClassName got an FileDeletedMsg for path $path, routing to ${router.routees.size} routees")
        router.route(FileDeletedMsg(path, isRemote = true), context.self)
      }

    case FileCreatedMsg(path, isRemote) =>
      if (isRemote) {
        log.debug(s"$getClassName got an FileCreatedMsg for path $path with isRemote = true, " +
          s"sending FileCreatedMsg to fileHandler $fileHandlerActor")
        fileHandlerActor ! FileCreatedMsg(path, isRemote = true)
      } else {
        log.debug(s"$getClassName got an FileCreatedMsg for path $path, routing to ${router.routees.size} routees")
        router.route(FileCreatedMsg(path, isRemote = true), context.self)
      }

    case DiffEventMsg(path, patch, isRemote) =>
      log.debug(s"$getClassName got an DiffEventMsg for path $path, isRemote? $isRemote")
      if (isRemote) {
        diffActor ! ApplyPatchMsg(path, patch.toPatch)
      } else {
        log.debug(s"$getClassName routing DiffEventMsg for path $path to ${router.routees.size} routees")
        router.route(DiffEventMsg(path, patch, isRemote = true), context.self)
      }


    //TODO more msgs

    case msg => log.warning(s"$getClassName got an unidentified msg $msg")

  }

  private def isValidUrl(url: String) = {
    url.forall(c => c.isLetterOrDigit || c == '.')
  }

  private def createRemotePath (url: String, systemName: Option[String], port: Int, actorClass: String): String = {
    s"$protocol://${systemName.getOrElse(context.system.name)}@$url:$port/user/$actorClass"
  }
}

object CommActor {
  sealed trait RemoteConnectionMsg extends Message
  case class AddRemoteConnectionMsg(url: String, port: Int, actorClass: String,
                                    systemName: Option[String] = None, verifyConnection: Boolean = true) extends RemoteConnectionMsg
  case class RemoveRemoteConnectionMsg(url: String, msg: Option[String] = None) extends RemoteConnectionMsg
  case class HasConnectionQuery(url: String) extends RemoteConnectionMsg
  case class DisconnectMsg(msg: Option[String] = None) extends RemoteConnectionMsg
}
