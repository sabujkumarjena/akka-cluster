package part4_wordcounting

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors}

object CountWorker {

  trait Command
  final case class Count(word: String, replyTo: ActorRef[Counted]) extends Command
    with CborSerializable
  final case class Counted(word: String, length: Int) extends Command
    with CborSerializable
  private case object ClearCache extends Command

  def apply():Behavior[Command]= Behaviors.setup { ctx =>
    withCache(ctx, Map.empty)
  }

  private def withCache(ctx: ActorContext[Command],
                        cache: Map[String, Int]): Behavior[Command] =
    Behaviors.receiveMessage {
      case Count(word, replyTo) =>
        ctx.log.info("Worker processing request [{}]", word)
        cache.get(word) match {
          case Some(len) =>
            replyTo ! Counted(word,len)
            Behaviors.same
          case None =>
            val len = word.length
            val updatedCache = cache + (word -> len)
            replyTo ! Counted(word, len)
            withCache(ctx, updatedCache)
        }
      case ClearCache =>
        withCache(ctx, Map.empty)
    }
}
