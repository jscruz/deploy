package magenta

import org.scalatest.{Matchers, FlatSpec}
import java.io.File
import tasks._
import tasks.BlockFirewall
import tasks.CheckUrls
import tasks.CopyFile
import tasks.Service
import tasks.WaitForPort
import org.json4s._
import org.json4s.JsonDSL._
import magenta.deployment_type.ResinWebapp

class ResinWebappTest extends FlatSpec with Matchers {
  implicit val fakeKeyRing = KeyRing(SystemUser(None))

  "resin web app package type" should "have a deploy action" in {
    val p = DeploymentPackage("webapp", Seq.empty, Map.empty, "resin-webapp", new File("/tmp/packages/webapp"))

    val host = Host("host_name")

    ResinWebapp.perHostActions("deploy")(p)(host, fakeKeyRing) should be (List(
      BlockFirewall(host as "resin"),
      CopyFile(host as "resin", "/tmp/packages/webapp/", "/resin-apps/webapp/"),
      Service(host as "resin", "webapp"),
      WaitForPort(host, 8080, 1 * 60 * 1000),
      CheckUrls(host, 8080, List("/webapp/management/healthcheck"), 2 * 60 * 1000, 5),
      UnblockFirewall(host as "resin")
    ))
  }

  it should "allow port to be overriden" in {
    val basic = DeploymentPackage("webapp", Seq.empty, Map.empty, "resin-webapp", new File("/tmp/packages/webapp"))
    ResinWebapp.port(basic) should be (8080)

    val overridden = DeploymentPackage("webapp", Seq.empty, Map("port" -> "80"), "resin-webapp", new File("/tmp/packages/webapp"))
    ResinWebapp.port(overridden) should be (80)
  }

  it should "check urls when specified" in {
    val urls = List("/test", "/xx")
    val urls_json = JArray(urls map { JString(_)})

    val p = DeploymentPackage("webapp", Seq.empty, Map("healthcheck_paths" -> urls_json), "resin-webapp", new File("/tmp/packages/webapp"))
    val host = Host("host_name")

    ResinWebapp.perHostActions("deploy")(p)(host, fakeKeyRing) should be (List(
      BlockFirewall(host as "resin"),
      CopyFile(host as "resin", "/tmp/packages/webapp/", "/resin-apps/webapp/"),
      Service(host as "resin", "webapp"),
      WaitForPort(host, 8080, 1 * 60 * 1000),
      CheckUrls(host, 8080, urls, 2 * 60 * 1000, 5),
      UnblockFirewall(host as "resin")
    ))
  }

  it should "allow wait and check times to be overriden" in {
    val basic = DeploymentPackage("webapp", Seq.empty, Map.empty, "resin-webapp", new File("/tmp/packages/webapp"))
    ResinWebapp.waitseconds(basic) should be (60)
    ResinWebapp.checkseconds(basic) should be (120)

    val overridden = DeploymentPackage("webapp", Seq.empty, Map("waitseconds" -> 120, "checkseconds" -> 60), "resin-webapp", new File("/tmp/packages/webapp"))
    ResinWebapp.waitseconds(overridden) should be(120)
    ResinWebapp.checkseconds(overridden) should be(60)
  }


}
