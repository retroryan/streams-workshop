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
        val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)
        var count = 0L
        Flow(fileProducer).fold(0) { (count, frame) =>
          val nextCount = count + 1
          System.out.print(f"\rFRAME ${nextCount}%05d")
          nextCount
        }
    }
  }


}