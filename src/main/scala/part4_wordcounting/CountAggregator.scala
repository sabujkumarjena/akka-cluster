package part4_wordcounting

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.duration.DurationInt


object CountAggregator {
  sealed trait Event
  private case object Timeout extends Event
  private case class CalculationComplete(length: Int) extends Event

  def apply(words: Seq[String], workers: ActorRef[CountWorker.Count], replyTo: ActorRef[CountService.Response]): Behavior[Event]=
    Behaviors.setup { ctx =>
      ctx.setReceiveTimeout(3.seconds, Timeout)
      val responseAdaptor = ctx.messageAdapter[CountWorker.Counted](counted => CalculationComplete(counted.length))
      words.foreach { word =>
        workers ! CountWorker.Count(word, responseAdaptor)
      }
      waiting(replyTo, words.size, Nil)
    }

  private def waiting(replyTo: ActorRef[CountService.Response], expectedResponses: Int, results: List[Int]): Behavior[Event]=
    Behaviors.receiveMessage {
      case CalculationComplete(length) =>
        val newResults = results :+ length
        if (newResults.size == expectedResponses) {
          val meanWordLength = newResults.sum.toDouble / newResults.size
          replyTo ! CountService.JobResult(meanWordLength)
          Behaviors.stopped
        } else {
          waiting(replyTo, expectedResponses, newResults)
        }
      case Timeout =>
        replyTo ! CountService.JobFailed("Service unavailable, try again later")
        Behaviors.stopped
    }
}
