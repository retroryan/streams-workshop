package sample
package stream

import java.awt.Color
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow,Duct}
import akka.stream.{Transformer, MaterializerSettings, FlowMaterializer}
import org.reactivestreams.api.{Consumer, Producer}
import video.{ScreenCapture, Frame}

import scala.collection.immutable


object Movement {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val m = FlowMaterializer(MaterializerSettings())
    import concurrent.ExecutionContext.Implicits.global
    val stream: Producer[Frame] =
    //  video.FFMpeg.readFile(new java.io.File("goose.mp4"), system)
        ScreenCapture.readScreenCapture(system)
    val screen: Consumer[Frame] =
      video.Display.create(system)


    //Flow(stream).transform(DiffTransformer).produceTo(m, screen)
    stream produceTo screen
  }

  // SUPER LAZY AND BAD IMPLEMENTATION.
  def diffFrame(left: Frame, right: Frame): Frame = {
    val g = left.image.createGraphics()
    g.setXORMode(Color.WHITE)
    g.drawImage(right.image, 0, 0, null)
    g.dispose()
    left
  }
}

object DiffTransformer extends Transformer[Frame, Frame] {
  var previous: Option[Frame] = None
  def onNext(next: Frame): immutable.Seq[Frame] = {
    val result = previous match {
      case Some(frame) =>
        val image = new BufferedImage(frame.image.getWidth, frame.image.getHeight, BufferedImage.TYPE_3BYTE_BGR)
        val g = image.createGraphics()
        g.drawImage(frame.image, 0, 0, null)
        g.setXORMode(Color.WHITE)
        g.drawImage(next.image, 0, 0, null)
        immutable.Seq(frame.copy(image = image))
      case None =>
        immutable.Seq.empty
    }
    previous = Some(next)
    result
  }
}