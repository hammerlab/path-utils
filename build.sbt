name := "paths"
version := "1.1.0-SNAPSHOT"
deps ++= Seq(
  libs.value('commons_io),
  libs.value('slf4j)
)
testDeps := Seq(scalatest.value)
addScala212
