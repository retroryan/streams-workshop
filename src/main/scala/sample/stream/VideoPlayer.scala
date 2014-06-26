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

object VideoPlayer {

  /**
   * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)
    var count = 0L
    val (player, uiControls) = video.Display.createPlayer
    val playerProcessor = new PlayerProcessor(system, new File(args(0)))
    uiControls produceTo playerProcessor
    playerProcessor produceTo player
    ()
  }
}

// An implementation of a processor.  takes a UI Control input stream
// and 'controls' an underlying file input stream, ensuring that playing happens only when in
// the play state.
class PlayerProcessor(system: ActorSystem, file: File) extends video.AbstractProducer[Frame] with Processor[UIControl, Frame] {
  private val playerActor = system.actorOf(Props(new PlayerActor))
  private class PlayerActor extends Actor {
    private var subscribers: Set[Sub] = Set.empty
    private var videoStream: Option[Subscription] = None
    val generic: Receive = {
      case Register(sub) =>
        subscribers += sub
        sub.s.onSubscribe(sub)
      case Cancel(sub) =>
        subscribers -= sub
    }
    private def makeHandler(r: Receive): Receive = 
      r orElse generic
    val stopped: Receive = makeHandler {
      case video.Play =>
        context become playing
        startFile()
    }
    val paused: Receive = makeHandler {
      case video.Play =>
        // Request more frames, and continue
        context become playing
        requestMore(1)
      case video.Stop => stop()
      case f: video.Frame =>
      // We should fire the frame on, but we don't need to request more.
      // POSSIBLY we should just buffer this for later...
        fireFrame(f)
    }
    val playing: Receive = makeHandler {
      case video.Stop => stop()
      case VideoSubscription(s) =>
        videoStream = Some(s)
        s.requestMore(1)
      case video.Pause =>
        context become paused
      case f: video.Frame =>
        fireFrame(f)
      // TODO - Don't assume one subscriber...
      case RequestMore(s, e) =>
        requestMore(e)
    }
    override def receive = stopped
    
    private def requestMore(e: Int): Unit = 
      videoStream.foreach(_.requestMore(e))
    private def fireFrame(f: Frame): Unit =
      for(s <- subscribers) {
        s.s.onNext(f)
      }
    
    private def stop(): Unit = {
      context become stopped
      videoStream foreach (_.cancel)
      videoStream = None
    }
    
    private def startFile(): Unit = {
      object videoStreamSubscriber extends Subscriber[Frame] {
        override def onSubscribe(subscription: Subscription): Unit = 
          self ! VideoSubscription(subscription)
        def onNext(element: Frame): Unit = self ! element
        def onComplete(): Unit = self ! VideoComplete
        def onError(cause: Throwable): Unit = self ! VideoError(cause)
      }
      video.FFMpeg.readFile(file, system).getPublisher.subscribe(videoStreamSubscriber)
    }
  }

  
  
  // Gross details of creating a new SPI provider...
  object getSubscriber extends Subscriber[UIControl] {
    def onSubscribe(subscription: Subscription): Unit = () // TODO - what to do here
    def onNext(element: UIControl): Unit = playerActor ! element
    def onComplete(): Unit = () // TODO - Fire complete downstream
    def onError(cause: Throwable): Unit = () /// TODO - fire downstream
  }
  object getPublisher extends Publisher[Frame] {
    def subscribe(s: Subscriber[Frame]): Unit = {
      playerActor ! Register(new Sub(s))
    }
  }
  private case class Sub(val s: Subscriber[Frame]) extends Subscription {
    override def requestMore(elements: Int): Unit = 
      playerActor ! RequestMore(this, elements)
    override def cancel(): Unit = playerActor ! Cancel(this)
  }
  private case class VideoSubscription(s: Subscription)
  private case object VideoComplete
  private case class VideoError(e: Throwable)
  private case class Register(s: Sub)
  private case class RequestMore(s: Sub, e: Int)
  private case class Cancel(s: Sub)

  private def openFile: Producer[Frame] = video.FFMpeg.readFile(file, system)
}