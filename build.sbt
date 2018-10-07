name := "paths"
v"1.6.0"
dep(
  case_app,
  commons.io,
  hammerlab.types % "1.4.0",
  slf4j
)
testDeps := Seq(
  scalatest,
  log4j
)
github.repo("path-utils")
