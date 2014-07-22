package sample.clustered

import video.Frame
import org.reactivestreams.api.Consumer
import akka.actor.ActorPath

object ClusteredMessages {
  case object BackendRegistration
  case class StartVideo(consumerActorName:String)
}
