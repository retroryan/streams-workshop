package video

import java.io.File
import org.reactivestreams.spi.{Publisher, Subscriber}
import org.reactivestreams.api.Producer
import akka.actor.{ActorSystem, ActorRefFactory, Actor, Props, ActorRef}
import sample.utils.BasicActorSubscription

/** An implementation of a producer that will use Xuggler to read FFMpeg files. */
private[video] class FFMpegFileProducer(file: File, system: ActorRefFactory) extends AbstractProducer[Frame] {

  val ffMpegProducerWorker = system.actorOf(FFMpegProducerWorker.props(file))

  override object getPublisher extends Publisher[Frame] {
    def subscribe(subscriber: Subscriber[Frame]): Unit =
      ffMpegProducerWorker ! FFMpegProducerWorker.Subscribe(subscriber)
  }

}


object FFMpegProducerWorker {

  case class Subscribe(subscriber: Subscriber[Frame])

  case object Finished

  def props(file: File): Props = Props(new FFMpegProducerWorker(file))
}

/**
 * An Actor Worker which accepts subscription messages and manages the subscribers.
 * This uses on Worker Actor per subscription request.
 * Subscribers are stored in a map based on the Subscription Worker Actor
 */
class FFMpegProducerWorker(file: File) extends Actor {


  var subscribers = Set.empty[Subscriber[Frame]]
  var workers = Map.empty[ActorRef, Subscriber[Frame]]

  override def receive: Actor.Receive = {
    case FFMpegProducerWorker.Subscribe(subscriber) => registerSubscriber(subscriber)
    case FFMpegProducerWorker.Finished => workerFinished(sender)
  }

  def registerSubscriber(subscriber: Subscriber[Frame]): Unit = {
    val worker = context.actorOf(FFMpegSubscriptionWorker.props(file, subscriber))
    val subscription = new BasicActorSubscription(worker)
    subscribers += subscriber
    workers = workers.updated(worker, subscriber)
    subscriber.onSubscribe(subscription)
  }


  private def workerFinished(worker: ActorRef): Unit = {
    val subscriber = workers(worker)
    workers -= worker
    subscribers -= subscriber
    if (subscribers.isEmpty) {
      context.stop(self)
    }
  }

}