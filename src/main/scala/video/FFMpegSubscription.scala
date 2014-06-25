package video

import java.io.File
import org.reactivestreams.spi.Subscriber
import com.xuggle.mediatool.{MediaListenerAdapter, ToolFactory}
import com.xuggle.mediatool.event.{IVideoPictureEvent, ICloseEvent}
import com.xuggle.xuggler.{IError, Utils}
import akka.actor.{Actor, Props}
import sample.utils.BasicActorSubscription.{Cancel, RequestMore}

case class FFMpegError(raw: IError) extends Exception(raw.getDescription)

object FFMpegSubscriptionWorker {
  def props(file: File, subscriber: Subscriber[Frame]): Props =
    Props(new FFMpegFileReader(file, subscriber))

}

/** A subscription which only reads more packets from the file when more elements are requested. */
private[video] class FFMpegFileReader(file: File, subscriber: Subscriber[Frame]) extends Actor {
  // TODO - Ensure filename is legitimate on windows too.
  private val reader = ToolFactory.makeReader(file.getAbsolutePath)
  private var closed: Boolean = false
  private var frameCount: Long = 0L

  override def receive: Receive = {
    case RequestMore(elements) => requestMore(elements)
    case Cancel => cancel()
  }

  /** Register a listener that will forward all events down the Reactive Streams chain. */
  reader.addListener(new MediaListenerAdapter() {
    override def onClose(e: ICloseEvent): Unit = {
      // TODO - No way to tell if we had errors here...
      if(!closed) {
        closed = true
        subscriber.onComplete()
      }
    }
    override def onVideoPicture(e: IVideoPictureEvent): Unit = {
      if(e.getMediaData.isComplete) {
        subscriber.onNext(Frame(Utils.videoPictureToImage(e.getMediaData)))
        frameCount += 1
      }
    }
  })

  /** Actually drives reading the file. */
  def requestMore(elements: Int): Unit = {
    val done = frameCount + elements
    // Close event should automatically occur.
    while(!closed && frameCount < done) {
      try (reader.readPacket match {
        case null => // Ignore
        case error =>
          closed = true
          // TODO - Make sure close event hasn't gone out yet.
          subscriber.onError(FFMpegError(error))
      }) catch {
        // Failure reading
        case e: Exception =>
          closed = true
          subscriber.onError(e)
      }
    }
  }

  def cancel(): Unit = {
    closed = true
    reader.close()
    context.parent ! FFMpegProducerWorker.Finished
  }
}


