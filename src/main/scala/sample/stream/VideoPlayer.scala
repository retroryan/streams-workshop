package sample.stream

import java.awt._
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage

import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import java.io.File
import org.reactivestreams.api.Producer
import video.Frame
import org.reactivestreams.api.Processor
import video.UIControl
import akka.actor.Actor
import org.reactivestreams.spi.Subscriber
import akka.actor.Props
import org.reactivestreams.spi.Subscription
import org.reactivestreams.spi.Publisher
import imageUtils.{ImageOverlay, ConvertImage}
import akka.stream.actor.ActorProducer
import akka.stream.actor.ActorConsumer
import video.Play
import video.Pause
import video.Stop
import akka.actor.ActorRef

object VideoPlayer {



  /**
   * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)
    val overlay = new ImageOverlay(new File("crosshairs-overlay.jpg"))


    val (player, uiControls) = video.Display.createPlayer(system)
    
    // EXERCISE - Show this to user in terms of flow, have diagrams
    val file = new File("goose.mp4")
    val playEngineActor = system.actorOf(Props(new PlayerProcessorActor(file)))
    val playEngineConsumer = ActorConsumer[UIControl](playEngineActor)
    val playEngineProducer = ActorProducer[Frame](playEngineActor)
    uiControls produceTo playEngineConsumer
    var frameCount = 0L
    Flow(playEngineProducer).map { frame =>
      // TODO - We use a mutable write here because we're optimising memory usage in
      // this flow.
      overlay.overlayOnto((frame.image)); frame
      //frame.copy(image = overlay.overlay(frame.image))
      //frame.copy(image = ConvertImage.grayscale(frame.image))
    }.map { frame =>
      frameCount += 1

      Frame(addWaterMark(frame.image, frameCount), frame.timeStamp, frame.timeUnit)
    }.produceTo(materializer, player)
    //playEngineProducer produceTo player

    ()
  }

  def addWaterMark(inputImage: BufferedImage, frameCount: Long): BufferedImage = {
    val g2d: Graphics2D = inputImage.createGraphics
    val alpha: AlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)

    val slowDown = 10.0
    val xMax = ((math.cos(frameCount/slowDown)+1)/2.0 * inputImage.getWidth).toInt
    val yMax = ((math.sin(frameCount/slowDown)+1)/2.0 * inputImage.getHeight).toInt

    g2d.setComposite(alpha)
    g2d.setColor(Color.white)
    g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g2d.setFont(new Font("Arial", Font.BOLD, 30))
    val watermark: String = "Copyright Typesafe © 2014"
    val fontMetrics: FontMetrics = g2d.getFontMetrics
    val rect: Rectangle2D = fontMetrics.getStringBounds(watermark, g2d)
    g2d.drawString(watermark, inputImage.getWidth - (rect.getWidth.asInstanceOf[Int] + xMax), inputImage.getHeight - (rect.getHeight.asInstanceOf[Int] + yMax))
    g2d.dispose
    return inputImage
  }
}

case class PlayerRequestMore(elements: Int)
case object PlayerDone

/** An actor which is responsible for taking Play/Pause/Stop requests and adjusting
 *  the output frame stream accordingly.
 *  
 *  This player 
 */
class PlayerProcessorActor(file: File) extends ActorProducer[Frame] with ActorConsumer /*[UIControl]*/ {
  // Ensure that we only ask for UI events one at a time.
  override val requestStrategy = ActorConsumer.OneByOneRequestStrategy
  // Private data, holding which actor is currently acting as a Consumer[Frame] for us.
  private var currentPlayer: Option[ActorRef] = None
  // Whether or not the stream is in a pause state.
  // TODO - not provided initially for exercise?
  private var isPaused: Boolean = false
  
