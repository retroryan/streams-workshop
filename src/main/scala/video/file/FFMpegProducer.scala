package video
package file

import akka.stream.actor.ActorProducer
import java.io.File
import com.xuggle.mediatool.ToolFactory
import com.xuggle.mediatool.MediaListenerAdapter
import com.xuggle.mediatool.event.ICloseEvent
import com.xuggle.mediatool.event.IVideoPictureEvent
import com.xuggle.xuggler.Utils
import com.xuggle.xuggler.IError
import org.reactivestreams.api.Producer
import akka.actor.ActorRefFactory
import akka.actor.Props
import akka.stream.scaladsl.Flow


case class FFMpegError(raw: IError) extends Exception(raw.getDescription)

/** An actor which reads the given file on demand. */
private[video] class FFMpegProducer(file: File) extends ActorProducer[Frame] {
  private var closed: Boolean = false
  private var frameCount: Long = 0L
  
  /** Open the reader. */  
  private val reader = ToolFactory.makeReader(file.getAbsolutePath)
  /** Register a listener that will forward all events down the Reactive Streams chain. */
  reader.addListener(new MediaListenerAdapter() {
    override def onClose(e: ICloseEvent): Unit = {
      // TODO - No way to tell if we had errors here...
      if(!closed) {
        closed = true
        onComplete()
      }
    }
    override def onVideoPicture(e: IVideoPictureEvent): Unit = {
      if(e.getMediaData.isComplete) {
        onNext(Frame(Utils.videoPictureToImage(e.getMediaData), e.getTimeStamp, e.getTimeUnit))
        frameCount += 1
      }
    }
  })
  /** Our actual behavior. */
  override def receive: Receive = {
    case ActorProducer.Request(elements) => read(elements)
    case ActorProducer.Cancel => reader.close()
  }
  
  // Reads the given number of frames, or bails on error.
  // Note: we have to track frames via the listener we have on the reader.
  private def read(frames: Int): Unit = {
    val done = frameCount + frames
    // Close event should automatically occur.
    while(!closed && frameCount < done) {
      try (reader.readPacket match {
        case null => // Ignore
        case error =>
          closed = true
          // TODO - Make sure close event hasn't gone out yet.
          onError(FFMpegError(error))
      }) catch {
        // Failure reading
        case e: Exception =>
          closed = true
          onError(e)
      }
    }
  }
}
object FFMpegProducer {
  
  def apply(factory: ActorRefFactory, file: File): Producer[Frame] = 
    ActorProducer(factory.actorOf(Props(new FFMpegProducer(file))))
}