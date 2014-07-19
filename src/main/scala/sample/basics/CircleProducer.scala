package sample.basics

import akka.stream.actor.ActorProducer
import video.{DisplayProperties, Frame}
import akka.actor.{Props, ActorRefFactory}
import org.reactivestreams.api.Producer
import java.util.Date
import java.util.concurrent.TimeUnit
import imageUtils.{ImageUtils, CircleProperties}


class CircleProducer extends ActorProducer[Frame] {
  private var frameCount: Long = 0L
  private var closed: Boolean = false

  val circleGeneratorRef = context.actorOf(Props[CircleGenerator])
  val width = DisplayProperties.getWidth(context.system)
  val height = DisplayProperties.getHeight(context.system)

  /** Our actual behavior. */
  override def receive: Receive = {
    case ActorProducer.Request(elements) => generateCircles(elements)

    case circleProperties@CircleProperties(_, _, _) => produceCircles(circleProperties)

    case ActorProducer.Cancel =>
      context stop self
  }

  private def generateCircles(frames: Int): Unit = {
    val done = frameCount + frames
    // Close event should automatically occur.
    while (!closed && frameCount < done) {
      //Ask for a circle slightly smaller than the frame size
      circleGeneratorRef ! CircleGenerator.NextNumber(width - 25, height - 25)
      frameCount += 1
    }
  }

  private def produceCircles(circleProperties: CircleProperties) = {
    val circleImage = ImageUtils.createBufferedImage(width, height, circleProperties)
    onNext(Frame(circleImage, new Date().getTime, TimeUnit.MILLISECONDS))
  }
}

object CircleProducer {
  def apply(factory: ActorRefFactory): Producer[Frame] =
    ActorProducer(factory.actorOf(Props(new CircleProducer())))
}