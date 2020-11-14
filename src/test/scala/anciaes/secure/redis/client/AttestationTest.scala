package anciaes.secure.redis.client

import io.gatling.core.Predef._
import io.gatling.core.structure.ScenarioBuilder
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.util.Random

class AttestationTest extends Simulation{

  private val baseUrl = "https://localhost:8443"
  private val contentType = "application/json"
  private val endpoint = "/redis"

  private val authServerTokenUrl = "https://ns31249243.ip-51-210-0.eu:8678/auth/realms/thesis-realm/protocol/openid-connect/token"
  private val urlEncodedHeader = "application/x-www-form-urlencoded"

  private val numberOfAttestationRequest = 100

  private val randomNonceDataFeeder: Iterator[Map[String, String]] = Iterator.continually(Map("nonce" -> Random.nextInt(100000000).toString))

  val httpProtocol: HttpProtocolBuilder = http
    .baseUrl(baseUrl)
    .contentTypeHeader(contentType)
    .acceptHeader("*/*")
    .userAgentHeader("User-Agent: Gatling/2.0")
    .disableCaching

  val scn: ScenarioBuilder = scenario("Redis Attestation Tests")
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
    .repeat(numberOfAttestationRequest) {
      feed(randomNonceDataFeeder)
        .exec(
          http("Proxy Attestation Requests")
            .get("/attest").disableUrlEncoding
            //.header("Authorization", """"Bearer ${accessToken}"""")
            .queryParam("nonce", """${nonce}""")
            .check(status is 200)
        )
    }

  setUp(scn.inject(atOnceUsers(1))).protocols(httpProtocol)
}
