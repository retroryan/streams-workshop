package exerciseOne

import video.Frame
import akka.stream.actor.ActorProducer
import akka.actor.ActorSystem

// ------------
// EXERCISE 1.4
// ------------
// Fill in the code necessary to produce random circles based on the requested demand.
// The properties of the circles should be retrieved from the the CircleGenerator actor
// which will return the random properties of a circle.  The random circles
// should then be drawn to a Buffered Image and used to create the Frame.
//
// When the Frame is ready it should be sent to the consumer using:
//      onNext(Frame ... )
//
// See video.imageUtils.ImageUtils.createBufferedImage
class CircleProducer extends ActorProducer[Frame] {

  override def receive: Receive = {

    case ActorProducer.Request(elements) => ???

    case ActorProducer.Cancel => context stop self
  }

}


object CircleProducer {

  def main(args: Array[String]): Unit = {
    // ActorSystem represents the "engine" we run in, including threading configuration and concurrency semantics.
    val system = ActorSystem()

    // Fill in the code necessary to construct a UI display and consume the Frames produced
    // by the Circle producer.

    // TODO - Your code here.

  }
}
