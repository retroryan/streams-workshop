package video

import org.reactivestreams.spi.{
  Publisher, Subscriber, Subscription
}
import org.reactivestreams.api.{
  Producer, Consumer
}

/** Helper class which we THINK is correct, to make it less ugly to implement raw Producers from Xuggler API. */
abstract class AbstractProducer[T] extends Producer[T] {
  override def produceTo(c: Consumer[T]): Unit = {
    getPublisher subscribe c.getSubscriber
  }
}