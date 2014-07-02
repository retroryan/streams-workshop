package video

import com.github.sarxos.webcam.{Webcam=>WC}
import java.util.concurrent.TimeUnit

object WebCam {

  private val cam = WC.getDefault

  def stream() = {

    def retreive(f: Boolean): Option[Frame] = {
      if(!cam.isOpen) cam.open()

     Some(Frame(cam.getImage, System.currentTimeMillis, TimeUnit.MILLISECONDS))
    }

  }
}
