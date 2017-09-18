package org.hammerlab.paths

import java.nio.{ channels, file }
import java.nio.file.OpenOption
import java.nio.file.attribute.FileAttribute
import java.util

import com.sun.nio.zipfs

/**
 * Thin override of [[zipfs.JarFileSystemProvider]] that defines `newByteChannel` in terms of `newFileChannel`.
 *
 * The former is (inexplicably?) missing from the default implementation, and throws a
 * [[UnsupportedOperationException]].
 */
class JarFileSystemProvider
  extends zipfs.JarFileSystemProvider {
  override def newByteChannel(path: file.Path,
                              set: util.Set[_ <: OpenOption],
                              fileAttributes: FileAttribute[_]*): channels.SeekableByteChannel =
    newFileChannel(path, set, fileAttributes: _*)
}
