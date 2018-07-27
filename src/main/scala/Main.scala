import actors.{FileHandlerActor, FileWatcherConfigurer}
import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File

object Main extends App {
  override def main(args: Array[String]): Unit = {
    val fileToWatch: File = File.currentWorkingDirectory / "src" / "test" / "resources"
    val system = ActorSystem("someSys")
    var fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(ActorRef.noSender, ActorRef.noSender)))
    var configurer: FileWatcherConfigurer = new FileWatcherConfigurer(system, fileHandler, fileToWatch)

    while (true) {}
  }
}
