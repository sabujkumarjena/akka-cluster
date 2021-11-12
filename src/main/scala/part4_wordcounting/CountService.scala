package part4_wordcounting

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

object CountService {

  sealed trait Command extends CborSerializable
  final case class ProcessText(text: String, replyTo: ActorRef[Response]) extends Command {
    require(text.nonEmpty)
  }
  case object Stop extends Command

  sealed trait Response extends CborSerializable
  final case class JobResult(meanWordLength: Double) extends Response
  final case class JobFailed(reason: String) extends Response

  def apply(workers: ActorRef[CountWorker.Count]): Behavior[Command] =
    Behaviors.setup { context =>
      context.watch(workers)
      Behaviors.receiveMessage {
        case ProcessText(text, replyTo) =>
          context.log.info("Delegating request")
          val words = text.split(' ').toIndexedSeq
          // create per request actors that collects replies from workers
          context.spawnAnonymous(CountAggregator(words,workers, replyTo))
          Behaviors.same
        case Stop =>
          Behaviors.stopped
      }
    }
}
