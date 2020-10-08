package com.syedatifakhtar.dataplatform

import java.io.{File, PrintWriter}

import com.syedatifakhtar.pipelines.Pipelines.{MultiSequencePipeline, Pipeline, UnitStep}
import com.syedatifakhtar.scalaterraform.TerraformPipelines.TerraformStep
import com.syedatifakhtar.scalaterraform.{DefaultConfigArgsResolver, TerraformModule, TerraformPipelines}
import com.typesafe.config.{Config, ConfigFactory}

import scala.sys.process.Process
import scala.util.Try

object PipelineBuilder {

  object PlatformInfraConfig {
    val config: Config = ConfigFactory.load("conf/platform.conf")
  }


  private def configValueResolver = {
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
  private val configTree = "azure-data-platform.infra"

  private def configResolverBuilder = DefaultConfigArgsResolver(configValueResolver)(configTree) _

  val srcDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}"
  val buildDir = s"${this.getClass.getClassLoader().getResource("terraform").getPath}/build"
  val kubeConfigDir = s"${buildDir}/credentials"
  val kubeConfigPath = s"${kubeConfigDir}/kubeconfig.conf"
  val kubeServicesDir = s"${this.getClass.getClassLoader().getResource("terraform/platform/services").getPath}"

  private def buildPipelineMap(pipelines: Pipeline*): Map[String, Pipeline] = {
    pipelines.map { p => p.name -> p }.toMap
  }

  def getModule(moduleName: String) = {
    TerraformModule(srcDir, buildDir)(moduleName)(configResolverBuilder(moduleName))
  }

  def getPipelines(command: String): Map[String, Pipeline] = {
    val accountStep = TerraformStep(getModule("account")) _
    val environmentStep = TerraformStep(getModule("environment")) _
    val platformModule = getModule("platform")
    val platformStep = TerraformStep(platformModule) _
    val infraPipeline = TerraformPipelines
      .TerraformPipeline
      .empty("all_infra", command) ->
      accountStep ->
      environmentStep ->
      platformStep

    val deploy_kube_apps_pipeline =
      Pipeline.empty("deploy_kube_apps") ->
        UnitStep("Sync Kube Config") { _ =>
          val output = platformModule.output
          val kubeConfig = s"${output.get("kube-config")}"
          println(s"Writing kube config to : $kubeConfigDir")
          val directory = new File(kubeConfigDir)
          if (!directory.exists()) {
            directory.mkdirs()
          }
          new PrintWriter(kubeConfigPath) {
            write(kubeConfig);
            close()
          }
          println(s"Kube Config from Terraform: $kubeConfig")
          output.get
        } ->
        UnitStep("Deploy Atlas") { _ =>

          Process(Seq("bash", "-c", s"kubectl --kubeconfig ${kubeConfigDir}/kubeconfig.conf apply -f atlas/pod.yaml --force"), Some(new File(s"${kubeServicesDir}"))).!!
          val output = Process(Seq("bash", "-c", s"kubectl --kubeconfig ${kubeConfigDir}/kubeconfig.conf get service atlas-service"), Some(new File(s"${kubeServicesDir}"))).!!
          println(s"Atlas deploy output: ${output}")
          Map.empty[String, String]
        }
    val multiPipeline = MultiSequencePipeline.empty("build_all") ->
      infraPipeline ->
      deploy_kube_apps_pipeline
    buildPipelineMap(infraPipeline, multiPipeline,deploy_kube_apps_pipeline)
  }

}
