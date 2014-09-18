import sbt.Keys._
import sbt._

object MyBuild extends Build {

  lazy val copyDependencies = TaskKey[Unit]("copy-dependencies")

  def copyDepTask = copyDependencies <<= (update, crossTarget, scalaVersion) map {
    (updateReport, out, scalaVer) =>
      updateReport.allFiles foreach { srcPath =>
        val destPath = out / "lib" / srcPath.getName
        IO.copyFile(srcPath, destPath, preserveLastModified=true)
      }
  }

  lazy val root = Project(
    "root",
    file("."),
    settings = Defaults.defaultSettings ++ Seq(
      copyDepTask
    )
  )
}