package ui

import akka.actor.ActorSystem

abstract class UIHandler(actorSystem: ActorSystem) {
  def displayMainScreen(): Unit
  def displayConnectToSomeoneScreen(): Unit

  // TODO more
}
