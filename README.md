# Secure Redis Client

![GitHub MasterThesis](https://img.shields.io/badge/MasterThesis-Delivered-brightgreen)
[![GitHub release](https://img.shields.io/github/release/aanciaes/secure-redis-client.svg)](https://github.com/aanciaes/secure-redis-client/releases/)
[![GitHub issues](https://img.shields.io/github/issues/aanciaes/secure-redis-client.svg)](https://github.com/aanciaes/secure-redis-client/issues/)

Simulated client of the secure redis proxy. This client is a tester built with [gatling](https://gatling.io) framework and written in Scala to analise performance metrics of [Secure Redis Proxy](https://github.com/aanciaes/secure-redis-proxy) and [Secure Redis Container](https://github.com/aanciaes/secure-redis-container) thesis different configurations.

## Run Performance Tests

On terminal run `sbt gatling:test` or `sbt "gatling:testOnly anciaes.secure.redis.client.SearchHomoEncryptionTest"`

or

On intelliJ run task `gatling:test` or `gatling:testOnly anciaes.secure.redis.client.SearchHomoEncryptionTest`

## Check Report

After running the command above, gatling will output the location of the report on the filesystem. For example `Please open the following file: /(...)/secure-redis-client/target/gatling/secureredisclient-20201015205906582/index.html`

Open a browser and navigate to `file:///path/to/gatling/report.html`

## Docker Stats Recorder

Bash script that records docker container stats:

```
usage: $ docker-stats-recorder -c <container_id> [options]
options:
    -h                                Show help (this screen)
    -c <container_id>,<container_id>  Indicate container to record
    -o <output_dir>                   Indicate directory where stats will be stored
    -i <interval seconds>             Interval between samples
    -s <number of samples>            Number of samples to recover. Set -1 to infinite
    -r <user:password@host:port>      Redis host to recover memory information
```

## Docker StartUp Recorder

Bash script that records a docker container startup time:

```
usage: $ docker-startup-time-recorder -i <image> -u <health_url> [options]
options:
    -h                              Show help (this screen)
    -m                              Set -m option if running on macos
    -n <name>                       Container name
    -p <port:port>                  Container ports. Repeatable
    -e <environment>                Container environment variable. Repeatable
    -d <device>                     Container device mount
    -i <image:tag>                  Container image to run
    -u <protocol://host:port/url>   Service health url
```
