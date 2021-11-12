package part4_wordcounting

import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.{ActorRef, Behavior}

import scala.concurrent.duration.DurationInt

object Client {
  sealed trait Event
  private case object Tick extends Event
  private case class ServiceResponse(result: CountService.Response) extends Event

  def apply( service: ActorRef[CountService.ProcessText]): Behavior[Event] =
    Behaviors.setup { ctx =>
      Behaviors.withTimers { timers =>
        timers.startTimerWithFixedDelay(Tick, Tick, 2.seconds)
        val responseAdapter = ctx.messageAdapter(ServiceResponse)

        Behaviors.receiveMessage {
          case Tick =>
            ctx.log.info("Sending process request")
            service ! CountService.ProcessText("this is the text that will be analyzed", responseAdapter)
            Behaviors.same

          case ServiceResponse(result) =>
            ctx.log.info("Service result: {}", result)
            Behaviors.same
        }


      }
    }

}
