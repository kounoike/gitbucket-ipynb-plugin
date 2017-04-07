val Organization = "io.github.kounoike"
val ProjectName = "gitbucket-ipynb-plugin"
val ProjectVersion = "0.1.1"

lazy val root = project in file(".")

organization := Organization
name := ProjectName
version := ProjectVersion
scalaVersion := "2.12.1"

libraryDependencies ++= Seq(
  "io.github.gitbucket" %% "gitbucket"          % "4.11.0" % "provided",
  "javax.servlet"        % "javax.servlet-api"  % "3.1.0"  % "provided",
  "org.json4s"           %% "json4s-jackson"     % "3.5.0"
)

scalacOptions := Seq("-deprecation", "-feature", "-language:postfixOps")
javacOptions in compile ++= Seq("-target", "8", "-source", "8")

useJCenter := true