import sbt.Keys.{`package` => pack}

lazy val root = (project in file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    name := "neovim-idea",
    organization := "xyz.aoei",
    version := "0.1",
    scalaVersion := "2.11.8",

    libraryDependencies += "xyz.aoei" %% "neovim-scala" % "1.1"
  )

lazy val packager: Project = (project in file("subproject/packager"))
  .settings(
    scalaVersion := "2.11.8",
    artifactPath := (baseDirectory.in(ThisBuild).value / "target" / name.in(root).value),
    dependencyClasspath <<= {
      dependencyClasspath in (root, Compile)
    },
    mappings := {
      import Packaging.PackageEntry._
      val lib = Seq(
        Artifact(pack.in(root, Compile).value, s"lib/${name.in(root).value}.jar")
      )

      Packaging.convertEntriesToMappings(lib, dependencyClasspath.value)
    },
    pack := {
      Packaging.packagePlugin(mappings.value, artifactPath.value)
      artifactPath.value
    }
)
