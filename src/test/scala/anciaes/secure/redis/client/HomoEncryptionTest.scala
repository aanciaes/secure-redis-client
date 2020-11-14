package anciaes.secure.redis.client

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.util.Random

class HomoEncryptionTest extends Simulation {

  private var counter = 0

  private val baseUrl = "https://localhost:8443"
  private val contentType = "application/json"
  private val endpoint = "/redis"

  private val authServerTokenUrl = "https://ns31249243.ip-51-210-0.eu:8678/auth/realms/thesis-realm/protocol/openid-connect/token"
  private val urlEncodedHeader = "application/x-www-form-urlencoded"

  // Insert data - Not relevant to this test
  private val numberOfSets = 100
  private val keySizeBytes = 200
  private val valueLimit = 100000
  //

  private val numberOfSums = 10
  private val numberOfMultiplications = 10
  private val sumLimit = 10000
  private val multiplicationLimit = 9

  private val keyPrefix = Random.alphanumeric.take(keySizeBytes).mkString
  private val randomSetDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("key" -> (s"$keyPrefix-" + increment()), "value" -> Random.nextInt(valueLimit).toString))
  private val randomSumDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("key" -> (s"$keyPrefix-" + (Random.nextInt(counter) + 1)), "sum" -> Random.nextInt(sumLimit).toString))
  private val randomMultiplicationDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("key" -> (s"$keyPrefix-" + (Random.nextInt(counter) + 1)), "mult" -> Random.nextInt(multiplicationLimit).toString))

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
    .repeat(numberOfSets) {
      feed(randomSetDataFeeder)
        .exec(
          http("Redis Set Requests")
            .post(endpoint)
            .body(ElFileBody("set-body.json")).asJson
            //.header("Authorization", """Bearer ${accessToken}""")
            .check(status is 201)
        )
    }
    .pause(10)
    .repeat(numberOfSums) {
      feed(randomSumDataFeeder)
        .exec(
          http("Redis Sum Requests")
            .put(endpoint + """/${key}/sum""").disableUrlEncoding
            //.header("Authorization", """"Bearer ${accessToken}"""")
            .queryParam("sum", """${sum}""")
            .check(status is 204)
        )
    }
    .pause(10)
    .repeat(numberOfMultiplications) {
      feed(randomMultiplicationDataFeeder)
        .exec(
          http("Redis Multiplication Requests")
            .put(endpoint + """/${key}/mult""").disableUrlEncoding
            //.header("Authorization", """"Bearer ${accessToken}"""")
            .queryParam("mult", """${mult}""")
            .check(status is 204)
        )
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)

  def increment(): Int = {
    counter += 1
    counter
  }
}
