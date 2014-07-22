package sample.clustered

object ClusteredApp {

  /**
   *
   * run:
   *      ./activator -Dconfig.resource=clustering.conf 'runMain sample.clustered.ClusteredApp'
   */
  def main(args: Array[String]): Unit = {
    // starting 1 frontend nodes and 1 backend nodes for testing
    ServerProduceVideo.main(Seq("2551").toArray)
    Thread.sleep(500)

    ClientShowVideo.main(Seq("2552").toArray)
  }

}
