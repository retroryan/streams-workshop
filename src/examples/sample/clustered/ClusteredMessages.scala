package sample.clustered

import java.io.File
import akka.actor.ActorRef

object ClusteredMessages {

  case class BackendRegistration(serverActorName:String)

  case class OpenFile(fileName: String)

  case class VideoFileActor(ref: ActorRef)

  case class StartVideo(consumerActorName: String)

}
