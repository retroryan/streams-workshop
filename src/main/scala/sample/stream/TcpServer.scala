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

object TcpServer {

  /**
   * Use parameters `server 0.0.0.0 6001` to start server listening on port 6001.
   *
   */
  def main(args: Array[String]): Unit = {
    val serverAddress =
      if (args.length == 3) new InetSocketAddress(args(1), args(2).toInt)
      else new InetSocketAddress("127.0.0.1", 6000)

    val system = ActorSystem("Server")
    server(system, serverAddress)
  }

  def server(system: ActorSystem, serverAddress: InetSocketAddress): Unit = {
    implicit val sys = system
    implicit val ec = system.dispatcher
    val settings = MaterializerSettings()
    val materializer = FlowMaterializer(settings)
    implicit val timeout = Timeout(5.seconds)

    val serverFuture = (IO(StreamTcp) ? StreamTcp.Bind(serverAddress, settings = settings))

    serverFuture.onSuccess {
      case serverBinding: StreamTcp.TcpServerBinding =>
        println("Server started, listening on: " + serverBinding.localAddress)

        Flow(serverBinding.connectionStream).foreach { conn ⇒
          println("Client connected from: " + conn.remoteAddress)
          conn.inputStream.produceTo(conn.outputStream)
        }.consume(materializer)
    }

    serverFuture.onFailure {
      case e: Throwable =>
        println(s"Server could not bind to $serverAddress: ${e.getMessage}")
        system.shutdown()
    }

  }


}