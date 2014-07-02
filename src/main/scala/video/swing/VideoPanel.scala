package video
package swing

import javax.swing.JComponent
import akka.stream.actor.ActorConsumer
import java.awt.Color
import java.awt.Graphics
import org.reactivestreams.api.Consumer
import akka.actor.ActorRefFactory
import akka.actor.Props

/**
 * A video panel which can consume Frame elements and display them in the UI
 *  at whatever rate they are fired.
 */
private[swing] class VideoPanel extends JComponent {
  private var lastFrame: Option[Frame] = None
  private val color = Color.BLACK
  

  override protected def paintComponent(g: Graphics): Unit =
    lastFrame match {
      case None =>
        g.setColor(color)
        g.drawRect(0, 0, getWidth, getHeight)
      case Some(frame) => drawFrame(g, frame)
    }
  
  def updateFrame(f: Frame): Unit = {
    lastFrame = Some(f)
    drawFrame(getGraphics, f)
  }

  private def drawFrame(g: Graphics, frame: Frame): Unit = 
    if (g != null) {
      g.drawImage(frame.image, 0, 0, getWidth, getHeight, 0, 0, frame.image.getWidth, frame.image.getHeight, color, this)
    }
}
private[swing] class VideoPanelActor(panel: VideoPanel) extends ActorConsumer {
  override val requestStrategy = ActorConsumer.OneByOneRequestStrategy
  def receive: Receive = {
    case ActorConsumer.OnNext(frame: Frame) => panel.updateFrame(frame)
    case ActorConsumer.OnComplete =>
      // TODO - blank out the screen
    case ActorConsumer.OnError(err) =>
      // TODO - display error.
  }
}
object VideoPanel  {
  private def props(panel: VideoPanel): Props = Props(new VideoPanelActor(panel))
  /** Construct a video panel which consumes frames and renders them on the swing component. */
  def apply(factory: ActorRefFactory): (Consumer[Frame], JComponent) = {
    // TODO - this is horribly wrong for error handling, but the alternative is more annoying and
    // much harder to implement (rewiring actors to physical swing controls when restarted).
    val panel = new VideoPanel()
    val actorRef = factory.actorOf(props(panel).withDispatcher("swing-dispatcher"), "video-panel")
    ActorConsumer(actorRef) -> panel
  }
}