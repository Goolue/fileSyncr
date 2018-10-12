package utils

import java.io.{BufferedReader, InputStreamReader}
import java.net.{InetAddress, NetworkInterface, URL}

import org.fourthline.cling.UpnpServiceImpl
import org.fourthline.cling.model.meta.{LocalDevice, RemoteDevice}
import org.fourthline.cling.registry.{Registry, RegistryListener}

import org.fourthline.cling.support.igd.PortMappingListener
import org.fourthline.cling.support.model.PortMapping

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
        .filter(address => !address.getHostAddress.contains(':') && address.isReachable(2))
        .map(address => address.getHostAddress)
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

  def getUPnPService: UpnpServiceImpl = {
    val listener = new RegistryListener {
      override def remoteDeviceAdded(registry: Registry, device: RemoteDevice): Unit =
        println(s"remote device added. device: ${device.getDisplayString}, registry: $registry")

      override def remoteDeviceRemoved(registry: Registry, device: RemoteDevice): Unit =
        println(s"remote device removed. device: ${device.getDisplayString}, registry: $registry")

      override def localDeviceRemoved(registry: Registry, device: LocalDevice): Unit =
        println(s"local device added. device: ${device.getDisplayString}, registry: $registry")

      override def localDeviceAdded(registry: Registry, device: LocalDevice): Unit =
        println(s"local device added. device: ${device.getDisplayString}, registry: $registry")

      override def afterShutdown(): Unit =
        println(s"after shutdown")

      override def beforeShutdown(registry: Registry): Unit =
        println(s"before shutdown. registry: $registry")

      override def remoteDeviceUpdated(registry: Registry, device: RemoteDevice): Unit =
        println(s"discovery updated: ${device.getDisplayString}")

      override def remoteDeviceDiscoveryStarted(registry: Registry, device: RemoteDevice): Unit =
        println(s"discovery started: ${device.getDisplayString}")

      override def remoteDeviceDiscoveryFailed(registry: Registry, device: RemoteDevice, ex: Exception): Unit =
        println(s"discovery failed: ${device.getDisplayString}, ex: $ex")
    }

    println("starting Cling")

    new UpnpServiceImpl(listener)
  }

  def bindPort(upnpService: UpnpServiceImpl, port: Int, protocol: PortMapping.Protocol,
               internalClient: String = InetAddress.getLocalHost.getHostAddress): Unit = {
    println(s"binding port $port")
    val registryListener = new PortMappingListener(new PortMapping(port, internalClient, protocol))
    upnpService.getRegistry.addListener(registryListener)
  }



}
