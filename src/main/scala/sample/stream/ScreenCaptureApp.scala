package sample.stream

import javax.swing.JFrame
import java.awt.Graphics2D

object ScreenApp{

  def main(args: Array[String]): Unit = {
    val frame = new javax.swing.JFrame() {
      setTitle("STREAMING VIDEO")
      setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
      setSize(800, 600)
      setVisible(true)

      dummyStream(getGraphics.asInstanceOf[Graphics2D])
    }
  }

  def dummyStream(g: Graphics2D) = {
   /* val screen = capture.Screen.stream()(newSyncContext)
    val webcam = capture.WebCam.stream(newSyncContext)
    val overlayImage = video.ImageUtils.staticImageStream(new java.io.File("overlay.png"))(newSyncContext)

    val layout = video.Layout(
      size = new Rectangle(800,600),
      items = Seq(
        video.LayoutItem(screen, new Rectangle(100,200, 200, 300)),
        video.LayoutItem(overlayImage, new Rectangle(0,0, 800,600))
      )
    )

    val overlayStream = video.Overlays.makeOverlay(layout)

    val run = webcam &> video.ImageUtils.resize(400, 400) apply Iteratee.foreach { snap =>
      g.drawImage(snap.screen, null, 0, 0)

      */
    }
  }