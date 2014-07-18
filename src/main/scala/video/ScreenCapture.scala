package video

import java.awt.Rectangle
import java.awt.image.BufferedImage
import java.io.File
import java.util.concurrent.TimeUnit
import akka.actor.{Props, ActorSystem}
import akka.stream.actor.ActorProducer
import org.reactivestreams.api.Producer


object ScreenCapture {

  /** Creates a screen capture and pushes its stream events out.
    */
  def readScreenCapture(system: ActorSystem): Producer[Frame] = {
    val ref = system.actorOf(Props(new ScreenCaptureProducer))
    ActorProducer(ref)
  }
}



/** An implementation of a producer that that can grab the screen. It will generate a stream of the captured video later.
  *
  *  maxFrameCount is a temporary to limit the amount of frames capture since screen capture can be an infinite stream.
  *  Ideal would be to hook-up the start / stop stream events similar to what is used for controlling streaming video
  *
  * */
private[video] class ScreenCaptureProducer extends ActorProducer[Frame] {

  def receive: Receive = {
    case ActorProducer.Request(e) => runGrabs()
    case ActorProducer.Cancel => context stop self
  }

  def runGrabs(): Unit =
    while(totalDemand > 0) {
      onNext(grabScreen())
    }

  private val robot = new java.awt.Robot()

  def toolkit = java.awt.Toolkit.getDefaultToolkit
  private var frameCount: Long = 0L
  private val startTime = System.currentTimeMillis();


  def fullScreenSize: Rectangle = {
    new Rectangle(toolkit.getScreenSize)
  }

  def grabScreen(recordArea: Rectangle = fullScreenSize): Frame = {
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
    if (sourceImage.getType() == targetType) sourceImage
    else {
      val newImage = new BufferedImage(sourceImage.getWidth(),
        sourceImage.getHeight(), targetType);
      newImage.getGraphics().drawImage(sourceImage, 0, 0, null)
      newImage
    }
  }
}
