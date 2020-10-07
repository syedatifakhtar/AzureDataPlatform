import com.syedatifakhtar.pipelines.Pipelines.Pipeline
import com.syedatifakhtar.scalaterraform.TerraformPipelines.TerraformStep
import com.syedatifakhtar.scalaterraform._
import com.typesafe.config._

import scala.util.Try


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

object TerraformHelper {

  object PlatformInfraConfig {
    val config: Config = ConfigFactory.load("conf/platform.conf")
  }


  def main(args: Array[String]): Unit = {

    val argsMap = ArgsParser.parse(args)
    println("Got args:\n")
    argsMap.foreach(println)
    val srcDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}"
    val buildDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}/build"


    def configValueResolver = {
      import scala.collection.JavaConverters._
      path: String =>
        Try {
          PlatformInfraConfig
            .config
            .getConfig(path)
            .entrySet()
            .asScala.map(x => (x.getKey, x.getValue.unwrapped.toString))
            .toMap
        }.toOption
    }

    val configTree = "azure-data-platform.infra"

    def configResolverBuilder =
      DefaultConfigArgsResolver(configValueResolver)(configTree) _

    def registerPipelines(pipelines: Pipeline*): Map[String, Pipeline] = {
      pipelines.map { p => p.name -> p }.toMap
    }

    val command = argsMap(ArgsParser.TASKNAME)
    val partiallyAppliedModule = TerraformModule(srcDir, buildDir) _
    val accountStep = TerraformStep(partiallyAppliedModule("account")(configResolverBuilder("account"))) _
    val environmentStep = TerraformStep(partiallyAppliedModule("environment")(configResolverBuilder("environment"))) _
    val platformStep = TerraformStep(partiallyAppliedModule("platform")(configResolverBuilder("platform"))) _
    val pipeline = TerraformPipelines
      .TerraformPipeline
      .empty("all", command) ->
      accountStep ->
      environmentStep ->
      platformStep

    val pipelinesAvailable = registerPipelines(pipeline)

    pipelinesAvailable(argsMap(ArgsParser.PIPELINENAME)).execute


  }

}
