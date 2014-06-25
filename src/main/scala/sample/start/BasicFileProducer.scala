package sample.start

import org.reactivestreams.api.{Consumer, Producer}
import org.reactivestreams.spi.{Subscription, Subscriber, Publisher}
import scala.io.Source


class BasicFileProducer extends Producer[String] {

  override def produceTo(c: Consumer[String]): Unit = {
    getPublisher.subscribe(c.getSubscriber)
  }

  class FilePublisher extends Publisher[String] {
    def subscribe(subscriber: Subscriber[String]): Unit =
      subscriber.onSubscribe((new FileReader(subscriber)))
  }

  override def getPublisher(): Publisher[String] = new FilePublisher
}

class FileReader(subscriber: Subscriber[String]) extends Subscription {

  val source = Source.fromFile("data/kjvdat.txt", "utf-8")

  val fileIterator = source.getLines()

  private var demand: Int = 0

  private var closed: Boolean = false

  override def cancel(): Unit =
    closed = true


  override def requestMore(elements: Int): Unit = {
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



