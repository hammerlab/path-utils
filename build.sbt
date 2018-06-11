name := "paths"
v"1.5.1"
dep(
  case_app,
  commons.io,
  slf4j
)
testDeps := Seq(
  scalatest,
  log4j
)
github.repo("path-utils")
