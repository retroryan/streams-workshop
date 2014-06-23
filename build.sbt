name := """stream-workshop"""

version := "1.0"

scalaVersion := "2.10.3"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "0.2",
  "xuggle" % "xuggle-xuggler" % "5.2" from "http://xuggle.googlecode.com/svn/trunk/repo/share/java/xuggle/xuggle-xuggler/5.2/xuggle-xuggler-5.2.jar",
  "org.boofcv" % "xuggler" % "0.16" 
)

