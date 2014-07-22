package exerciseFive


import java.io.File

import akka.actor.{Props, ActorSystem}
import akka.stream.actor.ActorProducer
import akka.stream.scaladsl.Flow
import akka.stream.{FlowMaterializer, MaterializerSettings}
import org.reactivestreams.api.{Consumer, Producer}
import video.{Frame, UIControl}


object VideoPlayer {


  /**
   * run:
   *   ./activator 'runMain exerciseFive.VideoPlayer'
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("video-player")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)

    // Here are the input + output streams for the player.
    val (ui: Producer[UIControl], player: Consumer[Frame]) =
      video.Display.createPlayer(system)

    // here is the file to read.
    val videoFile = new File("goose.mp4")

    // EXERCISE 5 - Create a set of actors which will:
    // Consumer the `ui` event stream.
    // 1. On Play, it will open the video file, if needed, and begin feeding Frame events to the player
    // 2. On Pause, it will stop requesting more events from the file
    // 3. On Stop, it will close the file.

    // Hint #1:  An actor can be both a consumer and a producer
    // Hint #2: Actors can create child actors to coordinate work.


  }
}