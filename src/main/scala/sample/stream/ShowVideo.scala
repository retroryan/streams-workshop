package sample.stream

import akka.stream.scaladsl.Flow
import java.io.File
import org.reactivestreams.api.Producer
import video.{FlowAction, Frame}

object ShowVideo {

  /**
   * use:
   * sbt 'runMain sample.stream.ShowVideo file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    FlowAction.action {
      (materializer, system) =>
        val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)
        val flow = Flow(fileProducer)

        flow.toProducer(materializer)
          .produceTo(video.Display.create)

        flow
    }
  }
}