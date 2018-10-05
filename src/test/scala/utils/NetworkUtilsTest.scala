package utils

import java.net.{HttpURLConnection, InetAddress, URL}

import akka.actor.ActorSystem
import com.typesafe.config.{ConfigFactory, ConfigValueFactory}
import extensions.AddressExtension
import org.scalatest._

class NetworkUtilsTest extends WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {

  "getExternalIp" must {
    // TODO
    //    "be an accessible Ip" in {
    //      val ip = NetworkUtils.getExternalIp.get
    //      val url = new URL("http://" + ip)
    //      val connection = url.openConnection.asInstanceOf[HttpURLConnection]
    //      connection.setRequestMethod("GET")
    ////      connection.connect()
    //      val respCode = connection.getResponseCode
    //      respCode should be (200) // 200 = OK
    //      connection.disconnect()
    //    }

    "only contain dots, characters and numbers" in {
      val ip = NetworkUtils.getExternalIp.get
      ip.forall(c => c.isLetterOrDigit || c == '.') should be(true)
    }

    "not contain '..'" in {
      val ip = NetworkUtils.getExternalIp.get
      ip.contains("..") should be(false)
    }

    "have numbers with up to 3 digits" in {
      val ip = NetworkUtils.getExternalIp.get
      ip.split('.').forall(str => str.length <= 3 && !str.isEmpty) should be(true)
    }

    "have exactly 4 numbers" in {
      val ip = NetworkUtils.getExternalIp.get
      ip.split('.').length should be(4)
    }

    "have exactly 3 dots" in {
      val ip = NetworkUtils.getExternalIp.get
      var count = 0
      var index = ip.indexOf('.')
      while (index >= 0) {
        count += 1
        index = ip.indexOf('.', index + 1)
      }

      count should be(3)
    }
  }

  "getLocalIp" must {
    "only contain dots, characters and numbers" in {
      val ip = NetworkUtils.getLocalIp.get
      ip.forall(c => c.isLetterOrDigit || c == '.') should be(true)
    }

    "not contain '..'" in {
      val ip = NetworkUtils.getLocalIp.get
      ip.contains("..") should be(false)
    }

    "have numbers with up to 3 digits" in {
      val ip = NetworkUtils.getLocalIp.get
      ip.split('.').forall(str => str.length <= 3 && !str.isEmpty) should be(true)
    }

    "have exactly 4 numbers" in {
      val ip = NetworkUtils.getLocalIp.get
      ip.split('.').length should be(4)
    }

    "have exactly 3 dots" in {
      val ip = NetworkUtils.getLocalIp.get
      var count = 0
      var index = ip.indexOf('.')
      while (index >= 0) {
        count += 1
        index = ip.indexOf('.', index + 1)
      }

      count should be(3)
    }
  }

}
