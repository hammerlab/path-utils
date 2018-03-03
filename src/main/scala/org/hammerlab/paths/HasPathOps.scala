package org.hammerlab.paths

trait HasPathOps {
  implicit val makePathSymbolOps = HasPathOps.PathSymbolOps _
  implicit val makePathStringOps = HasPathOps.PathStringOps _
}

object HasPathOps {
  implicit class PathSymbolOps(val s: Symbol) extends AnyVal {
    def /(t: String): Path = Path(s.name) / t
    def /(t: Symbol): Path = Path(s.name) / t
  }
  implicit class PathStringOps(val s: String) extends AnyVal {
    def /(t: String): Path = Path(s) / t
    def /(t: Symbol): Path = Path(s) / t
  }
}
