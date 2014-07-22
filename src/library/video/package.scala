import java.io.File

import akka.actor.{ActorSystem, ActorRefFactory}
import org.reactivestreams.api.{Consumer, Producer}

/**
 * Helper methods for reactive streams workshop.
 */
package object video {


  /**
   * Constructs a Swing UI which will render, at 1x speed, the stream of frames it's consuming.
   *
   * @param system  The actor system used to run the Display.
   * @return   The consumer of frames we can use to render in Swing.
   */
  def display(system: ActorSystem): Consumer[Frame] = Display.create(system)


  /** Construct a swing UI with two pieces:
    * 1. a  button panel which generates UIControl events, like Play/Pause/Stop
    * 2. A video panel which can render a video stream.
    *
    * @param system  The actor system used to run the UI.
    * @return  A tuple containing the Producer of UI events and the consumer of Frames.
    */
  def displayWithControl(system: ActorSystem): (Producer[UIControl], Consumer[Frame]) =
    Display.createPlayer(system)


  /** Helper method which will read a movie file (mp4 or other FFMPeg supported) and produce a stream of just the
    * video frames.
    *
    * @param file  Location of the move file
    * @param factory An actor factory we use to instantiate the underlying producer.
    * @return  A Producer of video frames.
    */
  def readFFmpegFile(file: File, factory: ActorRefFactory): Producer[Frame] =
    FFMpeg.readFile(file, factory)


  /**
   * Obtains a stream which captures screenshots form the default webcam.
   * @param system  The actor factory we use to construct our stream instance.
   * @return
   *         A stream of webcam snapshots.
   */
  def webcam(system: ActorRefFactory) = WebCam.default(system)

  /**
   * Obtains a stream of screen capture snapshots.
   * @param system  The actor factory we use to construct the stream instance
   * @return  A stream of screen captures.
   */
  def screen(system: ActorRefFactory) = ScreenCapture.readScreenCapture(system)

}
