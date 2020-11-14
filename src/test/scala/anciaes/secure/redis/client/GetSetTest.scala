package anciaes.secure.redis.client

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.concurrent.duration._
import scala.util.Random

class GetSetTest extends Simulation {

  private var counter = 0

  private val baseUrl = "https://localhost:8443"
  private val contentType = "application/json"
  private val endpoint = "/redis"

  private val authServerTokenUrl = "https://ns31249243.ip-51-210-0.eu:8678/auth/realms/thesis-realm/protocol/openid-connect/token"
  private val urlEncodedHeader = "application/x-www-form-urlencoded"

  private val numberOfSets = 100000
  private val numberOfGets = 100000
  private val keySizeBytes = 200
  private val valueSizeBytes = 1000
  private val keyPrefix = Random.alphanumeric.take(keySizeBytes).mkString
  private val randomSetDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("key" -> (s"$keyPrefix-" + increment()), "value" -> Random.alphanumeric.take(valueSizeBytes).mkString))
  private val randomGetDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("key" -> (s"$keyPrefix-" + (Random.nextInt(counter) + 1))))

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
    .repeat(numberOfSets) {
      // feed(jsonFileFeeder.queue)
      feed(randomSetDataFeeder)
        .exec(
          http("Redis Set Requests")
            .post(endpoint)
            .body(ElFileBody("set-body.json")).asJson
            //.header("Authorization", """Bearer ${accessToken}""")
            .check(status is 201)
        )
    }.pause(5 seconds)
    .repeat(numberOfGets) {
      feed(randomGetDataFeeder)
        .exec(
          http("Redis Get Requests")
            .get(endpoint + """/${key}""").disableUrlEncoding
            //.header("Authorization", """"Bearer ${accessToken}"""")
            .check(status is 200)
        )
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

  def increment(): Int = {
    counter += 1
    counter
  }
}
