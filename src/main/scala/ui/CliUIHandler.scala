package ui

import akka.actor.ActorSystem
import utils.NetworkUtils

class CliUIHandler(private val actorSystem: ActorSystem) extends UIHandler(actorSystem) with Output with Input {

  override def displayMainScreen(): Unit = {
    print("Hello!")
  }

  override def displayConnectToSomeoneScreen(): Unit = {
    print("Please enter an IP")
    var input = read()
    while (!NetworkUtils.isValidIp(input)) {
      print("Invalid IP, please try again")
      input = read()
    }
    print("Please enter a port")
    input = read()
    while (!NetworkUtils.isValidPort(input)) {
      print("Invalid port, please try again")
      input = read()
    }

    // TODO
  }
}
