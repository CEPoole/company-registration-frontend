import sbt._

object FrontendBuild extends Build with MicroService {
  import sbt.Keys._

  val appName = "company-registration-frontend"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()

  override lazy val playSettings : Seq[Setting[_]] = Seq(
    dependencyOverrides += "org.scala-lang" % "scala-library" % "2.11.8",
    dependencyOverrides += "uk.gov.hmrc" %% "domain" % "5.3.0",
    dependencyOverrides += "uk.gov.hmrc" %% "secure" % "7.0.0",
    dependencyOverrides += "io.netty" % "netty" % "3.9.8.Final",
    dependencyOverrides += "com.typesafe.play" % "twirl-api_2.11" % "1.1.1"
  )
}

private object AppDependencies {
  import play.core.PlayVersion
  import play.sbt.PlayImport.ws

  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "bootstrap-play-25" % "4.9.0",
    "uk.gov.hmrc" %% "auth-client" % "2.20.0-play-25",
    "uk.gov.hmrc" %% "play-partials" % "6.5.0",
    "uk.gov.hmrc" %% "url-builder" % "2.1.0",
    "uk.gov.hmrc" %% "http-caching-client" % "8.1.0",
    "uk.gov.hmrc" %% "play-conditional-form-mapping" % "0.2.0",
    "org.bitbucket.b_c" % "jose4j" % "0.5.0",
    "uk.gov.hmrc" %% "time" % "3.3.0",
    "uk.gov.hmrc" %% "play-whitelist-filter" % "2.0.0",
    "commons-validator" % "commons-validator" % "1.6",
    "uk.gov.hmrc" %% "play-language" % "3.4.0",
    "uk.gov.hmrc" %% "play-reactivemongo" % "6.4.0",
    "uk.gov.hmrc" %% "govuk-template" % "5.27.0-play-25",
    "uk.gov.hmrc" %% "play-ui" % "7.33.0-play-25"
  )

  def defaultTest(scope: String) = Seq(
      "org.scalatest" %% "scalatest" % "3.0.1" % scope,
      "org.scalatestplus.play" %% "scalatestplus-play" % "2.0.0" % scope,
      "org.pegdown" % "pegdown" % "1.6.0" % scope,
      "org.jsoup" % "jsoup" % "1.10.2" % scope,
      "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
      "uk.gov.hmrc" %% "hmrctest" % "3.6.0-play-25" % scope,
      "org.mockito" % "mockito-all" % "2.0.2-beta" % scope
  )

  object Test {
    def apply() = defaultTest("test")
  }

  object IntegrationTest {
    def apply() = defaultTest("it") ++ Seq(
      "com.github.tomakehurst" % "wiremock" % "2.6.0" % "it"
    )
  }

  def apply() = compile ++ Test() ++ IntegrationTest()
}
