package org.hammerlab.paths

import java.nio.{ channels, file }
import java.nio.file.OpenOption
import java.nio.file.attribute.FileAttribute
import java.util

import com.sun.nio.zipfs

class JarFileSystemProvider
  extends zipfs.JarFileSystemProvider {
  override def newByteChannel(path: file.Path,
                              set: util.Set[_ <: OpenOption],
                              fileAttributes: FileAttribute[_]*): channels.SeekableByteChannel =
    newFileChannel(path, set, fileAttributes: _*)
}
