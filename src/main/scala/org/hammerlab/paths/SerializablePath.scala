package org.hammerlab.paths

class SerializablePath
  extends Serializable {

  // Relative paths put their toString here, others put their uri.toString
  var str: String = _

  def readResolve: Any = Path(str)
}
