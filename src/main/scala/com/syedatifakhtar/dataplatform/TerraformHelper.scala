package com.syedatifakhtar.dataplatform


object ArgsParser {
  def parse(args: Array[String]) = {
    args.map {
      arg =>
        (arg.split("--")(1).split("=")(0), arg.split("--")(1).split("=")(1))
    }.toMap
  }
  val PIPELINENAME = "pipelineName"
  val TASKNAME = "taskName"
}

object Application {


  def main(args: Array[String]): Unit = {
    val argsMap = ArgsParser.parse(args)
    println("Got args:\n")
    argsMap.foreach(println)
    val pipelinesAvailable = PipelineBuilder.getPipelines(argsMap(ArgsParser.TASKNAME))
    pipelinesAvailable(argsMap(ArgsParser.PIPELINENAME)).execute.get
  }

}
