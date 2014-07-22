package sample.clusterRedux

object ClusteredMessagesRedux {

  case object BackendRegistration

  case class StartVideo(consumerActorName: String)

}
