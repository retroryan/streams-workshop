package video

import java.awt.image.BufferedImage
import java.io.File
import akka.actor.ActorSystem
import org.reactivestreams.api.Producer


object ScreenCapture {

  /** Creates a screen capture and pushes its stream events out.
    */
  def readScreenCapture(maxFrameCount: Long, system: ActorSystem): Producer[Frame] = new ScreenCaptureProducer(maxFrameCount, system)
}