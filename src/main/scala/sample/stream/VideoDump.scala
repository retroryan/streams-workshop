package sample.stream

import akka.stream.scaladsl.Flow
import java.io.File
import org.reactivestreams.api.Producer
import video.{FlowAction, Frame}
import org.reactivestreams.api.Consumer
import akka.actor.ActorSystem
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import com.xuggle.mediatool.ToolFactory

object VideoDump {

  /**
   * use:
   * sbt 'runMain sample.stream.VideoDump file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    val system = ActorSystem()
    val producer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)
    val consumer: Consumer[Frame] = video.FFMpeg.writeFile(new File("test.mp4"), system, 640, 480)
    producer.produceTo(consumer)
    // TODO - Figure out how to close the actor system...
  }
  
  // Init...
  //try ToolFactory.makeWriter("dummy.mp4").close()
  //catch {
  //  case t: Throwable => /* ignore */
  //}
}