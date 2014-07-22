package video

import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit
import javax.imageio.ImageIO
import java.io.{ObjectOutputStream, ObjectInputStream}

/** Fundamental unit of video in the client. */
case class Frame(@transient var image: BufferedImage, timeStamp: Long, timeUnit: TimeUnit) extends Serializable{

  private def writeObject(out: ObjectOutputStream) {
    out.defaultWriteObject
    ImageIO.write(image, "png", out)
  }

  private def readObject(in: ObjectInputStream) {
    in.defaultReadObject
    image = ImageIO.read(in)
  }
}
