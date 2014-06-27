package video

import java.io.File
import org.reactivestreams.spi.{Publisher, Subscriber, Subscription}
import org.reactivestreams.api.Consumer
import akka.actor.{ActorSystem, Actor, Props, ActorRef}
import sample.utils.BasicActorSubscription
import com.xuggle.mediatool.ToolFactory
import com.xuggle.mediatool.IMediaWriter
import java.util.concurrent.TimeUnit
import com.xuggle.xuggler.IRational

/** An implementation of a producer that will use Xuggler to read FFMpeg files. */
private[video] class FFMpegFileConsumer(file: File, system: ActorSystem, width: Int, height: Int, frameRate: IRational = IRational.make(3, 1)) extends Consumer[Frame] {
  import FFMpegConsumerWorker._
  val worker = system.actorOf(props(file, width, height, frameRate))
  override object getSubscriber extends Subscriber[Frame] {
    def onSubscribe(subscription: Subscription): Unit =
      worker ! Sub(subscription)
    def onNext(element: Frame): Unit =
      worker ! Next(element)
    def onComplete(): Unit =
      worker ! Complete
    def onError(cause: Throwable): Unit =
      worker ! Error(cause)
  }

}

object FFMpegConsumerWorker {
  def props(f: File, width: Int, height: Int, frameRate: IRational = IRational.make(3, 1)): Props = Props(new FFMpegConsumerWorker(f, frameRate, width, height))
  case object Complete
  case class Error(cause: Throwable)
  case class Next(e: Frame)
  case class Sub(s: Subscription)
}
class FFMpegConsumerWorker(file: File, frameRate: IRational, width: Int, height: Int) extends Actor {
  import FFMpegConsumerWorker._
  // TODO - this is pretty evil
  var subscription: Subscription = null
  val writer: IMediaWriter = ToolFactory.makeWriter(file.getAbsolutePath)
  writer.addVideoStream(0, 0, frameRate, width, height);
  def receive: Receive = {
    case Sub(s) =>
      subscription = s
      subscription.requestMore(1)
    case Error(e) =>
      if(subscription != null) {
	      System.err.println("Error in stream")
	      // Kill the file on error.
	      writer.close()
	      file.delete()
	      subscription = null
      }
    case Next(frame) =>
      // TODO - Ensure image is  BufferedImage.TYPE_3BYTE_BGR
      writer.encodeVideo(0, frame.image, frame.timeStamp, frame.timeUnit);
      subscription.requestMore(1)
    case Complete =>
      writer.close()
      subscription = null   
      
      // NOTE - SUPER DUPER HACK
      context.system.shutdown()
  }
}