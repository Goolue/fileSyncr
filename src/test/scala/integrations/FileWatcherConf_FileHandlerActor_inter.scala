//package integrations
//
//import java.util.concurrent.TimeUnit
//
//import actors.Messages.FileEventMessage.FileCreatedMsg
//import actors.{FileHandlerActor, FileWatcherConfigurer}
//import akka.actor.{ActorRef, ActorSystem, Props}
//import akka.testkit.{ImplicitSender, TestKit}
//import better.files.File
//import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, Matchers, WordSpecLike}
//
//import scala.concurrent.duration.Duration
//
//class FileWatcherConf_FileHandlerActor_inter extends TestKit(ActorSystem("MySpec")) with ImplicitSender
//  with WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfter {
//
//  val fileToWatch: File = File.currentWorkingDirectory / "src" / "test" / "resources"
//  var fileHandler: ActorRef = _
//  var configurer: FileWatcherConfigurer = _
//
//  before {
//    fileHandler = system.actorOf(Props(new FileHandlerActor(testActor, testActor)))
//    configurer = new FileWatcherConfigurer(system, fileHandler, fileToWatch)
//  }
//
//  after {
//    fileHandler = null
//    configurer = null
//  }
//
//  override def afterAll {
//    TestKit.shutdownActorSystem(system)
//  }
//
//  "FileHandlerActor" must {
//    "receive a FileCreatedMsg(file) when a file is created" in {
//      //create a file
////      val file = fileToWatch.createChild("someNewFileCreated.txt")
//      val file = File.apply(fileToWatch.toString() + "/someNewFile")
//        .deleteOnExit().createIfNotExists()
//      expectMsg(Duration.apply(1, TimeUnit.DAYS), FileCreatedMsg(file))
//    }
//  }
//
//}
