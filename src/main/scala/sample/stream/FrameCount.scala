package sample.stream

import video.{Frame, FlowAction}
import org.reactivestreams.api.Producer
import java.io.File
import akka.stream.scaladsl.Flow

object FrameCount {

  /**
   * Use:
   * sbt 'runMain sample.stream.FrameCount file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    FlowAction.action {
      (materializer, system) =>
        val videoStream: Producer[Frame] = video.FFMpeg.readFile(new File("goose.mp4"), system)
        Flow(videoStream).fold(0) { (count, frame) =>
          val nextCount = count + 1
          System.out.print(f"\rFRAME ${nextCount}%05d")
          nextCount
        }
    }
  }


}