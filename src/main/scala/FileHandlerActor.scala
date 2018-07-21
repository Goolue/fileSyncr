import java.nio.file.Path

import Messages.FileEventMessage.FileCreatedMsg
import akka.actor.{Actor, ActorRef}

import scala.collection.mutable

class FileHandlerActor(val diffActor: ActorRef) extends Actor{

  val pathToLines: mutable.Map[Path, Traversable[String]] = mutable.Map()

  def receive() = {
    case fileCreateMsg: FileCreatedMsg =>
      diffActor ! fileCreateMsg
//      val lines = fileCreateMsg.file.lines
//      pathToLines += (fileCreateMsg.file.path -> lines)
  }

//  def mapContains(path: Path): Boolean = pathToLines.contains(path)
}
