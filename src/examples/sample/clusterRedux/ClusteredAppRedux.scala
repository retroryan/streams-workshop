package sample.clusterRedux

object ClusteredAppRedux {

  def main(args: Array[String]): Unit = {
    // starting 1 frontend nodes and 1 backend nodes for testing
    ServerVideoRedux.main(Seq("2551").toArray)
    Thread.sleep(500)

    ClientVideoRedux.main(Seq("2552").toArray)
  }

}
