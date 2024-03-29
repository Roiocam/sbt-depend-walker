name := "sbt-depend-walker"
organization := "io.github.roiocam"
description := "sbt plugin for walk on the dependency tree of Build"

sbtPlugin := true

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-encoding",
  "UTF-8"
)

ThisBuild / scalaVersion := "2.12.18"

// start ----------- sbt-ci-release
inThisBuild(
  List(
    homepage := Some(url("https://github.com/roiocam/sbt-depend-walker")),
    // Alternatively License.Apache2 see https://github.com/sbt/librarymanagement/blob/develop/core/src/main/scala/sbt/librarymanagement/License.scala
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        id = "roiocam",
        name = "Andy chen",
        email = "iRoiocam@gmail.com",
        url = url("https://github.com/roiocam")
      )
    )
  )
)

sonatypeCredentialHost := "s01.oss.sonatype.org"
sonatypeRepository := "https://s01.oss.sonatype.org/service/local"
// end ----------- sbt-ci-release

// So that publishLocal doesn't continuously create new versions
def versionFmt(out: sbtdynver.GitDescribeOutput): String = {
  val snapshotSuffix =
    if (out.isSnapshot()) "-SNAPSHOT"
    else ""
  out.ref.dropPrefix + snapshotSuffix
}

def fallbackVersion(d: java.util.Date): String =
  s"HEAD-${sbtdynver.DynVer timestamp d}"

ThisBuild / version := dynverGitDescribeOutput.value.mkVersion(
  versionFmt,
  fallbackVersion(dynverCurrentDate.value)
)
ThisBuild / dynver := {
  val d = new java.util.Date
  sbtdynver.DynVer
    .getGitDescribeOutput(d)
    .mkVersion(versionFmt, fallbackVersion(d))
}

// start ----------- sbt-github-actions
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    name = Some("Build project"),
    commands = List("test", "scripted")
  )
)
ThisBuild / githubWorkflowTargetTags ++= Seq("v*")
// publish snapshot and release
ThisBuild / githubWorkflowPublishTargetBranches :=
  Seq(
    RefPredicate.Equals(Ref.Branch("main")),
    RefPredicate.StartsWith(Ref.Tag("v"))
  )
ThisBuild / githubWorkflowPublish := Seq(
  WorkflowStep.Sbt(
    commands = List("ci-release"),
    name = Some("Publish project"),
    env = Map(
      "PGP_PASSPHRASE" -> "${{ secrets.PGP_PASSPHRASE }}",
      "PGP_SECRET" -> "${{ secrets.PGP_SECRET }}",
      "SONATYPE_PASSWORD" -> "${{ secrets.SONATYPE_PASSWORD }}",
      "SONATYPE_USERNAME" -> "${{ secrets.SONATYPE_USERNAME }}"
    )
  )
)

ThisBuild / githubWorkflowOSes := Seq(
  "ubuntu-latest",
  "macos-latest",
  "windows-latest"
)

ThisBuild / githubWorkflowJavaVersions := Seq(
  JavaSpec.temurin("8"),
  JavaSpec.temurin("11"),
  JavaSpec.temurin("17")
)
// end ----------- sbt-github-actions

enablePlugins(SbtPlugin)
libraryDependencies ++= Dependencies.sbtDependWalker
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-Xlint",
  "-encoding",
  "UTF-8"
)
scriptedLaunchOpts += "-Xmx1024m"
scriptedLaunchOpts ++= Seq("-Dplugin.version=" + version.value)

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/roiocam/sbt-depend-walker"),
    "scm:git@github.com:roiocam/sbt-depend-walker.git"
  )
)

enablePlugins(SbtPlugin)
scriptedBufferLog := false
