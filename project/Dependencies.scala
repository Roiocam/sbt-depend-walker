import sbt._

object Library {

  // Versions
  val bndVersion = "6.4.1"
  val specs2Version = "4.20.5"

  // Libraries
  val bndLib = "biz.aQute.bnd" % "biz.aQute.bndlib" % bndVersion
  val specs2 = "org.specs2" %% "specs2-core" % specs2Version
}

object Dependencies {

  import Library._

  val sbtDependWalker = List(specs2 % Test)
}