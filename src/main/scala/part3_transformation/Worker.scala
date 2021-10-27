package part3_transformation

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.scaladsl.Behaviors

object Worker {

  sealed trait Command
  final case class TransformText(text: String, replyTo: ActorRef[TextTransformed]) extends Command with CborSerializable
  final case class TextTransformed(text: String) extends CborSerializable

  val WorkerServiceKey = ServiceKey[Worker.TransformText]("Worker")

  def apply(): Behavior[Command] = Behaviors.setup { ctx =>
      // each worker registers themselves with the receptionist
      ctx.log.info("Registering myself with receptionist")
      ctx.system.receptionist ! Receptionist.Register(WorkerServiceKey, ctx.self)

      Behaviors.receiveMessage {
        case TransformText(text, replyTo) =>
          //Thread.sleep(2000)
          replyTo ! TextTransformed(text.toUpperCase)
          Behaviors.same
      }
    }

}
