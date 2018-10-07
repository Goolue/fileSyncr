package ui

import actors.ActorsContainer
import utils.NetworkUtils

class CliUIHandler(private val actorsContainer: ActorsContainer) extends UIHandler(actorsContainer) with Output with Input {

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
    val ip = input

    print("Please enter a port")
    input = read()
    while (!NetworkUtils.isValidPort(input)) {
      print("Invalid port, please try again")
      input = read()
    }
    val port = input.toInt

    actorsContainer.addRemoteConnection(ip, port)
  }


}
