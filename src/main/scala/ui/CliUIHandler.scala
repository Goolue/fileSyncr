package ui

import akka.actor.ActorSystem

class CliUIHandler(private val actorSystem: ActorSystem) extends UIHandler(actorSystem) with Output with Input {

  private def isValidIP(input: String) = {
    // TODO
    true
  }

  private def isValidPort(input: String) = {
    // TODO
    true
  }

  override def displayMainScreen(): Unit = {
    print("Hello!")
  }

  override def displayConnectToSomeoneScreen(): Unit = {
    print("Please enter an IP")
    var input = read()
    while (!isValidIP(input)) {
      print("Invalid IP, please try again")
      input = read()
    }
    print("Please enter a port")
    input = read()
    while (!isValidPort(input)) {
      print("Invalid port, please try again")
      input = read()
    }

    // TODO
  }
}