  /** The main message loop of the actor.  All our behavior/state changes reside in here. */
  override def receive: Receive = {
    case ActorConsumer.OnNext(control: UIControl) =>
        // EXERCISE - Implement handling of UI control messages.
        //     On any control message, we need to update our state appropriately.
        //     Play - we need to ensure that we have a bridge to a Producer[Frame] which
        //            which will play back our file.   (See `kickOffFileProducer`).
        //            Additionally, we need to make sure we *ask* for data from the player
        //            once we ourselves have been asked for data.
        //     Pause - we need to modify our state so that we do not ask for any more
        //             Frame messsages from the underlying `currentPlayer`.
        //     Stop  -  We need to cancel the currentPlayer and update our state. 
    	control match {
    	  case Play =>
	        // Update state and kick off the stream
    	    if(currentPlayer.isEmpty) kickOffFileProducer()
    	    isPaused = false
    	    // TODO - If we have pause cache, we should fire those events.
    	    requestMorePlayer()
    	  case Pause =>
    	    isPaused = true
    	  case Stop =>
    	    isPaused = false
    	    currentPlayer.foreach(_ ! Stop)
    	    currentPlayer = None
    	}
    case ActorProducer.Request(elements) => 
      // EXERCISE - Implement handling of request for more elements.
      //    When the downstream asks for more elements, we should delegate these
      //    down to our underlying `currentPlayer`.  However, if we are in pause mode,
      //    then we should not ask for more Frames.
      //  Hint:  The key to backpressure in reactive streams is that we cannot send any frames
      //           until after this message is received.
      //  Hint2:  If you're running out of memory, remember to flush any buffers you might have.
      if(!isPaused) {
        if(tryEmptyBuffer()) requestMorePlayer()
      }
    case PlayerDone =>
      // EXERCISE - Implement what to do when the file is complete.
      //    When the file completes you have the following choices:
      //    1. send the completion message downstream.
      //    2. kill the curentPlayer and create a new one, looping back to the begging.
      //    3. Issue an error, just to see what happens.
      kickOffFileProducer()
      requestMorePlayer()
    case f: Frame => 
      // EXERCISE - Implement what we do when the underlying stream pushes us some data.
      //            
      //            Hint:  Look at the totalDemand member of ActorProducer trait.
      //            Hint2:  If you see runtime errors, make sure you're only pushing data after
      //                    the consumer has asked for it.  Also, check out the private `buffer` methods.
      if(totalDemand > 0) onNext(f)
      else buffer(f)
  }
  
  
  
  
  
  // Buffering (unbounded) if we get overloaded on input frames.
  private val buf = collection.mutable.ArrayBuffer.empty[Frame]
  /** Buffers the given frame to be pushed to future clients later. */
  private def buffer(f: Frame): Unit = buf.append(f)
  /** Attempts to empty the buffer of frames we couldn't yet send, if there is
   *  demand from consumers.   Returns true if the entire buffer was emptied, false otherwise.
   */
  private def tryEmptyBuffer(): Boolean = {
    while(!buf.isEmpty && totalDemand > 0)
      onNext(buf.remove(0))
    buf.isEmpty
  }
  /** Creates a new `Producer[Frame]` for the underlying file, and feeds its `Frame` output
   *  to ourselves as raw `Frame` messages
   */
  private def kickOffFileProducer(): Unit = {
    val producer = video.FFMpeg.readFile(file, context)
    val consumerRef = context.actorOf(Props(new PlayerActor(self)))
    currentPlayer = Some(consumerRef)
    producer produceTo ActorConsumer(consumerRef)
  }
  /** Request as much data from the underlying Producer[Frame] as our client consumers
   *  have requests from us.
   */
  private def requestMorePlayer(): Unit = {
    if(totalDemand > 0) currentPlayer match {
      case Some(player) => player ! PlayerRequestMore(totalDemand)
      case None => ()
    }
  }
}
/** A helper actor which forwards requests from the file-reading consumer back to
 *  the main controlling actor.
 *  
 *  This just forwards `Frame` and `PlayerDone` messages to the main actor.  In addition,
 *  any `PlayerRequestMore` messages are delegated down as actual akka stream `requestMore` calls.
 */
class PlayerActor(consumer: ActorRef) extends ActorConsumer {
  // All requests for more data handled by our 'consumer' actor.
  override val requestStrategy = ActorConsumer.ZeroRequestStrategy  
  override def receive: Receive = {
    // Just delegate back/forth with the controlling 'consumer' actor.
    case ActorConsumer.OnNext(frame: Frame) =>
      consumer ! frame
    case PlayerRequestMore(e) => request(e)
    case Stop => cancel()
    case ActorConsumer.OnComplete =>
      consumer ! PlayerDone
  }
}