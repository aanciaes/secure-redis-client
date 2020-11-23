package anciaes.secure.redis.client

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration.DurationInt

class SearchHomoEncryptionTest extends Simulation {

  private val baseUrl = "https://ns31249243.ip-51-210-0.eu:8777"
  // private val baseUrl = "https://localhost:8443"
  private val contentType = "application/json"
  private val endpoint = "/redis"

  private val authServerTokenUrl = "https://ns31249243.ip-51-210-0.eu:8678/auth/realms/thesis-realm/protocol/openid-connect/token"
  private val urlEncodedHeader = "application/x-www-form-urlencoded"

  private val insertTimes = 1

  private val searchDuration = 5
  private val searchTermFeeder = Array(
    Map("search" -> "5n8DSM"),
    Map("search" -> "Fc71258aXJ70p"),
    Map("search" -> "LeZDc86V0Uf7AUCvU4i0BBA2XSMI7lvj5jtV17ov5d2221999949r"),
    Map("search" -> "wZ8ZS3P7RZ3y5p8U7mzhPibVQxk3cMcurmPg89mkI0kGM143s3449"),
    Map("search" -> "3cp1qg5n0HO2NS9R3RS0we"),
    Map("search" -> "Ee9hyMx9Z,Nw0IqP7iTi3BdG3Gq4dQ"),
    Map("search" -> "97621IEAwIMjA9GfvP3lcd10HRd815swM279J451U87z84240f60567N91YCIlK4oVBNDa8sWGH6i8A59fjhIFK4y"),
    Map("search" -> "dnwv20X"),
    Map("search" -> "eK8eW9BGj2MOBiQ9s710gy7uGxKaHMNSe5DB18"),
    Map("search" -> "5W4FX75sUHO05afvJnEHDt6p"),
    Map("search" -> "eTJ3Y54G56949w7291Rks0cSNFjxm93xN1jZort0p"),
    Map("search" -> "qQ2Zg6WpO0mb0CNS7DlCM8JMW3d9w3417L08TpT5nz3747l32Qk1Y9vx9sh8eAxesp412YS"),
    Map("search" -> "xdtxv3Ld9iFlw9v4O6O1GlN2XUG1PndQ891o2UmwHueY3C1"),
    Map("search" -> "cA8gXGSHNw9882yS0WX5UKzL2uk,C8"),
    Map("search" -> "682iZmA1F3cp"),
    Map("search" -> "yHz0ka8NG0C1P3800Wp0E2G8pwa3H17i6aXro2p8giei0x"),
    Map("search" -> "5MmVDvhN692P"),
    Map("search" -> "45n8DSM36c21nar7DXe2RYu8QU5FtG99DP1UMF69S2x161zo7N43d2sumd"),
    Map("search" -> "8GS2148B1D13OzyBmfr50Pwt2Mb2Zzt89daj1Ts7UoS6kdLC6uV3xm67"),
    Map("search" -> "uH5hy54GK1p5tjRSA635iUx4Jv4u"),
    Map("search" -> "qg5n0HO2N,1Vi0HqS3Hh8w7Cba28089Ov2"),
    Map("search" -> "1Q1E"),
  ).circular

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .contentTypeHeader(contentType)
    .acceptHeader("*/*")
    .userAgentHeader("User-Agent: Gatling/2.0")
    .disableCaching

  val scn: ScenarioBuilder = scenario("Redis Homomorphic Encryption Tests")
    .exec(
      http("Auth Server Login")
        .post(authServerTokenUrl)
        .header("Content-Type", urlEncodedHeader)
        .formParam("grant_type", "password")
        .formParam("username", "anciaes")
        .formParam("password", "C9miKE*dUAtqKHPYEGZPm9!c9")
        .formParam("client_id", "thesis-redis-client")
        .asFormUrlEncoded
        .check(status is 200)
        .check(jmesPath("access_token").saveAs("accessToken"))
    )
    //.during(60) {
    .repeat(insertTimes) {
        exec(
          http("Redis Sadd Request")
            .post(endpoint + "/sadd")
            .body(ElFileBody("zadd-body.json")).asJson
            .header("Authorization", """Bearer ${accessToken}""")
            .check(status is 201)
            .check(jmesPath("key").saveAs("key"))
        )
    }
    .pause(10)
    //.repeat(searchTimes) {
    .during(searchDuration minutes) {
      feed(searchTermFeeder)
        .exec(
          http("Redis Search Requests")
            .get(endpoint + """/sadd/${key}""").disableUrlEncoding
            .header("Authorization", """Bearer ${accessToken}""")
            .queryParam("search", """${search}""")
            .check(status is 200)
        )
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
