name := "paths"
version := "1.3.1"
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
