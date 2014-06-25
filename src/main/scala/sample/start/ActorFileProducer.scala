package sample.start

import org.reactivestreams.api.{Consumer, Producer}
import org.reactivestreams.spi.{Subscriber, Publisher}
import akka.actor._
import scala.io.Source
import sample.utils.BasicActorSubscription.Cancel
import sample.utils.BasicActorSubscription.RequestMore
import sample.utils.BasicActorSubscription


class BasicActorProducer(system: ActorSystem) extends Producer[String] {

  val fileProducerWorker = system.actorOf(FileProducerWorker.props())

  override def produceTo(c: Consumer[String]): Unit = {
    getPublisher.subscribe(c.getSubscriber)
  }

  object getPublisher extends Publisher[String] {
    def subscribe(subscriber: Subscriber[String]): Unit = {
      fileProducerWorker ! FileProducerWorker.Subscribe(subscriber)
    }
  }

}

object FileProducerWorker {

  case class Subscribe(subscriber: Subscriber[String])

  case object Finished

  def props(): Props = Props(new FileProducerWorker())
}

class FileProducerWorker extends Actor {

  val source = Source.fromFile("data/kjvdat.txt", "utf-8")

  var subscribers = Set.empty[Subscriber[String]]
  var workers = Map.empty[ActorRef, Subscriber[String]]

  override def receive: Actor.Receive = {
    case FileProducerWorker.Subscribe(subscriber) => registerSubscriber(subscriber)
    case FileProducerWorker.Finished => workerFinished(sender)
  }

  def registerSubscriber(subscriber: Subscriber[String]): Unit = {
    val worker = context.actorOf(FileSubscriptionWorker.props(source.getLines(), subscriber))
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

object FileSubscriptionWorker {
  def props(fileIterator: Iterator[String], subscriber: Subscriber[String]): Props =
    Props(new FileSubscriptionWorker(fileIterator, subscriber))

}

class FileSubscriptionWorker(fileIterator: Iterator[String], subscriber: Subscriber[String]) extends Actor {

  private var demand: Int = 0
  private var closed: Boolean = false

  override def receive: Receive = {
    case RequestMore(elements) => requestMore(elements)
    case Cancel => cancel()
  }

  def cancel(): Unit = {
    closed = true
    context.parent ! FileProducerWorker.Finished
  }


  def requestMore(elements: Int): Unit = {
    demand += elements
    while ((demand > 0) && (fileIterator.hasNext)) {
      subscriber.onNext(fileIterator.next())
      demand -= 1
    }

    if (!fileIterator.hasNext) {
      subscriber.onComplete()
    }
  }

}



