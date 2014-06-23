package video


import java.awt.image.BufferedImage
import org.reactivestreams.spi.{
  Publisher, Subscriber, Subscription
}
import org.reactivestreams.api.{
  Producer, Consumer
}
import javax.swing.JComponent
import java.util.concurrent.atomic.AtomicReference
import java.awt.Color
import java.awt.Graphics
import javax.swing.JFrame
import java.awt.GridLayout
import javax.swing.SwingUtilities


/** A Swing component which will render frames to the screen as they are consumed. */
class FrameDisplay extends JComponent with Consumer[Frame] {
  private var lastFrame: AtomicReference[Option[Frame]] = new AtomicReference(None)
  private var color = Color.BLACK
  private var frame = 0L
  object getSubscriber extends Subscriber[Frame] {
    private var subscription: Subscription = null
    // TODO - Maybe we do some of this on the UI thread?
    def onSubscribe(s: Subscription): Unit = {
      subscription = s
      // Here we start asking for more frames.
      subscription.requestMore(1)
      
    }
    def onNext(frame: Frame): Unit = {
      // TODO - This is to help relieve memory pressure, due to naive architecture.
      lastFrame.get.foreach(image => image.image.flush())
      lastFrame.lazySet(Some(frame))
      // TODO - Right now this is too integrated, so we're using too much stack and saving way too much memory.
      SwingUtilities.invokeLater(new Runnable() {
        def run(): Unit = {
          drawFrame(FrameDisplay.this.getGraphics, frame)
          subscription.requestMore(1)
        }
      })
    }
    def onComplete(): Unit = {
      lastFrame.set(None)
      FrameDisplay.this.repaint()
    }
    // Errors and complete streams are treated the same, for now.
    def onError(cause: Throwable ): Unit = onComplete()
  }
  
  private def drawFrame(g: Graphics, frame: Frame): Unit =
    if(g != null) {
      g.drawImage(frame.image, 0, 0, getWidth, getHeight, 0, 0, frame.image.getWidth, frame.image.getHeight, color, this)
    }
    
  override protected def paintComponent(g: Graphics): Unit = {
    lastFrame.get match {
      case None =>
        g.setColor(color)
        g.drawRect(0, 0, getWidth, getHeight)
      case Some(frame) => drawFrame(g, frame)
    }
  }
}

object Display {
  def create(): Consumer[Frame] = {
    val jframe = new JFrame("Display Window")
    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val display = new FrameDisplay()
    jframe.add(display)
    jframe.setLayout(new GridLayout(1,1))
    jframe.setSize(640, 480)
    jframe.setVisible(true)
    display
  }
}