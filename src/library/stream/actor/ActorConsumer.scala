/**
 * Copyright (C) 2014 Typesafe Inc. <http://www.typesafe.com>
 *
 *   This is copied from the akka stream source at:
 *   https://github.com/akka/akka/tree/releasing-akka-stream-and-http-experimental-0.4
 *
 *   This was copied to get the Actor Producer to work with clustering.
 *
 *   The only change needed was to modify the class ActorSubscriber so that it extends serializable.
 */
package stream.actor

import java.util.concurrent.ConcurrentHashMap
import org.reactivestreams.api.Consumer
import org.reactivestreams.spi.Subscriber
import org.reactivestreams.spi.Subscription
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.ExtendedActorSystem
import akka.actor.Extension
import akka.actor.ExtensionId
import akka.actor.ExtensionIdProvider
import akka.stream.actor.ActorConsumerImpl
import stream.actor

object ActorConsumer {

  /**
   * Attach a [[ActorConsumer]] actor as a [[org.reactivestreams.api.Consumer]]
   * to a [[org.reactivestreams.api.Producer]] or [[akka.stream.Flow]].
   */
  def apply[T](ref: ActorRef): Consumer[T] = actor.ActorConsumerImpl(ref)

  /**
   * Java API: Attach a [[ActorConsumer]] actor as a [[org.reactivestreams.api.Consumer]]
   * to a [[org.reactivestreams.api.Producer]] or [[akka.stream.Flow]].
   */
  def create[T](ref: ActorRef): Consumer[T] = apply(ref)

  @SerialVersionUID(1L) case class OnNext(element: Any)

  @SerialVersionUID(1L) case object OnComplete

  @SerialVersionUID(1L) case class OnError(cause: Throwable)

  /**
   * INTERNAL API
   */
  @SerialVersionUID(1L) case class OnSubscribe(subscription: Subscription)

  /**
   * An [[ActorConsumer]] defines a `RequestStrategy` to control the stream back pressure.
   */
  trait RequestStrategy {
    /**
     * Invoked by the [[ActorConsumer]] after each incoming message to
     * determine how many more elements to request from the stream.
     *
     * @param remainingRequested current remaining number of elements that
     *                           have been requested from upstream but not received yet
     * @return demand of more elements from the stream, returning 0 means that no
     *         more elements will be requested
     */
    def requestDemand(remainingRequested: Int): Int
  }

  /**
   * Requests one more element when `remainingRequested` is 0, i.e.
   * max one element in flight.
   */
  case object OneByOneRequestStrategy extends RequestStrategy {
    def requestDemand(remainingRequested: Int): Int =
      if (remainingRequested == 0) 1 else 0
  }

  /**
   * When request is only controlled with manual calls to
   * [[ActorConsumer# r e q u e s t]].
   */
  case object ZeroRequestStrategy extends RequestStrategy {
    def requestDemand(remainingRequested: Int): Int = 0
  }

  object WatermarkRequestStrategy {
    /**
     * Create [[WatermarkRequestStrategy]] with `lowWatermark` as half of
     * the specifed `highWatermark`.
     */
    def apply(highWatermark: Int): WatermarkRequestStrategy =
      WatermarkRequestStrategy(highWatermark, lowWatermark = math.max(1, highWatermark / 2))
  }

  /**
   * Requests up to the `highWatermark` when the `remainingRequested` is
   * below the `lowWatermark`. This a good strategy when the actor performs work itself.
   */
  case class WatermarkRequestStrategy(highWatermark: Int, lowWatermark: Int) extends RequestStrategy {
    def requestDemand(remainingRequested: Int): Int =
      if (remainingRequested < lowWatermark)
        highWatermark - remainingRequested
      else 0
  }

  /**
   * Requests up to the `max` and also takes the number of messages
   * that have been queued internally or delegated to other actors into account.
   * Concrete subclass must implement [[#i n F l i g h t I n t e r n a l l y]].
   * It will request elements in minimum batches of the defined [[#b a t c h S i z e]].
   */
  abstract class MaxInFlightRequestStrategy(max: Int) extends RequestStrategy {

    /**
     * Concrete subclass must implement this method to define how many
     * messages that are currently in progress or queued.
     */
    def inFlightInternally: Int

    /**
     * Elements will be requested in minimum batches of this size.
     * Default is 5. Subclass may override to define the batch size.
     */
    def batchSize: Int = 5

    override def requestDemand(remainingRequested: Int): Int = {
      val batch = math.min(batchSize, max)
      if ((remainingRequested + inFlightInternally) <= (max - batch))
        math.max(0, max - remainingRequested - inFlightInternally)
      else 0
    }
  }

}

