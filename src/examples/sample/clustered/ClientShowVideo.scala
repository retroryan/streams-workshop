package sample.clustered


import language.postfixOps
import scala.concurrent.duration._
import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import java.util.concurrent.atomic.AtomicInteger
import sample.clustered.ClusteredMessages.{StartVideo, BackendRegistration}
import org.reactivestreams.api.Consumer
import video.Frame
import org.reactivestreams.spi.Subscriber
import akka.actor.Terminated
import sample.clustered.ClusteredMessages.StartVideo

//#frontend
class ClientShowVideo extends Actor {

  var backends = IndexedSeq.empty[ActorRef]


  def receive = {


    case BackendRegistration if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()
      println(s"recieved backend registration message - sending a start video")
      val (consumer, consumerActorRef) = video.Display.createActorRef(context.system)
      println(s"consumerActorRef: = ${consumerActorRef.path.name}  and address: ${consumerActorRef.path.address}")
      sender() ! StartVideo(consumerActorRef.path.name)

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }
}

//#frontend

object ClientShowVideo {
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