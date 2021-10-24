package part2_clustering

import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import com.typesafe.config.ConfigFactory


object ClusteringBasics  {
  object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { context =>
      // Create an actor that handles cluster domain events
      context.spawn(ClusterSubscriber(), "ClusterListener")

      Behaviors.empty
    }
  }

// val ports =  Seq(25251, 25252, 0)

  def startup(port: Int): Unit = {
    // Override the configuration of the port
    val config = ConfigFactory.parseString(s"""
      akka.remote.artery.canonical.port=$port
      """).withFallback(ConfigFactory.load("part2_clustering/clusteringBasics.conf"))

    // Create an Akka system
    ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config) //all the actor system in the cluster must have same name
  }
  def main(args: Array[String]): Unit = {
    val ports =
      if (args.isEmpty)
        Seq(25251, 25252, 0)
      else
        args.toSeq.map(_.toInt)
    ports.foreach(startup)
  }
 // ports.foreach(startup)
  //println("sabuj")
}

/*
Build distributed applications
- decentralised, peer-to-peer
- no single point of failure
- automatic node membership and gossip protocol
- failure detector

Clustering is based on Remoting
- in most cases, use Clustering instead of Remoting

Clusters are composed of member nodes
- node = host + port + UID
- on the same JVM
- on multiple JVMs on the same machine
- on a set of machines of any scale

Cluster membership
- convergent gossip protocol
- no leader election - leader is deterministically chosen

Join a Cluster
1.Contact seed nodes in order (from configuration)
    - If I am the first seed node, I will join myself
    - Send a Join command to the seed node that responds first
2. Node is in the "joining" state
    - wait for gossip to converge
    - all nodes in the cluster must acknowledge the new node
3. The leader will set the state of the new node to "up"

Leave a cluster
Option 1: safe and quiet
- node switches its state to "leaving"
- gossip converges
- leader sets the state to "exiting"
- gossip converges
- leader marks it "removed"
Option 2: the hard way
- a node becomes "unreachable"
- gossip convergence and leader actions are not possible
- must be removed (downed) manually
- can also be auto-downed by the leader

      ***DO NOT use automatic downing in production****

When To Use Clustering

In a distributed application
- tightly coupled
- easier codebase, especially when starting out
- single artifacts
- all the benefits of clustering: availability, fault tolerance etc

Within microservices
- nodes in a service are tightly coupled
- the service has the benefits of clustering, fault tolerance etc
- the microservices can be loosely coupled with each other

When NOT to use clustering
- for inter-microservice communication
- distributed monolith
 */