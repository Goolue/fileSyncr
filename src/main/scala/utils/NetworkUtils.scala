package utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{NetworkInterface, URL}

import scala.collection.JavaConverters._

object NetworkUtils {

  private def log(msg: String): Unit = {
    // TODO replace with something better
    println(msg)
  }

  def isValidPort(input: String): Boolean = {
    val MAX_PORT_NUM = 65535
    try {
      val num = input.toInt
      0 < num && num <= MAX_PORT_NUM
    } catch {
      case _: Throwable => false
    }
  }

  def isValidIp(ip: String): Boolean = {
    ip.forall(c => c.isLetterOrDigit || c == '.') && !ip.contains("..") && {
      if (!ip.exists(c => c.isLetter)){
        val split = ip.split('.')
        split.length == 4 && split.forall(num => num.length > 0 && num.length <= 3)
      }
      else true
    }
  }
  
  // taken, with modifications, from https://stackoverflow.com/a/38393486
  def getExternalIp: Option[String] = {
    val whatIsMyIpURL = new URL("http://checkip.amazonaws.com")
    try {
      val in: BufferedReader = new BufferedReader(new InputStreamReader(whatIsMyIpURL.openStream()))
      val ip = in.readLine()
      Some(ip).filter(isValidIp)
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
      addresses.find(_ => true).filter(isValidIp)
    }
    catch {
      case e: Throwable =>
        log(s"could not get local IP. exception is: ${e.getMessage}")
        None
    }

  }

}
