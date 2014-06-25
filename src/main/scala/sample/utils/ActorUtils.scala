package sample.utils

import akka.actor.ActorRef
import org.reactivestreams.spi.Subscription

object BasicActorSubscription {

  case object Cancel

  case class RequestMore(elements: Int)

}

class BasicActorSubscription(subscriptionWorker: ActorRef) extends Subscription {

  import BasicActorSubscription._

  def cancel(): Unit = subscriptionWorker ! Cancel

  def requestMore(elements: Int): Unit =
    if (elements <= 0) throw new IllegalArgumentException("The number of requested elements must be > 0")
    else subscriptionWorker ! RequestMore(elements)

  override def toString = "BasicActorSubscription"
}
