package anciaes.secure.redis.client

import io.gatling.core.Predef._
import io.gatling.core.feeder.FileBasedFeederBuilder
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._

class SecureRedisClient extends Simulation {

  private val baseUrl = "https://localhost:8443"
  private val contentType = "application/json"
  private val endpoint = "/redis"

  private val authServerTokenUrl = "https://ns31249243.ip-51-210-0.eu:8678/auth/realms/thesis-realm/protocol/openid-connect/token"
  private val urlEncodedHeader = "application/x-www-form-urlencoded"

  val jsonFileFeeder: FileBasedFeederBuilder[Any] = jsonFile("mock-data.json")

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .contentTypeHeader(contentType)
    .acceptHeader("*/*")
    .userAgentHeader("User-Agent: Gatling/2.0")
    .disableCaching

  val scn: ScenarioBuilder = scenario("Redis Secure Proxy Tests")
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
    .repeat(3) {
      feed(jsonFileFeeder.queue)
        .exec(
          http("Redis Set Requests")
            .post(endpoint)
            .body(ElFileBody("set-body.json")).asJson
            //.header("Authorization", """Bearer ${accessToken}""")
            .check(status is 201)
        ).pause(5 milliseconds)
    }.pause(5)
    .repeat(10) {
      feed(jsonFileFeeder.random)
        .exec(
          http("Redis Get Requests")
            .get(endpoint + """/${key}""").disableUrlEncoding
            //.header("Authorization", """"Bearer ${accessToken}"""")
            .check(status is 200)
        ).pause(5 milliseconds)
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}