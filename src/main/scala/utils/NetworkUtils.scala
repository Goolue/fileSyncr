package utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{NetworkInterface, URL}

import scala.collection.JavaConverters._

object NetworkUtils {

  private def log(msg: String): Unit = {
    // TODO replace with something better
    println(msg)
  }

  // taken, with modifications, from https://stackoverflow.com/a/38393486
  def getExternalIp: Option[String] = {
    val whatIsMyIpURL = new URL("http://checkip.amazonaws.com")
    try {
      val in: BufferedReader = new BufferedReader(new InputStreamReader(whatIsMyIpURL.openStream()))
      Some(in.readLine())
    }
    catch {
      case e: Throwable =>
        log(s"could not get external IP. exception is: ${e.getMessage}")
        None
    }
  }

  // taken, with modifications, from https://stackoverflow.com/a/14364233
  def getLocalIp: Option[String] = {
    try {
      val addresses = NetworkInterface.getNetworkInterfaces.asScala
        .filter(interface => !interface.isLoopback && interface.isUp)
        .flatMap(interface => interface.getInetAddresses.asScala)
        .filter(address => !address.getCanonicalHostName.contains(':') && address.isReachable(2))
        .map(address => address.getCanonicalHostName)
        .toList
      log(s"found ${addresses.size} addresses: $addresses")
      addresses.find(_ => true)
    }
    catch {
      case e: Throwable =>
        log(s"could not get local IP. exception is: ${e.getMessage}")
        None
    }

  }

}
