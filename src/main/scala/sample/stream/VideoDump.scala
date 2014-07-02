package sample.stream

import akka.stream.scaladsl.Flow
import java.io.File
import org.reactivestreams.api.Producer
import video.{ScreenCapture, FlowAction, Frame}
import org.reactivestreams.api.Consumer
import akka.actor.ActorSystem
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import com.xuggle.mediatool.ToolFactory
import imageUtils.ConvertImage
import akka.stream.{FlowMaterializer, MaterializerSettings}

object VideoDump {

  /**
   * use:
   * sbt 'runMain sample.stream.VideoDump file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    captureScreenToFile()
  }

  def captureScreenToFile() = {
    implicit val system = ActorSystem()

    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)

    val producer: Producer[Frame] = ScreenCapture.readScreenCapture(maxFrameCount = 100, system)
    val fileConsumer: Consumer[Frame] = video.FFMpeg.writeFile(new File("test.mp4"), system, 640, 480)
    val displayConsumer: Consumer[Frame] = video.Display.create(system)

    Flow(producer)
      .tee(fileConsumer)
      .toProducer(materializer) produceTo displayConsumer

    // TODO - Figure out how to close the actor system...
  }

  def sampleReadWriteFile(inputFileName:String) = {
    val system = ActorSystem()
    val producer: Producer[Frame] = video.FFMpeg.readFile(new File(inputFileName), system)
    val consumer: Consumer[Frame] = video.FFMpeg.writeFile(new File("test.mp4"), system, 640, 480)
    producer.produceTo(consumer)

  }
  
  // Init...
  //try ToolFactory.makeWriter("dummy.mp4").close()
  //catch {
  //  case t: Throwable => /* ignore */
  //}
}