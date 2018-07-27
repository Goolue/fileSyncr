package actors

import java.nio.file.{StandardWatchEventKinds => EventType}

import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import akka.actor.{ActorRef, ActorSystem}
import better.files.FileWatcher._
import better.files._

// References:
// https://github.com/pathikrit/better-files/tree/master/akka
// https://github.com/pathikrit/better-files#akka-file-watcher

class FileWatcherConfigurer(val actorSystem: ActorSystem, val fileHandler: ActorRef, val fileToWatch: File) {

    println(s"watching $fileToWatch")
    val watcher: ActorRef = fileToWatch.newWatcher()(actorSystem)
    watcher ! when(events = EventType.ENTRY_CREATE, EventType.ENTRY_MODIFY, EventType.ENTRY_DELETE) {
      case (EventType.ENTRY_CREATE, file) =>
        println(s"$file got created")
        fileHandler ! FileCreatedMsg(file)
      case (EventType.ENTRY_MODIFY, file) =>
        println(s"$file got modified")
        fileHandler ! FileModifiedMsg(file)
      case (EventType.ENTRY_DELETE, file) =>
        println(s"$file got deleted")
        fileHandler ! FileDeletedMsg(file)
    }
}
