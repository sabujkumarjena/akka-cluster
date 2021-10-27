package part3_transformation

import akka.actor.typed.receptionist.Receptionist
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior}
import akka.util.Timeout

import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}


object Frontend {

  sealed trait Event
  private case object Tick extends Event
  private final case class WorkersUpdated(newWorkers: Set[ActorRef[Worker.TransformText]]) extends Event
  private final case class TransformCompleted(originalText: String, transformedText: String) extends Event
  private final case class JobFailed(why: String, text: String) extends Event

  def apply(): Behavior[Event] = Behaviors.setup { ctx =>
    Behaviors.withTimers { timer =>
      // subscribe to available workers
      val subscriptionAdapter = ctx.messageAdapter[Receptionist.Listing]{
        case Worker.WorkerServiceKey.Listing(workers) => WorkersUpdated(workers)
      }
      ctx.system.receptionist ! Receptionist.subscribe(Worker.WorkerServiceKey, subscriptionAdapter)
      timer.startTimerWithFixedDelay(Tick, Tick, 2.seconds)
      running(ctx, IndexedSeq.empty, jobCounter = 0)
    }
  }

  private def running(ctx: ActorContext[Event], workers: IndexedSeq[ActorRef[Worker.TransformText]], jobCounter: Int): Behavior[Event] =
    Behaviors.receiveMessage { message =>
      message match {
        case WorkersUpdated(newWorkers) =>
          ctx.log.info("List of services registered with the receptionist changed: {}", newWorkers)
          running(ctx, newWorkers.toIndexedSeq, jobCounter)
        case Tick =>
          if (workers.isEmpty) {
            ctx.log.warn("Got tick request but no workers available, not sending any work")
            Behaviors.same
          } else {
            // how much time can pass before we consider a request failed
            implicit val timeout = Timeout(5.seconds)
            val selectedWorker = workers(jobCounter % workers.size)
            ctx.log.info("Sending work for processing to {}", selectedWorker)
            val text = s"hello-$jobCounter"
            ctx.ask(selectedWorker, Worker.TransformText(text, _)){
              case Success(transformedText) => TransformCompleted(text, transformedText.text)
              case Failure(exception) => JobFailed(exception.getMessage, text)
            }
            running(ctx, workers, jobCounter + 1)
          }
        case TransformCompleted(originalText, transformedText) =>
          ctx.log.info("Got completed transform of {}: {}", originalText, transformedText)
          Behaviors.same

        case JobFailed(why, text) =>
          ctx.log.warn("Transformation of text {} failed. Because: {}", text, why)
          Behaviors.same
      }
    }

}
