package actors

import akka.actor.{Actor, ActorLogging}

abstract class BasicActor extends Actor with ActorLogging {

  override def preStart(): Unit = {
    log.info(s"$getClassName Starting")
  }

  def getClassName: String = this.getClass.getSimpleName

  override def preRestart(reason: Throwable, message: Option[Any]) {
    log.error(reason, s"$getClassName Restarting due to [{}] when processing [{}]",
      reason.getMessage, message.getOrElse(""))
  }

  //measuring elapsed time of a code block
  private def nanoToMilli(nano: Long): Long = nano / 1000000
  def time[R](block: => R): R = {
    val t0 = System.nanoTime()
    val result = block    // call-by-name
    val t1 = System.nanoTime()
    log.info("Elapsed time: " + nanoToMilli(t1 - t0) + "ms")
    result
  }

}
