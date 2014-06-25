package sample.start

import akka.stream.scaladsl.Flow
import akka.actor.ActorSystem
import akka.stream.{FlowMaterializer, MaterializerSettings}
import org.reactivestreams.api.Producer
import java.io.{FileOutputStream, PrintWriter}
import scala.util.{Failure, Success, Try}


object FileFlowAction {

  def runBasicFlow(getProducer: ActorSystem => Producer[String]) = {

    FileFlowAction.action(system => BasicFileProducer) {
      (flow, materializer) =>
        var count = 0L
        flow.foreach { nextLine =>
          count += 1
          println(s"$count : $nextLine ")
        }
    }

  }

  def runComplexFlow(getProducer: ActorSystem => Producer[String]) = {

    //Process the file as a stream, splitting the stream on the name of the book
    //each book will be written out to a separate file, with the line count and the text of the verse
    FileFlowAction.action(getProducer) {
      (flow, materializer) =>
        flow.map {
          //split into fields: book|chapter|verse|text
          line => line.split("\\s*\\|\\s*")
        }.
          groupBy {
          //group by the book of each line
          line => line.head
        }.
          foreach {
          //process each line using a partial function
          processLine(materializer)
        }
    }
  }


  def processLine(materializer: FlowMaterializer): PartialFunction[(String, Producer[Array[String]]), Unit] = {
    case (book, line) =>
      val output = new PrintWriter(new FileOutputStream(s"target/$book.txt"), true)
      var count = 0L
      Flow(line).
        //only print out the text of the verse and the line count
        foreach(line => {
        count += 1
        output.println(s"$count:${line.last}")
      }).
        // close resource when the group stream is completed
        onComplete(materializer)(_ => Try(output.close()))
  }


  // does this abstract away the flow handling too much
  // making it hard to understand how to manipulate flows?
  def action(getProducer: ActorSystem => Producer[String])(runFlow: (Flow[String], FlowMaterializer) => Flow[Unit]): Unit = {
    implicit val system = ActorSystem("test")
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)

    val fileProducer: Producer[String] = getProducer(system)

    val flow = Flow(fileProducer)
    runFlow(flow, materializer).onComplete(materializer)(handleOnComplete)

    System.out.println("Finished Action")

  }

  //shutdown the actor system when the flow completes
  def handleOnComplete(implicit system: ActorSystem): PartialFunction[Try[Unit], Unit] = {
    case Success(_) => system.shutdown()
    case Failure(e) =>
      println("Failure: " + e.getMessage)
      system.shutdown()
  }


}
