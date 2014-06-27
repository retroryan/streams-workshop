package sample.stream

import akka.stream.scaladsl.Flow
import java.io.File
import org.reactivestreams.api.Producer
import video.{FlowAction, Frame}
import org.reactivestreams.api.Consumer
import akka.actor.ActorSystem
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent

object ShowVideo {

  /**
   * use:
   * sbt 'runMain sample.stream.ShowVideo file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)
    val consumer: Consumer[Frame] = video.Display.create(system)
    fileProducer.produceTo(consumer)
    
  }
}