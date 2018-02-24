name := "paths"
v"1.5.0"
dep(
  case_app,
  commons_io,
  slf4j
)
testDeps := Seq(
  scalatest,
  log4j
)
github.repo("path-utils")
