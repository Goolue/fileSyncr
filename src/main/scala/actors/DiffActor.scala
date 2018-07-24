package actors

import actors.Messages.EventDataMessage.{DiffEventMsg, ModificationDataMsg}
import akka.actor.{Actor, ActorRef}
import com.github.difflib.DiffUtils
import com.github.difflib.patch.Patch

import scala.collection.JavaConverters._

class DiffActor(val commActor: ActorRef, val fileHandler : ActorRef) extends Actor{
  def receive() = {
    case ModificationDataMsg(path, newLines, oldLines) =>
      println("got a ModificationDataMsg")
      val patch: Patch[String] = oldLines match {
        case null => DiffUtils.diff(List[String]().asJava, newLines.toList.asJava)
        case _ => DiffUtils.diff(oldLines.toList.asJava, newLines.toList.asJava)
      }
      commActor ! DiffEventMsg(path, patch)
    case _ => println("got an unexpected msg!")
  }
}
