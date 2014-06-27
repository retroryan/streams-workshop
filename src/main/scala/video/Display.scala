package video

import java.awt.image.BufferedImage
import org.reactivestreams.spi.{
  Publisher,
  Subscriber,
  Subscription
}
import org.reactivestreams.api.{
  Producer,
  Consumer
}
import javax.swing.JComponent
import java.util.concurrent.atomic.AtomicReference
import java.awt.Color
import java.awt.Graphics
import javax.swing.JFrame
import java.awt.GridLayout
import javax.swing.SwingUtilities
import javax.swing.JPanel
import java.awt.BorderLayout
import javax.swing.JButton
import java.awt.event.ActionListener
import java.awt.event.ActionEvent
import java.awt.Dimension
import javax.swing.JLabel
import java.awt.Component
import akka.actor.ActorSystem
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

sealed trait UIControl
case object Play extends UIControl
case object Pause extends UIControl
case object Stop extends UIControl

/** A Swing component which will render frames to the screen as they are consumed. */
class FrameDisplay extends JComponent with Consumer[Frame] {
  setPreferredSize(new Dimension(640, 480))
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
    def onError(cause: Throwable): Unit = onComplete()
  }

  private def drawFrame(g: Graphics, frame: Frame): Unit =
    if (g != null) {
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

/** Widget which wraps another component, and exposes play/pause/stop buttons. */
class PlayerHolder(center: JComponent) extends JPanel with Producer[UIControl] {
  private val buttonPanel = new JPanel()
  private val playPauseButton = new JButton("play")
  private val stopButton = new JButton("stop")
  private var isPlaying = false
  setLayout(new BorderLayout)
  buttonPanel.setPreferredSize(new Dimension(640, 100))
  buttonPanel.setMinimumSize(new Dimension(640, 100))
  buttonPanel.setLayout(new GridLayout(1, 2))
  playPauseButton.setMinimumSize(new Dimension(200, 100))
  stopButton.setMinimumSize(new Dimension(200, 100))
  buttonPanel.add(playPauseButton)
  buttonPanel.add(stopButton)
  add(center, BorderLayout.CENTER)
  //add(new JLabel("Video Player"), BorderLayout.CENTER)
  add(buttonPanel, BorderLayout.SOUTH)
  setMinimumSize(new Dimension(640, 580))

  playPauseButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent): Unit = {
      if (isPlaying) {
        playPauseButton.setLabel("play")
        fireStreamEvent(Pause)
      } else {
        playPauseButton.setLabel("pause")
        fireStreamEvent(Play)
      }
      isPlaying = !isPlaying
    }
  });
  stopButton.addActionListener(new ActionListener() {
    override def actionPerformed(e: ActionEvent): Unit = {
      playPauseButton.setLabel("play")
      isPlaying = false
      fireStreamEvent(Stop)
    }
  })

  object getPublisher extends Publisher[UIControl] {
    def subscribe(subscriber: Subscriber[UIControl]): Unit = {
      val sub = new Sub(subscriber)
      subscriptions += sub
      // TODO - add to list
      subscriber onSubscribe sub
    }
  }
  // TODO - real thread safety around this
  @volatile
  private var subscriptions: Set[Sub] = Set.empty

  def produceTo(c: Consumer[UIControl]): Unit =
    getPublisher subscribe c.getSubscriber

  private case class Sub(c: Subscriber[UIControl]) extends Subscription {
    override def cancel(): Unit = {
      subscriptions -= this
    }
    override def requestMore(elements: Int): Unit = () // Ignore requests for more, we just always
    // fire...

    def fireStreamEvent(e: UIControl): Unit = {
      c onNext e
    }
  }

  // TODO - Implement.
  private def fireStreamEvent(e: UIControl): Unit =
    subscriptions.foreach(_ fireStreamEvent e)

}

object Display {
  def create(system: ActorSystem): Consumer[Frame] = {
    val display = new FrameDisplay()
    val frame = inFrame("Video Preview", display, system)
    display
  }
  def createPlayer(system: ActorSystem): (Consumer[Frame], Producer[UIControl]) = {
    val display = new FrameDisplay()
    val controller = new PlayerHolder(display)
    inFrame("Video Player", controller, system)
    display -> controller
  }

  private def inFrame[T](title: String, c: Component, system: ActorSystem): JFrame = {
    val jframe = new JFrame(title)
    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val pane = jframe.getContentPane
    pane.setLayout(new GridLayout(1, 1))
    pane.add(c)
    jframe.setSize(640, 580)
    jframe.setVisible(true)
    jframe.addWindowListener(new WindowAdapter() {
      override def windowClosed(e: WindowEvent): Unit = {
        system.shutdown()
      }
    });
    jframe
  }
}