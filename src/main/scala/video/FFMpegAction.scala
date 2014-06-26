package video

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import akka.util.Timeout
import org.reactivestreams.api.Producer
import java.io.File
import akka.stream.scaladsl.Flow
import scala.util.{Failure, Success, Try}

object FFMpegAction {

  /**
   * Does this abstract away the flow handling too much
   * making it hard to understand how to manipulate flows?
   *
   *
   * Maybe it would be better to just have the input of runFlow be the Producer instead of the flow?
   *
   * @param args
   * @param runFlow
   * @return
   */
  def action(args: Array[String])(runFlow:(Flow[Frame], FlowMaterializer) => Flow[Unit]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)
    val fileProducer: Producer[Frame] = video.FFMpeg.readFile(new File(args(0)), system)

    val flow = Flow(fileProducer)
    runFlow(flow, materializer)
      .onComplete(materializer)(handleOnComplete)

    System.out.println()
  }

  /**
   * ShowVideo consumes the stream and doesn't return a flow.
   * Is there a better way to share with action?
   *
   * @param args
   * @param runFlow
   * @return
   */
  def basicAction(args: Array[String])(runFlow:(Flow[Frame], FlowMaterializer) => Unit): Unit = {
     action(args) {
        (flow,materializer) =>
          runFlow(flow,materializer)
          flow.asInstanceOf[Flow[Unit]]
      }
  }

  //shutdown the actor system when the flow completes
  def handleOnComplete(implicit system: ActorSystem): PartialFunction[Try[Unit], Unit] = {
    case Success(_) => system.shutdown()
    case Failure(e) =>
      println("Failure: " + e.getMessage)
      system.shutdown()
  }

}
