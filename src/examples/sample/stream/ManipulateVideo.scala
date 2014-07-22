package sample.stream

import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import video.{ConvertImage, Frame}
import org.reactivestreams.api.{Consumer, Producer}
import java.io.File
import akka.stream.scaladsl.Flow

object ManipulateVideo {

  /**
   * run:
   *   ./activator 'runMain sample.stream.ManipulateVideo goose.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    val settings = MaterializerSettings()
    val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)
    val flow = Flow(fileProducer)
    val videoConsumer: Consumer[Frame] = video.Display.create(system)
    flow.map(frame => Frame(ConvertImage.addWaterMark(frame.image), frame.timeStamp, frame.timeUnit))
          .map(frame => Frame(ConvertImage.invertImage(frame.image), frame.timeStamp, frame.timeUnit))
          .produceTo(FlowMaterializer(settings), videoConsumer)
  }
}