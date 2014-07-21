package exerciseOne

import akka.actor.ActorSystem
import org.reactivestreams.api.{Consumer, Producer}
import video.Frame


object DisplayWebcam {

  def main(args: Array[String]): Unit = {
    // ActorSystem represents the "engine" we run in, including threading configuration and concurrency semantics.
    val system = ActorSystem()

    // ------------
    // EXERCISE 1.1
    // ------------
    // Fill in the code necessary to pipe either the webcam or the screen captures into the UI display.
    // If you have a webcam, use the webcam producer, otherwise use the screen capture producer.


    // Captures webcam photos.
    val webcam: Producer[Frame] = video.webcam(system)
    // Captures screenshots
    val screen: Producer[Frame] = video.screen(system)
    // Renders a video stream in a Swing UI.
    val display: Consumer[Frame] = video.display(system)

    // TODO - Your code here.
  }
}
