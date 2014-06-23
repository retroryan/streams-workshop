package video

import java.awt.image.BufferedImage
import java.io.File
import org.reactivestreams.spi.{
  Publisher, Subscriber, Subscription
}
import org.reactivestreams.api.{
  Producer, Consumer
}
import com.xuggle.mediatool.ToolFactory
import com.xuggle.mediatool.MediaListenerAdapter
import com.xuggle.mediatool.event.ICloseEvent
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.xuggler.IError
import com.xuggle.xuggler.Utils

case class FFMpegError(raw: IError) extends Exception(raw.getDescription)

/** Helper for dealing with FFMpeg data. */
object FFMpeg {  
  /** Reads a given file and pushes its stream events out. 
   *  Note: This will not prefetch any data, but only read when requested.  
   */
  def readFile(f: File): Producer[Frame] = new FFMpegFileProducer(f)
}

/** An implementation of a producer that will use Xuggler to read FFMpeg files. */
private[video] class FFMpegFileProducer(f: File) extends AbstractProducer[Frame] {
  override object getPublisher extends Publisher[Frame] {
    def subscribe(subscriber: Subscriber[Frame]): Unit =
      subscriber onSubscribe (new FFMpegFileReader(f, subscriber))
  }
}

/** A subscription which only reads more packets from the file when more elements are requested. */
private[video] class FFMpegFileReader(f: File, subscriber: Subscriber[Frame]) extends Subscription {
  // TODO - Ensure filename is legitimate on windows too.
  private val reader = ToolFactory.makeReader(f.getAbsolutePath)
  private var closed: Boolean = false
  private var frameCount: Long = 0L
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
  override def requestMore(elements: Int): Unit = {
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
  override def cancel(): Unit = {
    closed = true
    reader.close()
  }
}


