package video

import java.io.File
import org.reactivestreams.spi.{Publisher, Subscriber, Subscription}
import org.reactivestreams.api.{Producer, Consumer}
import akka.actor.{ActorSystem, Actor, Props, ActorRef}
import sample.utils.BasicActorSubscription
import com.xuggle.xuggler.IRational

/** Helper for dealing with FFMpeg data. */
object FFMpeg {
  /** Reads a given file and pushes its stream events out. 
    * Note: This will not prefetch any data, but only read when requested.
    */
  def readFile(file: File, system: ActorSystem): Producer[Frame] = new FFMpegFileProducer(file, system)
  /**
   * Writes a stream of frames to the given file as an FFMpeg.
   */
  def writeFile(file: File, system: ActorSystem, width: Int, height: Int, frameRate: IRational = IRational.make(3, 1)): Consumer[Frame] = new FFMpegFileConsumer(file, system, width, height, frameRate)
}