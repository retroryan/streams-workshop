package video

import org.reactivestreams.spi.Subscriber
import com.xuggle.mediatool.event.ICloseEvent
import com.xuggle.xuggler.{IError, Utils}
import akka.actor.{Actor, Props}
import sample.utils.BasicActorSubscription.{Cancel, RequestMore}
import java.awt.Rectangle
import java.util.concurrent.TimeUnit
import java.io.{IOException, File}
import javax.imageio.ImageIO
import java.awt.image.BufferedImage

case class ScreenCaptureError(raw: IError) extends Exception(raw.getDescription)

object ScreenCaptureSubscriptionWorker {
  def props(maxFrameCount: Long, subscriber: Subscriber[Frame]): Props =
    Props(new ScreenCaptureReader(maxFrameCount, subscriber))

}

/** A subscription which only reads more packets from the file when more elements are requested. */
private[video] class ScreenCaptureReader(maxFrameCount: Long, subscriber: Subscriber[Frame]) extends Actor {

  val robot = new java.awt.Robot()

  def toolkit = java.awt.Toolkit.getDefaultToolkit

  private var closed: Boolean = false
  private var frameCount: Long = 0L

  private val startTime = System.currentTimeMillis();

  override def receive: Receive = {
    case RequestMore(elements) => requestMore(elements)
    case Cancel => cancel()
  }

  /** Actually drives capturing the screen. */
  def requestMore(elements: Int): Unit = {
    val done = frameCount + elements

    // Close event should automatically occur.
    while (!closed && frameCount < done) {
      subscriber.onNext(captureScreen())
      frameCount += 1

      //once max frame count is hit shutdown
      if (frameCount >= maxFrameCount) {
        self ! Cancel
      }
    }
  }

  def fullScreenSize: Rectangle = {
    new Rectangle(toolkit.getScreenSize)
  }

  def captureScreen(recordArea: Rectangle = fullScreenSize): Frame = {
    val image = robot.createScreenCapture(recordArea)
    //xuggle can only convert the image to video if it is this specific type
    val convertedImage = convertToType(image,BufferedImage.TYPE_3BYTE_BGR)

    // TODO -  this does not include a mouse cursor, so we may need to add one...
    //val mousePosition = java.awt.MouseInfo.getPointerInfo.getLocation
    Frame(convertedImage, System.currentTimeMillis()-startTime, TimeUnit.MILLISECONDS)
  }

  def convertToType(sourceImage:BufferedImage,targetType:Integer) =
  {
    // if the source image is already the target type, return the source image
    if (sourceImage.getType() == targetType)
     sourceImage

    // otherwise create a new image of the target type and draw the new
    // image
    else
    {
      val newImage = new BufferedImage(sourceImage.getWidth(),
        sourceImage.getHeight(), targetType);
      newImage.getGraphics().drawImage(sourceImage, 0, 0, null)
      newImage
    }
  }

  def cancel(): Unit = {
    closed = true
    subscriber.onComplete()
    context.parent ! ScreenCaptureProducerWorker.Finished
  }
}


