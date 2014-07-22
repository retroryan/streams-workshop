package video

import org.reactivestreams.api.{
Producer,
Consumer
}
import javax.swing.{JComponent, JPanel, JFrame}
import akka.actor.{ActorRef, ActorSystem}
import java.awt.{BorderLayout, Component, GridLayout, Dimension}
import java.awt.event.{WindowAdapter, WindowEvent}


sealed trait UIControl

case object Play extends UIControl

case object Pause extends UIControl

case object Stop extends UIControl


/** Widget which wraps another component, and exposes play/pause/stop buttons. */
class VideoPlayerDisplay(display: JComponent, controls: JComponent, width: Int, height: Int) extends JPanel {
  setLayout(new BorderLayout)
  add(display, BorderLayout.CENTER)
  //add(new JLabel("Video Player"), BorderLayout.CENTER)
  add(controls, BorderLayout.SOUTH)
  setMinimumSize(new Dimension(width, height))
}

object Display {

  def create(system: ActorSystem): Consumer[Frame] = {
    val (consumer, display) = swing.VideoPanel(system)
    createFrame(system, display)
    consumer
  }

  def createActorRef(system: ActorSystem): ActorRef = {
    val (consumerActorRef, display ) = swing.VideoPanel.make(system)
    createFrame(system, display)
    consumerActorRef
  }

  def createFrame(system: ActorSystem, display: JComponent) {
    val width = DisplayProperties.getWidth(system)
    val height = DisplayProperties.getHeight(system)
    val frame = inFrame("Video Preview", display, system, width, height)
  }

  def createPlayer(system: ActorSystem): (Producer[UIControl], Consumer[Frame]) = {
    val (consumer, display) = swing.VideoPanel(system)
    val (producer, controls) = swing.PlayerControls(system)

    val width = DisplayProperties.getWidth(system)
    val height = DisplayProperties.getHeight(system)
    val player = new VideoPlayerDisplay(display, controls, width, height)
    inFrame("Video Player", player, system, width, height)
    producer -> consumer
  }


  private def inFrame[T](title: String, c: Component, system: ActorSystem, width: Int, height: Int): JFrame = {
    val jframe = new JFrame(title)
    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE)
    val pane = jframe.getContentPane
    pane.setLayout(new GridLayout(1, 1))
    pane.add(c)


    jframe.setSize(width, height)
    jframe.setVisible(true)
    jframe.addWindowListener(new WindowAdapter() {
      override def windowClosed(e: WindowEvent): Unit = {
        system.shutdown()
      }
    })
    jframe
  }
}