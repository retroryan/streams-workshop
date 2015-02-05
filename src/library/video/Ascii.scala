package video


import java.awt.{RenderingHints, Color}
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ActorRefFactory}
import org.reactivestreams.api.{Consumer, Producer}
import akka.stream.scaladsl.Duct

case class AsciiFrame(width: Int, height: Int, image: String, timeStamp: Long, timeUnit: TimeUnit)


object Ascii {

  def asciifier(rows: Int, cols: Int): Duct[Frame, AsciiFrame] =
    Duct.apply[Frame] map { frame =>
      toAscii(frame, rows, cols)
    }
  def toAscii(frame: Frame, rows: Int, cols: Int): AsciiFrame = {
    val scaled = ImageScaler.scale(frame.image, Some(cols))
    // First we need to resize the image
    AsciiFrame(scaled.getWidth, scaled.getHeight, toAscii(scaled), frame.timeStamp, frame.timeUnit)
  }


  // NOTE - this is ugly because we try to compress the string while we render.
  //        Avoding writing the control codes has hugely noticable affect on rendering.
  private def toAscii(image: BufferedImage): String = {
    case class MyState(lastColor: String, buf: StringBuilder)
    val buf = new StringBuilder("")
    var lastColor = ""
    var y = 0
    while(y < image.getHeight) {
      var x = 0
      while(x < image.getWidth) {
        val pixel = new Color(image.getRGB(x,y))
        val color = Ansi.FOREGROUND_COLOR(pixel)
        if(lastColor != color) buf.append(color)
        val char = chooseAsciiChar(pixel)
        buf.append(char)
        lastColor = color
        x += 1
      }
      buf.append("\n")
      lastColor=""
      y += 1
    }
    buf.toString()
  }

  // Convert color magnitude into a character
  private val asciiChars = List('#','A','@','%','$','+','=','*',':',',','.',' ')
  /** Converts a color to an ascii character based on its intensity. */
  def chooseAsciiChar(color: Color) = {
    def rgbMax =
      math.max(color.getRed, math.max(color.getGreen, color.getBlue))
    rgbMax match {
      case 0 => asciiChars.last
      case n => {
        val index = ((asciiChars.length * (rgbMax / 255)) - (0.5)).toInt
        asciiChars(index)
      }
    }
  }
}

