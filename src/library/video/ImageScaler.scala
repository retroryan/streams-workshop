package video

import java.awt.RenderingHints
import java.awt.image.BufferedImage


object ImageScaler {
  //Note: This is borrowed from https://github.com/cb372/scala-ascii-art/blob/master/src/main/scala/com/github/cb372/asciiart/Asciifier.scala
  def scale(image: BufferedImage, widthSetting: Option[Int]): BufferedImage = {
    val (width, height) = chooseDimensions(image, widthSetting)
    val scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val gfx = scaledImage.createGraphics()
    gfx.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
    gfx.drawImage(image, 0, 0, width, height, null)
    gfx.dispose
    scaledImage
  }

  // TODO - fix this method a bit.
  private def chooseDimensions(image: BufferedImage, widthSetting: Option[Int]): (Int, Int) = {
    widthSetting match {
      case Some(width) => {
        // use specified width, keep aspect ratio the same
        val height = calcResizedHeight(image, width)
        (width, height)
      }
      case None => {
        if (image.getWidth <= maxSensibleWidth && image.getHeight <= maxSensibleHeight) {
          // never enlarge the original image
          (image.getWidth, image.getHeight)
        } else {
          // image is too tall and/or wide
          // try reducing the width, see if height is now ok
          val tryHeight = calcResizedHeight(image, maxSensibleWidth)
          if (tryHeight <= maxSensibleHeight) {
            (maxSensibleWidth, tryHeight)
          } else {
            // reduce height, see if width is now ok
            val tryWidth = calcResizedWidth(image, maxSensibleHeight)
            if (tryWidth <= maxSensibleWidth) {
              (tryWidth, maxSensibleHeight)
            } else {
              // give up, just make image as big as possible
              (maxSensibleWidth, maxSensibleHeight)
            }
          }
        }
      }
    }
  }
  private def calcResizedHeight(image: BufferedImage, resizedWidth: Int): Int =
    ((resizedWidth.toDouble / image.getWidth) * image.getHeight).toInt

  private def calcResizedWidth(image: BufferedImage, resizedHeight: Int): Int =
    ((resizedHeight.toDouble / image.getHeight) * image.getWidth).toInt

  //TODO - don't hardcode width/height
  private val maxSensibleWidth = 120
  private val maxSensibleHeight = 50

}