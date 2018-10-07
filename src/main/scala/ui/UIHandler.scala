package ui

import actors.ActorsContainer

abstract class UIHandler(actorsContainer: ActorsContainer) {
  def displayMainScreen(): Unit
  def displayConnectToSomeoneScreen(): Unit

  // TODO more
}
