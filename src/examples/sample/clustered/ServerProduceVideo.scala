package sample.clustered


import language.postfixOps
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.Member
import akka.cluster.MemberStatus
import com.typesafe.config.ConfigFactory
import sample.clustered.ClusteredMessages.BackendRegistration
import org.reactivestreams.api.Producer
import java.io.File
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import video.Frame
import sample.clustered.ClusteredMessages.StartVideo
import stream.actor.ActorConsumer

//#backend
class ServerProduceVideo extends Actor {

  val cluster = Cluster(context.system)

  val mp4 = new File("goose.mp4")

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def pathFor(address: Address, name: String): ActorPath =
    RootActorPath(address) / "user" / name

  def receive = {
    case StartVideo(consumerActorName) =>
      val clientAddress: Address = sender().path.address
      val consumerPath: ActorPath = pathFor(clientAddress, consumerActorName)
      val consumerSelection: ActorSelection = context.actorSelection(consumerPath)
      consumerSelection ! Identify("1")

    case ActorIdentity(correlationId, optConsumerActorRef) =>
      optConsumerActorRef.foreach {
        consumerActorRef =>
          println(s"received consumerActorRef = ${consumerActorRef} and path: ${consumerActorRef.path}")
          val fileProducer: Producer[Frame] = video.FFMpeg.readFile(mp4, context.system)
          val consumer = ActorConsumer[Frame](consumerActorRef)
          fileProducer.produceTo(consumer)
      }

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach register

    case MemberUp(m) => register(m)
    case msg => println(s"unhandled msg = $msg")
  }

  def register(member: Member): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        BackendRegistration
}

//#backend

object ServerProduceVideo {

  /**
   *
   * run:
   *      ./activator -Dconfig.resource=clustering.conf 'runMain sample.clustered.ServerProduceVideo 2551'
   */
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(Props[ServerProduceVideo], name = "backend")
  }
}