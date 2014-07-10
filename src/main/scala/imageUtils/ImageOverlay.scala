package imageUtils

import java.awt._
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class ImageOverlay(imageFile: File) {
  private val  img: BufferedImage = ImageIO.read(imageFile)
  private val bg = new Color(0.0f, 0.0f, 0.0f, 0.0f)
  def overlayOnto(buf: BufferedImage): Unit = {
    val g: Graphics2D = buf.createGraphics
    //Create an alpha composite of 50%
    //val alpha: AlphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f)
    val alpha = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f)
    g.setComposite(alpha)
    //g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    g.drawImage(img, 0, 0, buf.getWidth, buf.getHeight, 0, 0, img.getWidth, img.getHeight, bg, null)
    //Free graphic resources
    g.dispose
  }
}