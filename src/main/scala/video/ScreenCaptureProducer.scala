package video


import org.reactivestreams.spi.{Publisher, Subscriber}
import akka.actor.{ActorSystem, Actor, Props, ActorRef}
import sample.utils.BasicActorSubscription

/** An implementation of a producer that that can grab the screen. It will generate a stream of the captured video later.
  *
  *  maxFrameCount is a temporary to limit the amount of frames capture since screen capture can be an infinite stream.
  *  Ideal would be to hook-up the start / stop stream events similar to what is used for controlling streaming video
  *
  * */
private[video] class ScreenCaptureProducer(maxFrameCount: Long, system: ActorSystem) extends AbstractProducer[Frame] {

  val screenCaptureProducerWorker = system.actorOf(ScreenCaptureProducerWorker.props())

  override object getPublisher extends Publisher[Frame] {
    def subscribe(subscriber: Subscriber[Frame]): Unit =
      screenCaptureProducerWorker ! ScreenCaptureProducerWorker.Subscribe(maxFrameCount, subscriber)
  }

}


object ScreenCaptureProducerWorker {

  case class Subscribe(maxFrameCount: Long, subscriber: Subscriber[Frame])

  case object Finished

  def props(): Props = Props(new ScreenCaptureProducerWorker())
}

/**
 * An Actor Worker which accepts subscription messages and manages the subscribers.
 * This uses on Worker Actor per subscription request.
 * Subscribers are stored in a map based on the Subscription Worker Actor
 */
class ScreenCaptureProducerWorker() extends Actor {

  var subscribers = Set.empty[Subscriber[Frame]]
  var workers = Map.empty[ActorRef, Subscriber[Frame]]

  override def receive: Actor.Receive = {
    case ScreenCaptureProducerWorker.Subscribe(maxFrameCount, subscriber) => registerSubscriber(maxFrameCount, subscriber)
    case ScreenCaptureProducerWorker.Finished => workerFinished(sender)
  }

  def registerSubscriber(maxFrameCount: Long, subscriber: Subscriber[Frame]): Unit = {
    val worker = context.actorOf(ScreenCaptureSubscriptionWorker.props(maxFrameCount, subscriber))
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