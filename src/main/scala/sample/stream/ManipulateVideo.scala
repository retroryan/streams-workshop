package sample.stream

import video.{FlowAction, Frame}
import imageUtils.ConvertImage
import org.reactivestreams.api.{Consumer, Producer}
import java.io.File
import akka.stream.scaladsl.Flow

object ManipulateVideo {

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

        val videoConsumer: Consumer[Frame] = video.Display.create(system)

        flow.map(frame => Frame(ConvertImage.addWaterMark(frame.image), frame.timeStamp, frame.timeUnit))
          .map(frame => Frame(ConvertImage.invertImage(frame.image), frame.timeStamp, frame.timeUnit))
          .toProducer(materializer)
          .produceTo(videoConsumer)

        flow
    }
  }
}