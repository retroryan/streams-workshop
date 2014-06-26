package sample.stream

import video.FFMpegAction

object FrameCount {

  /**
   * Use:
   * sbt 'runMain sample.stream.FrameCount file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {

    var count = 0L

    FFMpegAction.action(args) {
      (flow, materializer) =>
        flow.foreach { frame =>
          count += 1
          System.out.print(f"\rFRAME ${count}%05d")
        }
    }

  }


}