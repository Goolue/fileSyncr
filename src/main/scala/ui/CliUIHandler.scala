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

    print("Please enter the remote system's number")
    input = read()
    var sysNumOpt = tryToParseSystemNumber(input)
    while (sysNumOpt.isEmpty) {
      print("Invalid system number, please try again")
      input = read()
      sysNumOpt = tryToParseSystemNumber(input)
    }
    val systemNumber = sysNumOpt.get


    actorsContainer.addRemoteConnection(ip, port, systemNumber)
  }


}
