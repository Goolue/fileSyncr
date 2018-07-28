package actors

import akka.actor.{Actor, ActorLogging}

abstract class BasicActor extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"$getClassName Starting")
  }

  private def getClassName = this.getClass.getSimpleName

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, s"$getClassName Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }

}
