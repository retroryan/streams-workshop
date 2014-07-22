package sample.stream

import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import video.Frame
import org.reactivestreams.api.Producer
import java.io.File
import akka.stream.scaladsl.Flow

object FrameCount {

  /**
   * run:
   *    ./activator 'runMain sample.stream.FrameCount file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    val settings = MaterializerSettings()

    val videoStream: Producer[Frame] = video.FFMpeg.readFile(new File("goose.mp4"), system)
    Flow(videoStream).fold(0) { (count, frame) =>
      val nextCount = count + 1
      System.out.print(f"\rFRAME ${nextCount}%05d")
      nextCount
    }.onComplete(FlowMaterializer(settings)) {
      case _ => system.shutdown()
    }
  }
}