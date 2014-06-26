name := """stream-workshop"""

version := "1.0"

scalaVersion := "2.11.1"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "0.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.3.3",
  "org.boofcv" % "xuggler" % "0.16",
  "junit" % "junit" % "4.11",
  "org.scalatest" %% "scalatest" % "2.2.0"
)

resolvers += "xuggler-repo" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java"
