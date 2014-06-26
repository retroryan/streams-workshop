package sample.stream

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import java.io.File
import org.reactivestreams.api.Producer
import video.{FFMpegAction, Frame}

object ShowVideo {

  /**
   * use:
   * sbt 'runMain sample.stream.ShowVideo file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    FFMpegAction.basicAction(args) {
      (flow, materializer) =>
        flow.toProducer(materializer)
          .produceTo(video.Display.create)
    }
  }
}