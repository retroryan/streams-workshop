package video

import akka.actor.ActorSystem


object DisplayProperties {
  def getWidth(system: ActorSystem) = system.settings.config.getInt("width")

  def getHeight(system: ActorSystem) = system.settings.config.getInt("height")

}
