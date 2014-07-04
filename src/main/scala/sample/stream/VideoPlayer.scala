package sample.stream

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
import imageUtils.ConvertImage
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
    val (player, uiControls) = video.Display.createPlayer(system)
    
    // EXERCISE - Show this to user in terms of flow, have diagrams
    val file = new File(args(0))
    val playEngineActor = system.actorOf(Props(new PlayerProcessorActor(file)))
    val playEngineConsumer = ActorConsumer[UIControl](playEngineActor)
    val playEngineProducer = ActorProducer[Frame](playEngineActor)
    uiControls produceTo playEngineConsumer
    Flow(playEngineProducer).map { frame =>
      Frame(ConvertImage.addWaterMark(frame.image), frame.timeStamp, frame.timeUnit)
    }.produceTo(materializer, player)
    //playEngineProducer produceTo player

    ()
  }
}

case class PlayerRequestMore(elements: Int)
case object PlayerDone

/** An actor which is responsible for taking Play/Pause/Stop requests and adjusting
 *  the output frame stream accordingly.
 */
class PlayerProcessorActor(file: File) extends ActorProducer[Frame] with ActorConsumer {
  override val requestStrategy = ActorConsumer.OneByOneRequestStrategy
  private var currentPlayer: Option[ActorRef] = None
  private var isPaused: Boolean = false
  override def receive: Receive = {
    case ActorConsumer.OnNext(control: UIControl) =>
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
      // TODO - flush buffer.
      if(!isPaused) {
        if(tryEmptyBuffer) requestMorePlayer()
      }
    case PlayerDone =>
      kickOffFileProducer()
      requestMorePlayer()
    case f: Frame => 
      // TODO - Cache if we are paused.
      if(totalDemand > 0) onNext(f)
      else buffer(f)
  }
  
  // Buffering (unbounded) if we get overloaded.
  private val buffer = collection.mutable.ArrayBuffer.empty[Frame]
  private def buffer(f: Frame): Unit = buffer.append(f)
  private def tryEmptyBuffer(): Boolean = {
    while(!buffer.isEmpty && totalDemand > 0)
      onNext(buffer.remove(0))
    buffer.isEmpty
  }
  private def kickOffFileProducer(): Unit = {
    val producer = video.FFMpeg.readFile(file, context)
    val consumerRef = context.actorOf(Props(new PlayerActor(self)))
    currentPlayer = Some(consumerRef)
    producer produceTo ActorConsumer(consumerRef)
  }
  private def requestMorePlayer(): Unit = {
    if(totalDemand > 0) currentPlayer match {
      case Some(player) => player ! PlayerRequestMore(totalDemand)
      case None => ()
    }
  }
}
/** A helper actor which forwards requests from the file-reading consumer back to
 *  the main controlling actor.
 */
class PlayerActor(consumer: ActorRef) extends ActorConsumer {
  override val requestStrategy = ActorConsumer.ZeroRequestStrategy  
  override def receive: Receive = {
    case ActorConsumer.OnNext(frame: Frame) =>
      consumer ! frame
    case PlayerRequestMore(e) => request(e)
    case Stop => cancel()
    case ActorConsumer.OnComplete =>
      consumer ! PlayerDone
  }
}