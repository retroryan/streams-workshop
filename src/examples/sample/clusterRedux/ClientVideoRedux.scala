package sample.clusterRedux

import language.postfixOps
import akka.actor._
import sample.clustered.ClusteredMessages.{OpenFile, VideoFileActor, BackendRegistration}
import video.Frame
import com.typesafe.config.ConfigFactory
import stream.actor.ActorProducer
import akka.stream.scaladsl.Flow

//#frontend
class ClientVideoRedux extends Actor {

  var backends = IndexedSeq.empty[ActorRef]

  def pathFor(address: Address, name: String): ActorPath =
    RootActorPath(address) / "user" / name

  def receive = {

    case BackendRegistration(serverActorName) if !backends.contains(sender()) =>
      context watch sender()
      backends = backends :+ sender()
      setupServerStream(serverActorName)

    case VideoFileActor(videoServerActorRef) => {
      println(s"received videoServerActorRef = ${videoServerActorRef} and path: ${videoServerActorRef.path}")
      val serverProducer = ActorProducer[Frame](videoServerActorRef)
      val consumer = video.Display.create(context.system)
      serverProducer.produceTo(consumer)
    }

    case Terminated(a) =>
      backends = backends.filterNot(_ == a)
  }

  def setupServerStream(serverActorName: String) = {
    val serverAddress: Address = sender().path.address
    val serverPath: ActorPath = pathFor(serverAddress, serverActorName)
    val serverSelection: ActorSelection = context.actorSelection(serverPath)
    serverSelection ! OpenFile("goose.mp4")

  }
}

//#frontend

object ClientVideoRedux {
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [frontend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    val frontend = system.actorOf(Props[ClientVideoRedux], name = "frontend")

  }
}