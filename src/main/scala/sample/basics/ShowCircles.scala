package sample.basics

import org.reactivestreams.api.Producer
import video.Frame
import org.reactivestreams.api.Consumer
import akka.actor.ActorSystem

object ShowCircles {

  /**
   * use:
   * sbt 'runMain sample.basics.ShowCircles'
   *
   */
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    val circleProducer: Producer[Frame] = CircleProducer(system)
    val consumer: Consumer[Frame] = video.Display.create(system)
    circleProducer.produceTo(consumer)
  }
}