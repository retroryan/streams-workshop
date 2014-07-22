package exerciseFour

import akka.actor.ActorSystem
import akka.stream.MaterializerSettings
import video.Frame
import org.reactivestreams.api.Producer
import java.io.File
import akka.stream.scaladsl.Flow

object MovementVideo {

  /**
   * run:
   *   ./activator 'runMain exerciseFour.MovementVideo'
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    val settings = MaterializerSettings()

    // ------------
    // EXERCISE 4.1
    // ------------
    // Fill in the code necessary to create a flow dsl and manipulate the video stream to be grayscale.

    // Given - The location of the mp4 we can display (note first few seconds are still frame).
    val mp4 = new File("goose.mp4")

    //Create a Producer from the file system
    val fileProducer: Producer[Frame] = video.FFMpeg.readFile(mp4, system)
    val flow = Flow(fileProducer)

    // TODO - Your code here to consume and manipulate the video stream in a flow dsl.
  }
}