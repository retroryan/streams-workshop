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
import video.WebCam

object VideoDump {

  /**
   * use:
   * sbt 'runMain sample.stream.VideoDump file.mp4'
   *
   */
  def main(args: Array[String]): Unit = {
    // TODO - different main method.
    captureScreenToFile()
  }

  def sampleReadWriteFile(inputFileName:String) = {
    val system = ActorSystem()
    // EXERCISE -  Open a movie file as a Prodcuer[Frame] and push its output into
    //             a different file as a Consumer[Frame] using the raw reactive streams API.
    //  Hint:  Look at the src/main/scala/video/FFMpeg.scala file for helper methods.
    val producer: Producer[Frame] = video.FFMpeg.readFile(new File(inputFileName), system)
    val consumer: Consumer[Frame] = video.FFMpeg.writeFile(new File("test.mp4"), system, 640, 480)
    producer.produceTo(consumer)
  }
  
  def captureScreenToFile() = {
    implicit val system = ActorSystem()

    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)

    // EXERCISE - Open a screen reading Producer[Frame] which outputs its data into two locations:
    //            Both a UI display (so we can see what it captures) and a file.
    // Hint:   Use video.FFMpeg.writeFile(...) for writing to a file
    //         Use video.Display.create(..) for rendering to a swing UI
    //         Use video.ScreenCapture.readScreenCapture for grabbing screenshots.
    //         Look into akka.stream.scaladsl.Flow API for something which allows
    //          joinging mutliple consumers.
    val producer: Producer[Frame] = ScreenCapture.readScreenCapture(maxFrameCount = 100, system)
    //val producer: Producer[Frame] = WebCam.default(system)
    val fileConsumer: Consumer[Frame] = video.FFMpeg.writeFile(new File("test.mp4"), system, 640, 480)
    val displayConsumer: Consumer[Frame] = video.Display.create(system)

    Flow(producer)
      .tee(fileConsumer)
      .toProducer(materializer) produceTo displayConsumer

    // TODO - Figure out how to close the actor system...
  }
}