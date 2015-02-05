package sample.stream

import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import video.{Ansi, Ascii, Frame}
import org.reactivestreams.api.Producer
import java.io.File
import akka.stream.scaladsl.Flow

object AsciiVideo {

  /**
   * Use:
   * sbt 'runMain sample.stream.FrameCount file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    System.out.print(Ansi.CLEAR_SCREEN)
    System.out.print(Ansi.MOVE_CURSOR_TO_UPPER_LEFT)
    System.out.flush()
    implicit val system = ActorSystem()
    val settings = MaterializerSettings()
    val videoStream: Producer[Frame] = video.FFMpeg.readFile(new File("goose.mp4"), system)
    val size = 80 // TODO - figure out how to resize the image better.
    val (asciiIn, asciiOut) = Ascii.asciifier(size, size).build(FlowMaterializer(settings))
    videoStream.produceTo(asciiIn)
    // TODO - manually slow the rate of rendering down to realtime...
    Flow(asciiOut).map { frame =>
      // TODO - we may want the ascii rendering to happen directly against terminal.
      System.out.print(Ansi.MOVE_CURSOR_TO_UPPER_LEFT)
      System.out.print(frame.image)
      System.out.flush()
    }.onComplete(FlowMaterializer(settings)) {
      case _ =>
        system.shutdown()
    }
  }
}