name := "fileSyncr"
version := "0.1"
scalaVersion := "2.12.6"

resolvers += "4thline-repo" at "http://4thline.org/m2"

val betterFilesVersion = "3.5.0"

libraryDependencies += "com.typesafe.akka" %% "akka-remote" % "2.5.14"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.14"
libraryDependencies += "com.typesafe.akka" %% "akka-testkit" % "2.5.14" % Test
libraryDependencies += "com.github.wumpz" % "diffutils" % "2.2"

//libraryDependencies += "org.fourthline.cling" % "cling-core" % "2.1.2"
libraryDependencies += "org.fourthline.cling" % "cling-support" % "2.1.2"


libraryDependencies += "com.github.pathikrit" %% "better-files" % betterFilesVersion
libraryDependencies ++= Seq(
  "com.github.pathikrit" %% "better-files-akka" % betterFilesVersion,
  "com.typesafe.akka" %% "akka-actor" % "2.5.13"
)
//libraryDependencies += "org.scalactic" %% "scalactic" % "3.0.5"
libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % "test"
//libraryDependencies += "com.github.romix.akka" %% "akka-kyro-serialization" % "0.5.1"

test in assembly := {}
assemblyMergeStrategy in assembly := {
  case PathList("META-INF", xs@_*) => MergeStrategy.discard
  case PathList("reference.conf") => MergeStrategy.concat
  case x => MergeStrategy.deduplicate
}