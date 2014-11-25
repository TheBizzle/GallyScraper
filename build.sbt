val root = project in file (".")

name := "GallyScraper"

organization := "org.bizzle"

licenses += ("BSD-3", url("http://opensource.org/licenses/bsd-3-clause"))

version := "0.1"

scalaVersion := "2.11.4"

scalacOptions ++= "-deprecation -unchecked -feature -Xcheckinit -encoding us-ascii -target:jvm-1.7 -Xlint -Xfatal-warnings -language:_".split(" ").toSeq

// only log problems plz
ivyLoggingLevel := UpdateLogging.Quiet

libraryDependencies ++= Seq(
  "io.argonaut" %% "argonaut" % "6.0.4"
)

onLoadMessage := ""

logBuffered in testOnly in Test := false
