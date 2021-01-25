
import play.sbt.PlayImport.PlayKeys
import play.sbt.routes.RoutesCompiler.autoImport._
import sbt.Keys._
import sbt._
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin._
import uk.gov.hmrc.versioning.SbtGitVersioning
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion
import uk.gov.hmrc.{SbtArtifactory, SbtAutoBuildPlugin}

val appName = "company-registration-frontend"

lazy val appDependencies: Seq[ModuleID] = AppDependencies()

lazy val scoverageSettings = {
  // Semicolon-separated list of regexs matching classes to exclude
  Seq(
    ScoverageKeys.coverageExcludedPackages := "<empty>;Reverse.*;models/.data/..*;view.*;config.*;poc.view.*;poc.config.*;.*(AuthService|BuildInfo|Routes).*",
    ScoverageKeys.coverageMinimum := 80,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true
  )
}

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin, SbtArtifactory): _*)
  .settings(scoverageSettings: _*)
  .settings(scalaSettings: _*)
  .settings(scalacOptions ++= Seq("-feature", "-unchecked", "-deprecation", "-language:reflectiveCalls"))
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .settings(
    libraryDependencies ++= appDependencies,
    fork in Test := true,
    retrieveManaged := true,
    routesImport ++= Seq("uk.gov.hmrc.play.binders._"),
    scalaVersion := "2.12.12",
    resolvers ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo)
  )
  .settings(PlayKeys.devSettings := Seq(
    "akka.http.parsing.max-uri-length" -> "16k")
  )
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(majorVersion := 2)
  .settings(
    Keys.fork in IntegrationTest := true,
    javaOptions in IntegrationTest += "-Dlogger.resource=logback-test.xml",
    parallelExecution in IntegrationTest := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

