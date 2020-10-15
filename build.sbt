name := "secure-redis-client"

version := "0.1"

scalaVersion := "2.12.3"

enablePlugins(GatlingPlugin)

libraryDependencies ++= Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % "3.2.1" % "test",
  "io.gatling"            % "gatling-test-framework"    % "3.2.1" % "test"
)
