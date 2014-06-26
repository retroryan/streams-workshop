package sample.start

object RunFileStreams {

  def main(args: Array[String]): Unit = {

    val chooseFlowTest: Int = args(0).toInt

    if (chooseFlowTest == 1)
      FileFlowAction.runBasicFlow(system => BasicFileProducer)
    else if (chooseFlowTest == 2)
      FileFlowAction.runComplexFlow(system => BasicFileProducer)


    else if (chooseFlowTest == 3)
      FileFlowAction.runBasicFlow(system => BasicActorProducer(system))
    else if (chooseFlowTest == 4)
      FileFlowAction.runComplexFlow(system => BasicActorProducer(system))

    System.out.println(s"Finished Processing")

  }


}

