ThisBuild / scalaVersion := "3.8.4"

lazy val osName = System.getProperty("os.name") match {
  case name if name.startsWith("Linux")   => "linux"
  case name if name.startsWith("Mac")     => "mac"
  case name if name.startsWith("Windows") => "win"
  case name => throw new Exception(s"Unsupported operating system: $name")
}

lazy val root = (project in file("."))
  .settings(
    name := "Project_23079692",

    libraryDependencies ++= Seq(
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "org.openjfx" % "javafx-base" % "21" classifier osName,
      "org.openjfx" % "javafx-controls" % "21" classifier osName,
      "org.openjfx" % "javafx-graphics" % "21" classifier osName
    ),

    scalacOptions ++= Seq(
      "-Wunused:all",
      "-feature",
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s,cat=deprecation&msg=method handle:s"
    ),

    Compile / run / fork := true
  )