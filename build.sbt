val Organization = "io.github.kounoike"
val ProjectName = "gitbucket-ipynb-plugin"
val ProjectVersion = "0.3.1"

lazy val root = project in file(".")

organization := Organization
name := ProjectName
version := ProjectVersion
scalaVersion := "2.12.4"
gitbucketVersion := "4.26.0"
