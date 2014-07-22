package sample.clustered


import language.postfixOps
import akka.actor._
import com.typesafe.config.ConfigFactory
import sample.clustered.ClusteredMessages.BackendRegistration
import akka.actor.Terminated
import sample.clustered.ClusteredMessages.StartVideo

//#frontend
class ClientShowVideo extends Actor {

  var backends = IndexedSeq.empty[ActorRef]

  def receive = {

    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()
      val consumerActorRef = video.Display.createActorRef(context.system)
      sender() ! StartVideo(consumerActorRef.path.name)

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
}

//#frontend
object ClientShowVideo {

  /**
   *
   * run:
   *      ./activator -Dconfig.resource=clustering.conf 'runMain sample.clustered.ClientShowVideo 2551'
   */
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    val frontend = system.actorOf(Props[ClientShowVideo], name = "frontend")

  }
}