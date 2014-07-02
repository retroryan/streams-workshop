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

import akka.stream.actor.{
  ActorConsumer, 
  ActorProducer
}

sealed trait UIControl
case object Play extends UIControl
case object Pause extends UIControl
case object Stop extends UIControl


/** Widget which wraps another component, and exposes play/pause/stop buttons. */
class VideoPlayerDisplay(display: JComponent, controls: JComponent) extends JPanel {
  setLayout(new BorderLayout)
  add(display, BorderLayout.CENTER)
  //add(new JLabel("Video Player"), BorderLayout.CENTER)
  add(controls, BorderLayout.SOUTH)
  setMinimumSize(new Dimension(640, 580))
}

object Display {
  def create(system: ActorSystem): Consumer[Frame] = {
    val (consumer, display) = swing.VideoPanel(system)
    val frame = inFrame("Video Preview", display, system)
    consumer
  }
  def createPlayer(system: ActorSystem): (Consumer[Frame], Producer[UIControl]) = {
    val (consumer, display) = swing.VideoPanel(system)
    val (producer, controls) = swing.PlayerControls(system)
    val player = new VideoPlayerDisplay(display, controls)
    inFrame("Video Player", player, system)
    consumer -> producer
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