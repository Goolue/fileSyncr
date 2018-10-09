package actors

import akka.actor.DeadLetter
import akka.event.LoggingReceive

class DeadLettersActor extends BasicActor {

  // register this actor to dead letters
  context.system.eventStream.subscribe(context.self, classOf[DeadLetter])

  override def receive: Receive = LoggingReceive {
    case msg: DeadLetter =>
      log.warning(s"dead letters got msg: $msg")
    case msg: Messages.Message => log.warning(s"dead letters got non-DeadLetter msg: $msg")
    case x => log.warning(s"dead letters got an UNRECOGNIZED msg $x")
  }
}
