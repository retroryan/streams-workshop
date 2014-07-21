package video.imageUtils

import scala.util.Random
import java.awt.Color
import java.awt.image.BufferedImage

object ImageUtils {

  val rand = new Random()

  def randColor = {
    val r: Float = rand.nextFloat()
    val g: Float = rand.nextFloat()
    val b: Float = rand.nextFloat()
    new Color(r, g, b)
  }

  def createBufferedImage(width: Int, height: Int, circleProperties: CircleProperties) = {
    val circleImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val graphics = circleImage.createGraphics()
    graphics.setColor(circleProperties.color)
    graphics.fillOval(0, 0, circleProperties.width, circleProperties.height)
    circleImage
  }

}
