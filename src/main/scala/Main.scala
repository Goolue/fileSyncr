import actors.{DiffActor, FileHandlerActor, FileWatcherConfigurer}
import akka.actor.{ActorRef, ActorSystem, Props}
import better.files.File

object Main extends App {
  override def main(args: Array[String]): Unit = {
    val fileToWatch: File = File.currentWorkingDirectory / "src" / "test" / "resources"
    val system = ActorSystem("someSys")
    lazy val fileHandler: ActorRef = system.actorOf(Props(new FileHandlerActor(diffActor, ActorRef.noSender)))
    lazy val diffActor = system.actorOf(Props(new DiffActor(ActorRef.noSender, fileHandler)))
    var configurer: FileWatcherConfigurer = new FileWatcherConfigurer(system, fileHandler, fileToWatch)

    while (true) {}
  }
}
