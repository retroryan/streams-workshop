package sample.clusterRedux

import language.postfixOps
import akka.actor._
import akka.cluster.Cluster
import akka.cluster.Member
import akka.cluster.MemberStatus
import com.typesafe.config.ConfigFactory
import sample.clustered.ClusteredMessages.{VideoFileActor, OpenFile, BackendRegistration, StartVideo}
import org.reactivestreams.api.Producer
import java.io.File
import akka.actor.RootActorPath
import akka.cluster.ClusterEvent.CurrentClusterState
import akka.cluster.ClusterEvent.MemberUp
import video.Frame
import video.file.FFMpegProducer
import stream.actor.ActorConsumer

//#backend
class ServerVideoRedux extends Actor {

  val cluster = Cluster(context.system)

  // subscribe to cluster changes, MemberUp
  // re-subscribe when restart
  override def preStart(): Unit = cluster.subscribe(self, classOf[MemberUp])

  override def postStop(): Unit = cluster.unsubscribe(self)

  def pathFor(address: Address, name: String): ActorPath =
    RootActorPath(address) / "user" / name

  def receive = {

    case OpenFile(fileName) => {
      val mp4 = new File(fileName)
      sender ! VideoFileActor(FFMpegProducer.make(context, mp4))
    }

    case state: CurrentClusterState =>
      state.members.filter(_.status == MemberStatus.Up) foreach {
        m => register(m,context.self.path.name)
      }

    case MemberUp(m) => register(m, context.self.path.name)
    case msg => println(s"unhandled msg = $msg")
  }

  def register(member: Member, serverActorName:String): Unit =
    if (member.hasRole("frontend"))
      context.actorSelection(RootActorPath(member.address) / "user" / "frontend") !
        BackendRegistration(serverActorName)
}

//#backend
/**
 *
 * run:
 *      ./activator -Dconfig.resource=clustering.conf 'runMain sample.clusterRedux.ServerVideoRedux 2551'
 */
object ServerVideoRedux {
  def main(args: Array[String]): Unit = {
    // Override the configuration of the port when specified as program argument
    val port = if (args.isEmpty) "0" else args(0)
    val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load())

    val system = ActorSystem("ClusterSystem", config)
    system.actorOf(Props[ServerVideoRedux], name = "backend")
  }
}