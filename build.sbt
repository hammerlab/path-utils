name := "paths"
version := "1.1.1-SNAPSHOT"
deps ++= Seq(
  libs.value('commons_io),
  libs.value('slf4j),
  "com.github.alexarchambault" %% "case-app" % "1.2.0-M3"
)
testDeps := Seq(scalatest.value)
addScala212
