package sample.stream

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import java.io.File
import org.reactivestreams.api.Producer
import video.Frame

object ShowVideo {

  /**
   * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)
    var count = 0L

    val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)))

    val flow =
      Flow(fileProducer)
        .toProducer(materializer)
        .produceTo(video.Display.create)
  }
}