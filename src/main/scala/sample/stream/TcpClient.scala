package sample.stream

import java.net.InetSocketAddress
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import akka.actor.ActorSystem
import akka.pattern.ask
import akka.io.IO
import akka.stream.FlowMaterializer
import akka.stream.MaterializerSettings
import akka.stream.io.StreamTcp
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import akka.util.Timeout

object TcpClient {

  /**
   *
   * Use parameters `127.0.0.1 6001` to start client connecting to
   * server on 127.0.0.1:6001.
   *
   */
  def main(args: Array[String]): Unit = {

    val serverAddress =
      if (args.length == 3) new InetSocketAddress(args(1), args(2).toInt)
      else new InetSocketAddress("127.0.0.1", 6000)

    val system = ActorSystem("Client")
    client(system, serverAddress)


  }

  def client(system: ActorSystem, serverAddress: InetSocketAddress): Unit = {
    implicit val sys = system
    implicit val ec = system.dispatcher
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)

    val clientFuture = (IO(StreamTcp) ? StreamTcp.Connect(remoteAddress = serverAddress, settings = settings))
    clientFuture.onSuccess {
      case clientBinding: StreamTcp.OutgoingTcpConnection =>
        val testInput = ('a' to 'z').map(ByteString(_))
        Flow(testInput).toProducer(materializer).produceTo(clientBinding.outputStream)

        Flow(clientBinding.inputStream).fold(Vector.empty[Char]) { (acc, in) ⇒ acc ++ in.map(_.asInstanceOf[Char])}.
          foreach(result => println(s"Result: " + result.mkString("[", ", ", "]"))).
          onComplete(materializer) {
          case Success(_) =>
            println("Shutting down client")
            system.shutdown()
          case Failure(e) =>
            println("Failure: " + e.getMessage)
            system.shutdown()
        }
    }

    clientFuture.onFailure {
      case e: Throwable =>
        println(s"Client could not connect to $serverAddress: ${e.getMessage}")
        system.shutdown()
    }

  }

}