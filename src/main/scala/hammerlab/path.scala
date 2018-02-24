package hammerlab

import org.hammerlab.paths
import org.hammerlab.paths.HasPathOps

object path
  extends HasPathOps {
  type Path = paths.Path
  val Path = paths.Path
  val FileSystems = paths.FileSystems
}
