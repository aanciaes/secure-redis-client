# Secure Redis Client

Simulated client of the secure redis proxy. This client is a tester built with [gatling](https://gatling.io) framework and written in Scala to analise performance metrics of [Secure Redis Proxy](https://github.com/aanciaes/secure-redis-proxy) and [Secure Redis Container](https://github.com/aanciaes/secure-redis-container) thesis different configurations.

## Run Performance Tests

On terminal run `sbt gatling:test`

or

On intelliJ tun task `gatling:test`

## Check Report

After running the command above, gatling will output the location of the report on the filesystem. For example `Please open the following file: /(...)/secure-redis-client/target/gatling/secureredisclient-20201015205906582/index.html`

Open a browser and navigate to `file:///path/to/gatling/report.html`
