import com.syedatifakhtar.scalaterraform.DestroyArguments.DestroyArgument
import com.syedatifakhtar.scalaterraform.InitArguments.{BackendConfigs, HasBackend, InitArgument}
import com.syedatifakhtar.scalaterraform.PlanAndApplyArguments.{ApplyArgument, PlanArgument, Vars}
import com.syedatifakhtar.scalaterraform._
import com.typesafe.config._

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}


object TerraformHelper {


  object PlatformInfraConfig {
    val config: Config = ConfigFactory.load("platform.conf")
  }

  case class ConfigArgsResolver(nameInConfig: String) extends ArgsResolver {
    def toConfigMap(conf: Config) = {
      conf.entrySet().asScala.map(x => (x.getKey, x.getValue.render())).toMap
    }
    override def getInitArgs(): Seq[InitArgument] = {
      val keyName = s"azure-data-platform.infra.${nameInConfig}.backend-config"
      val backendConfig = if
      (PlatformInfraConfig.config.hasPath(keyName))
        Seq(HasBackend(), BackendConfigs(toConfigMap(PlatformInfraConfig.config.getConfig(keyName))))
      else Seq.empty
      backendConfig
    }
    override def getPlanArgs(): Seq[PlanArgument] = {
      val vars = toConfigMap(PlatformInfraConfig.config.getConfig(s"azure-data-platform.infra.${nameInConfig}.vars"))
      Seq(Vars(vars))
    }
    override def getApplyArgs(): Seq[ApplyArgument] = {
      val vars = toConfigMap(PlatformInfraConfig.config.getConfig(s"azure-data-platform.infra.${nameInConfig}.vars"))
      Seq(Vars(vars))
    }
    override def getDestroyArgs(): Seq[DestroyArgument] = {
      val vars = toConfigMap(PlatformInfraConfig.config.getConfig(s"azure-data-platform.infra.${nameInConfig}.vars"))
      Seq(DestroyArguments.Vars(vars))
    }
  }


  def main(args: Array[String]): Unit = {

    println("Base dir: " + this.getClass.getClassLoader().getResource("").getPath())
    val srcDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}/platform"
    val buildDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}/build"

    def platformApply = {
      val platform = TerraformModule(srcDir,
        buildDir,
        "platform")(ConfigArgsResolver("platform"))
      for {_ <- platform.init
           out <- platform.output} yield {
        out.toString()
      }
    }

    val methodMap: Map[String, () => Try[Any]] = Map(
      ("platformApply" -> platformApply _))
    methodMap(args(0))() match {
      case Failure(e) => e.printStackTrace()
      case Success(value) => println(s"Output: $value")
    }

  }

}
