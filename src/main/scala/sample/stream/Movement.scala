package sample
package stream

import java.awt.Color
import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.stream.scaladsl.Flow
import akka.stream.{MaterializerSettings, FlowMaterializer}
import org.reactivestreams.api.{Consumer, Producer}
import video.Frame


object Movement {
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    implicit val m = FlowMaterializer(MaterializerSettings())
    import concurrent.ExecutionContext.Implicits.global
    val stream: Producer[Frame] =
      video.FFMpeg.readFile(new java.io.File("goose.mp4"), system)
    val screen: Consumer[Frame] =
      video.Display.create(system)
    Flow(stream).grouped(2).map {
      case Seq(left, right) =>
        diffFrame(left,right)
    }.produceTo(m, screen)
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