package extensions

import akka.actor._

/**
  * An extension that easily allows getting the port and address of an ActorSystem.
  *
  * Take from: https://stackoverflow.com/a/50840215
  * @param system the ActorSystem to use.
  */

class AddressExtension(system: ExtendedActorSystem) extends Extension {
  val address: Address = system.provider.getDefaultAddress
}

object AddressExtension extends ExtensionId[AddressExtension] {
  def createExtension(system: ExtendedActorSystem): AddressExtension = new AddressExtension(system)

  def hostOf(system: ActorSystem): String = AddressExtension(system).address.host.getOrElse("")
  def portOf(system: ActorSystem): Int    = AddressExtension(system).address.port.getOrElse(0)
}