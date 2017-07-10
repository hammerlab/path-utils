name := "paths"
deps ++= Seq(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(scalatest)
addScala212
