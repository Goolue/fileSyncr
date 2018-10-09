package ui

import actors.ActorsContainer

abstract class UIHandler(actorsContainer: ActorsContainer) {
  def displayMainScreen(): Unit
  def displayConnectToSomeoneScreen(): Unit


  def tryToParseSystemNumber(name: String): Option[Int] = {
    try {
      val num = name.toInt
      if (num >= 0) Some(num)
      else None
    } catch {
      case _: Throwable => None
    }
  }
  // TODO more
}
