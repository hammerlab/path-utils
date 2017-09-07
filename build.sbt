name := "paths"
version := "1.3.0-SNAPSHOT"
deps ++= Seq(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(scalatest)
addScala212
