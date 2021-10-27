package part2_clustering

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import akka.cluster.typed.{Cluster, Subscribe}
import akka.cluster.ClusterEvent.{MemberEvent, MemberRemoved, MemberUp, ReachabilityEvent, ReachableMember, UnreachableMember}

object ClusterSubscriber {

  sealed trait Event

  //internal adapted cluster events only
  private final case class ReachabilityChange(reachabilityEvent: ReachabilityEvent ) extends Event
  private final case class MemberChange(event: MemberEvent) extends Event

  def apply(): Behavior[Event] = Behaviors.setup { ctx =>
    val memberEventAdapter: ActorRef[MemberEvent] = ctx.messageAdapter(MemberChange)
    val cluster = Cluster(ctx.system)
    cluster.subscriptions ! Subscribe(memberEventAdapter, classOf[MemberEvent]) //Subscribe(subscriber, eventClass)

    val reachabilityAdapter: ActorRef[ReachabilityEvent] = ctx.messageAdapter(ReachabilityChange)
    cluster.subscriptions ! Subscribe(reachabilityAdapter, classOf[ReachabilityEvent])


    Behaviors.receiveMessage { message =>
      message match {
        case ReachabilityChange(reachabilityEvent) =>
          reachabilityEvent match {
            case UnreachableMember(member) =>
              ctx.log.info("Member detected as unreachable: {}", member)
            case ReachableMember(member) =>
              ctx.log.info("Member back to reachable: {}", member)
          }
        case MemberChange(changeEvent) =>
          changeEvent match {
            case MemberUp(member) =>
              ctx.log.info("Member is Up: {}", member.address)
            case MemberRemoved(member, previousStatus) =>
              ctx.log.info("Member is removed : {} after {} ", member.address, previousStatus)
            case _:MemberEvent => //ignore
          }
      }
      Behaviors.same
    }

  }
}
