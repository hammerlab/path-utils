name := "paths"
version := "1.2.0"
deps ++= Seq(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(scalatest)
addScala212
