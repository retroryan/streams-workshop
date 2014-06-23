name := """stream-workshop"""

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "0.2",
  "org.boofcv" % "xuggler" % "0.16"
)

resolvers += "xuggler-repo" at "http://xuggle.googlecode.com/svn/trunk/repo/share/java"
