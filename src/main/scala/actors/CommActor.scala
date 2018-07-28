package actors

import akka.actor.Actor

class CommActor extends Actor {
  //TODO all
  def receive = {
    case x => println(x)
  }

}
