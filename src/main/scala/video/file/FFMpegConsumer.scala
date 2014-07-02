package video
package file

import java.io.File
import org.reactivestreams.spi.{Publisher, Subscriber, Subscription}
import org.reactivestreams.api.Consumer
import akka.actor.{ActorSystem, Actor, Props, ActorRef}
import sample.utils.BasicActorSubscription
import com.xuggle.mediatool.ToolFactory
import com.xuggle.mediatool.IMediaWriter
import java.util.concurrent.TimeUnit
import com.xuggle.xuggler.IRational
import akka.actor.ActorRefFactory
import akka.stream.actor.ActorConsumer



private[video] class FFMpegFileConsumerWorker(file: File, width: Int, height: Int, frameRate: IRational = IRational.make(3, 1)) extends ActorConsumer {
  override protected val requestStrategy = ActorConsumer.OneByOneRequestStrategy
  private val writer: IMediaWriter = ToolFactory.makeWriter(file.getAbsolutePath)
  writer.addVideoStream(0, 0, frameRate, width, height)
  override def receive: Receive = {
    case ActorConsumer.OnNext(frame: Frame) =>
      writer.encodeVideo(0, frame.image, frame.timeStamp, frame.timeUnit)
    case ActorConsumer.OnComplete =>
      writer.close()
    // Destory bad files
    case ActorConsumer.OnError(e) =>
      writer.close()
      file.delete()
  }
}

private[video] object FFMpegFileConsumerWorker {
  def apply(factory: ActorRefFactory, file: File, width: Int, height: Int, frameRate: IRational = IRational.make(3, 1)): Consumer[Frame] =
    ActorConsumer(factory.actorOf(Props(new FFMpegFileConsumerWorker(file, width, height, frameRate))))
}