/**
 * Extend/mixin this trait in your [[akka.actor.Actor]] to make it a
 * stream consumer with full control of stream back pressure. It will receive
 * [[ActorConsumer.OnNext]], [[ActorConsumer.OnComplete]] and [[ActorConsumer.OnError]]
 * messages from the stream. It can also receive other, non-stream messages, in
 * the same way as any actor.
 *
 * Attach the actor as a [[org.reactivestreams.api.Consumer]] to the stream with
 * [[ActorConsumer# a p p l y]].
 *
 * Subclass must define the [[RequestStrategy]] to control stream back pressure.
 * After each incoming message the `ActorConsumer` will automatically invoke
 * the [[RequestStrategy# r e q u e s t D e m a n d]] and propagate the returned demand to the stream.
 * The provided [[ActorConsumer.WatermarkRequestStrategy]] is a good strategy if the actor
 * performs work itself.
 * The provided [[ActorConsumer.MaxInFlightRequestStrategy]] is useful if messages are
 * queued internally or delegated to other actors.
 * You can also implement a custom [[RequestStrategy]] or call [[#r e q u e s t]] manually
 * together with [[ActorConsumer.ZeroRequestStrategy]] or some other strategy. In that case
 * you must also call [[#r e q u e s t]] when the actor is started or when it is ready, otherwise
 * it will not receive any elements.
 */
trait ActorConsumer extends Actor {

  import ActorConsumer._

  private val state = ActorConsumerState(context.system)
  private var subscription: Option[Subscription] = None
  private var requested = 0L
  private var canceled = false

  protected def requestStrategy: RequestStrategy

  override def aroundReceive(receive: Receive, msg: Any): Unit = msg match {
    case _: OnNext ⇒
      requested -= 1
      if (!canceled) {
        super.aroundReceive(receive, msg)
        request(requestStrategy.requestDemand(remainingRequested))
      }
    case OnSubscribe(sub) ⇒
      if (subscription.isEmpty) {
        subscription = Some(sub)
        if (canceled)
          sub.cancel()
        else if (requested != 0)
          sub.requestMore(remainingRequested)
      } else
        sub.cancel()
    case _: OnError ⇒
      if (!canceled) super.aroundReceive(receive, msg)
    case _ ⇒
      super.aroundReceive(receive, msg)
      request(requestStrategy.requestDemand(remainingRequested))
  }

  override def aroundPreStart(): Unit = {
    super.aroundPreStart()
    request(requestStrategy.requestDemand(remainingRequested))
  }

  override def aroundPostRestart(reason: Throwable): Unit = {
    state.get(self) foreach { s ⇒
      // restore previous state 
      subscription = s.subscription
      requested = s.requested
      canceled = s.canceled
    }
    state.remove(self)
    super.aroundPostRestart(reason)
    request(requestStrategy.requestDemand(remainingRequested))
  }

  override def aroundPreRestart(reason: Throwable, message: Option[Any]): Unit = {
    // some state must survive restart
    state.set(self, ActorConsumerState.State(subscription, requested, canceled))
    super.aroundPreRestart(reason, message)
  }

  override def aroundPostStop(): Unit = {
    state.remove(self)
    if (!canceled) subscription.foreach(_.cancel())
    super.aroundPostStop()
  }

  /**
   * Request a number of elements from upstream.
   */
  protected def request(elements: Int): Unit =
    if (elements > 0 && !canceled) {
      // if we don't have a subscription yet, it will be requested when it arrives
      subscription.foreach(_.requestMore(elements))
      requested += elements
    }

  /**
   * Cancel upstream subscription. No more elements will
   * be delivered after cancel.
   */
  protected def cancel(): Unit = {
    subscription.foreach(_.cancel())
    canceled = true
  }

  private def remainingRequested: Int = longToIntMax(requested)

  private def longToIntMax(n: Long): Int =
    if (n > Int.MaxValue) Int.MaxValue
    else n.toInt
}

/**
 * INTERNAL API
 */
case class ActorConsumerImpl[T](ref: ActorRef) extends Consumer[T] {
  override val getSubscriber: Subscriber[T] = new ActorSubscriber[T](ref)
}

/**
 * INTERNAL API
 */
final class ActorSubscriber[T](val impl: ActorRef) extends Subscriber[T] with Serializable {
  override def onError(cause: Throwable): Unit = impl ! ActorConsumer.OnError(cause)

  override def onComplete(): Unit = impl ! ActorConsumer.OnComplete

  override def onNext(element: T): Unit = impl ! ActorConsumer.OnNext(element)

  override def onSubscribe(subscription: Subscription): Unit = impl ! ActorConsumer.OnSubscribe(subscription)
}

/**
 * INTERNAL API
 * Some state must survive restarts.
 */
object ActorConsumerState extends ExtensionId[ActorConsumerState] with ExtensionIdProvider {
  override def get(system: ActorSystem): ActorConsumerState = super.get(system)

  override def lookup = ActorConsumerState

  override def createExtension(system: ExtendedActorSystem): ActorConsumerState =
    new ActorConsumerState

  case class State(subscription: Option[Subscription], requested: Long, canceled: Boolean)

}

/**
 * INTERNAL API
 */
class ActorConsumerState extends Extension {

  import ActorConsumerState.State

  private val state = new ConcurrentHashMap[ActorRef, State]

  def get(ref: ActorRef): Option[State] = Option(state.get(ref))

  def set(ref: ActorRef, s: State): Unit = state.put(ref, s)

  def remove(ref: ActorRef): Unit = state.remove(ref)
}
