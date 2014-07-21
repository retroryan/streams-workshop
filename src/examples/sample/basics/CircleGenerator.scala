package sample.basics

import scala.util.Random
import video.imageUtils.{ImageUtils, CircleProperties}

class CircleGenerator extends akka.actor.Actor {

  val rand = new Random()

  override def receive: Receive = {
    case CircleGenerator.NextNumber(maxWidth, maxHeight) => {
      val width = rand.nextInt(maxWidth)
      val height = rand.nextInt(maxHeight)
      sender ! CircleProperties(width, height, ImageUtils.randColor)
    }

  }
}

object CircleGenerator {
  case class NextNumber(maxWidth: Int, maxHeight: Int)
}
