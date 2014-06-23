package sample.stream

import java.net.InetSocketAddress
import scala.concurrent.duration._
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.stream.io.StreamTcp
import akka.stream.scaladsl.Flow
import akka.util.Timeout
import java.io.File

object FrameCount {

  /**
   * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   *
   */
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)
    var count = 0L
    val flow = 
     Flow(video.FFMpeg.readFile(new File(args(0)))).foreach { frame =>
       count += 1
       System.out.print(f"\rFRAME ${count}%05d")
     }.consume(materializer)
    System.out.println()
    
  }
}