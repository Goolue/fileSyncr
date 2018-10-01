package actors

import java.nio.file.{StandardWatchEventKinds => EventType}

import actors.Messages.FileEventMessage.{FileCreatedMsg, FileDeletedMsg, FileModifiedMsg}
import akka.actor.{ActorRef, ActorSystem}
import better.files.FileWatcher._
import better.files._
import utils.FileUtils

// References:
// https://github.com/pathikrit/better-files/tree/master/akka
// https://github.com/pathikrit/better-files#akka-file-watcher

object FileWatcherConfigurer {

  def watch(actorSystem: ActorSystem, fileHandler: ActorRef, fileToWatch: File): ActorRef = {
    actorSystem.log.info(s"watching $fileToWatch for $fileHandler")

    val watcher: ActorRef = fileToWatch.newWatcher()(actorSystem)
    watcher ! when(events = EventType.ENTRY_CREATE, EventType.ENTRY_MODIFY, EventType.ENTRY_DELETE) {
      case (EventType.ENTRY_CREATE, file) =>
        actorSystem.log.info(s"$file got created")
        fileHandler ! FileCreatedMsg(FileUtils.getFileAsRelativeStr(file))
      case (EventType.ENTRY_MODIFY, file) =>
        actorSystem.log.info(s"$file got modified")
        fileHandler ! FileModifiedMsg(FileUtils.getFileAsRelativeStr(file))
      case (EventType.ENTRY_DELETE, file) =>
        actorSystem.log.info(s"$file got deleted")
        fileHandler ! FileDeletedMsg(FileUtils.getFileAsRelativeStr(file))
    }

    watcher
  }

}
