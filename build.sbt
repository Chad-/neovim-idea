import sbt.Keys.{`package` => pack}

addCommandAlias("runPlugin", "; packager/package ; ideaRunner/run ")

addCommandAlias("buildPluginIdea", "; packager/package ; ideaBuilder/compile ")

lazy val root = (project in file("."))
  .enablePlugins(SbtIdeaPlugin)
  .settings(
    name := "neovim-idea",
    organization := "xyz.aoei",
    version := "0.1",
    scalaVersion := "2.11.8"
  )

lazy val ideaRunner = (project in file("subproject/ideaRunner"))
  .dependsOn(root % Provided)
  .settings(
    scalaVersion := "2.11.8",
    unmanagedJars in Compile := ideaMainJars.in(root).value,
    unmanagedJars in Compile += file(System.getProperty("java.home")).getParentFile / "lib" / "tools.jar",
    fork in run := true,
    mainClass in (Compile, run) := Some("com.intellij.idea.Main"),
    javaOptions in run ++= Seq(
      "-Xmx800m",
      "-XX:ReservedCodeCacheSize=64m",
      "-XX:MaxPermSize=250m",
      "-XX:+HeapDumpOnOutOfMemoryError",
      "-ea",
      "-Xdebug",
      "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005",
      "-Didea.is.internal=true",
      "-Didea.debug.mode=true",
      "-Dapple.laf.useScreenMenuBar=true",
      s"-Dplugin.path=${packagedPluginDir.value}",
      "-Didea.ProcessCanceledException=disabled"
    )
  )

lazy val ideaBuilder = (project in file("subproject/ideaBuilder"))
  .settings(
    mainClass in Compile := Some("com.intellij.idea.Main")
  )

lazy val packagedPluginDir = settingKey[File]("Path to packaged, but not yet compressed plugin")

packagedPluginDir in ThisBuild := baseDirectory.in(ThisBuild).value / "target" / name.in(root).value

lazy val packager: Project = (project in file("subproject/packager"))
  .settings(
    scalaVersion := "2.11.8",
    artifactPath := packagedPluginDir.value,
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
