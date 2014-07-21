package video.imageUtils

import java.awt._
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageOverlay(imageFile: File) {
  private val  img: BufferedImage = ImageIO.read(imageFile)
  val alpha = {
    if(!img.getColorModel.hasAlpha) {
      System.err.println("Image had no alpha values, blending will be lame!")
      0.2f
    } else 1.0f
  }
  private val bg = new Color(0.0f, 0.0f, 0.0f, 0.0f)
  def overlayOnto(buf: BufferedImage): Unit = {
    val g: Graphics2D = buf.createGraphics
    //Create an alpha composite of 50%
    // TODO - this is BAD in some way, we'd like a raw overlay function, but
    // didn't have time to research all of alpha composite API.
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha))
    g.drawImage(img, 0, 0, buf.getWidth, buf.getHeight, 0, 0, img.getWidth, img.getHeight, bg, null)
    //Free graphic resources
    g.dispose
  }

  def overlay(buf: BufferedImage): BufferedImage = {
    val result = new BufferedImage(buf.getWidth, buf.getHeight, BufferedImage.TYPE_4BYTE_ABGR)
    val g: Graphics2D = result.createGraphics
    g.drawImage(buf, 0, 0, buf.getWidth, buf.getHeight, 0, 0, buf.getWidth, buf.getHeight, bg, null)
    //Create an alpha composite of 50%
    // TODO - this is BAD in some way, we'd like a raw overlay function, but
    // didn't have time to research all of alpha composite API.
    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha))
    g.drawImage(img, 0, 0, buf.getWidth, buf.getHeight, 0, 0, img.getWidth, img.getHeight, bg, null)
    //Free graphic resources
    g.dispose
    result
  }
}