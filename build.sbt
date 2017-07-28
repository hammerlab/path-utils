name := "paths"
version := "1.1.1-SNAPSHOT"
deps ++= Seq(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(scalatest)
addScala212
