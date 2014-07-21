package video

import com.github.sarxos.webcam.{Webcam=>WC}
import java.util.concurrent.TimeUnit
import akka.stream.actor.ActorProducer
import akka.actor.Props
import org.reactivestreams.api.Producer
import collection.JavaConverters._
import akka.actor.ActorSystem
import akka.actor.ActorRefFactory

object WebCam {

  def default(system: ActorRefFactory): Producer[Frame] = 
    cameraStream(system)(WC.getDefault)

  def cameraStreams(system: ActorSystem): Seq[Producer[Frame]] =
    WC.getWebcams.asScala map cameraStream(system)
    
  private def cameraStream(system: ActorRefFactory)(cam: WC): Producer[Frame] =
    ActorProducer(system.actorOf(WebCamProducer.props(cam)))
}

object WebCamProducer {
  def props(cam: WC): Props = Props(new WebCamProducer(cam))
}

/** An actor which reads the given file on demand. */
private[video] class WebCamProducer(cam: WC) extends ActorProducer[Frame] {
    /** Our actual behavior. */
  override def receive: Receive = {
    case ActorProducer.Request(elements) => 
      while(totalDemand > 0) onNext(snap())
    case ActorProducer.Cancel => cam.close()
        context stop self
  }
  
  // Grab a webcam snapshot.
  def snap(): Frame = {
     if(!cam.isOpen) cam.open()
     Frame(cam.getImage, System.nanoTime, TimeUnit.NANOSECONDS)
  }
}
