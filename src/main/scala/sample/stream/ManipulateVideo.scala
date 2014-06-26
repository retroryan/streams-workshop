package sample.stream

import video.{FFMpegAction, Frame}
import imageUtils.ConvertImage

object ManipulateVideo {

  /**
   * use:
   * sbt 'runMain sample.stream.ShowVideo file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    FFMpegAction.basicAction(args) {
      (flow, materializer) =>
        flow.map(frame => Frame(ConvertImage.addWaterMark(frame.image)))
          .map(frame => Frame(ConvertImage.invertImage(frame.image)))
          .toProducer(materializer)
          .produceTo(video.Display.create)
    }
  }
}