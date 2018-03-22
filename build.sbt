organization := "takumon"
name := "gitbucket-jenkins-result-comment-plugin"
version := "0.0.1"
scalaVersion := "2.12.4"
gitbucketVersion := "4.20.0"

// lazy val root = (project in file(".")).enablePlugins(SbtTwirl)

useJCenter := true

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"          % "4.20.0"  % "provided",
  "com.typesafe.play"   %% "twirl-compiler"     % "1.3.0"  % "provided",
  "javax.servlet"        % "javax.servlet-api"  % "3.1.0"  % "provided"
)

