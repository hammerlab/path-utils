name := "paths"
r"1.4.0"
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
