name := "paths"
version := "1.3.2-SNAPSHOT"
deps ++= Seq(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(
  scalatest,
  log4j
)
addScala212
