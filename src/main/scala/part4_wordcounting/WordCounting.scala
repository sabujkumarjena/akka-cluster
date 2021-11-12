package part4_wordcounting

import akka.actor.typed.receptionist.{Receptionist, ServiceKey}
import akka.actor.typed.{ActorSystem, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, Routers}
import akka.cluster.typed.Cluster
import com.typesafe.config.ConfigFactory

object WordCounting {

  val CountServiceKey = ServiceKey[CountService.ProcessText](id = "CountService")
  private object RootBehavior {
    def apply(): Behavior[Nothing] = Behaviors.setup[Nothing] { ctx =>
      val cluster = Cluster(ctx.system)
      if (cluster.selfMember.hasRole("compute")) {
        // on every compute node there is one service instance that delegates to N local workers
        val numberOfWorkers =
          ctx.system.settings.config.getInt("count_service.workers-per-node")
        ctx.log.info("no of workers; {}", numberOfWorkers)

        val workers = ctx
        .spawn(Routers.pool(numberOfWorkers)(CountWorker().narrow[CountWorker.Count])
          // the worker has a per word cache, so send the same word to the same local worker child
          .withConsistentHashingRouting(1, _.word),
          "WorkerRouter"
        )
        val service = ctx.spawn(CountService(workers), "CountService")
        // published through the receiptionist to the other nodes in the cluster
        ctx.system.receptionist ! Receptionist.Register(CountServiceKey, service)
      }
      if (cluster.selfMember.hasRole(("client"))){
        val serviceRouter =
          ctx.spawn(Routers.group(CountServiceKey), "serviceRouter")
          ctx.spawn(Client(serviceRouter), "Client")

      }
      Behaviors.empty[Nothing]
    }
  }

  private def startup(role: String, port: Int): Unit = {

    // Override the configuration of the port when specified as program argument
    val config = ConfigFactory
      .parseString(s"""
      akka.remote.artery.canonical.port=$port
      akka.cluster.roles = [$role]
      """)
      .withFallback(ConfigFactory.load("part4_wordcounting/wordcounting"))

    ActorSystem[Nothing](RootBehavior(), "ClusterSystem", config)
  }

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      startup("compute", 25251)
      startup("compute", 25252)
      startup("compute", 0)
      startup("client", 0)
    } else {
      require(args.size == 2, "Usage: role port")
      startup(args(0), args(1).toInt)
    }
  }

}
