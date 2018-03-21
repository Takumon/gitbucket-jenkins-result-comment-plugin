name := "gitbucket-helloworld-plugin"
organization := "io.github.gitbucket"
version := "1.0.0"
scalaVersion := "2.12.4"
gitbucketVersion := "4.20.0"

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"          % "4.20.0"  % "provided",
  "com.typesafe.play"   %% "twirl-compiler"     % "1.3.0"  % "provided",
  "javax.servlet"        % "javax.servlet-api"  % "3.1.0"  % "provided"
)

useJCenter := true