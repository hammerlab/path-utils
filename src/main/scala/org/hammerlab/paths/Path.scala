package org.hammerlab.paths

import java.io.{ InputStream, ObjectStreamException, OutputStream, PrintWriter }
import java.net.URI
import java.nio.file.Files.{ newDirectoryStream, newInputStream, newOutputStream, readAllBytes }
import java.nio.file.{ DirectoryStream, Files, Paths, Path ⇒ JPath }

import caseapp.core.ArgParser
import caseapp.core.ArgParser.instance
import org.apache.commons.io.FilenameUtils.getExtension

import scala.collection.JavaConverters._

case class Path(path: JPath) {

  @throws[ObjectStreamException]
  def writeReplace: Object = {
    val sp = new SerializablePath
    sp.str = toString
    sp
  }

  override def toString: String =
    if (path.isAbsolute)
      uri.toString
    else
      path.toString

  def uri: URI = path.toUri

  def extension: String = getExtension(basename)

  def exists: Boolean = Files.exists(path)

  def parent: Path = Path(path.getParent)
  def basename: String = path.getFileName.toString

  def size: Long = Files.size(path)

  def endsWith(suffix: String): Boolean = basename.endsWith(suffix)

  def isDirectory: Boolean = Files.isDirectory(path)

  def walk: Iterator[Path] =
    (if (exists)
      Iterator(this)
    else
      Iterator()
    ) ++
      Files.list(path)
        .iterator()
        .asScala
        .map(Path(_))
        .flatMap(
          p ⇒
            if (p.isDirectory)
              p.walk
            else
              Iterator(p)
        )

  def delete(recursive: Boolean = false): Unit = {
    if (recursive) {
      Files.list(path)
        .iterator()
        .asScala
        .map(Path(_))
        .foreach(
          p ⇒
            if (p.isDirectory)
              p.delete(recursive = true)
            else
              Files.delete(p)
        )
    }
    Files.delete(path)
  }

  def inputStream: InputStream = newInputStream(path)
  def outputStream: OutputStream = newOutputStream(path)

  private implicit def toScala(dirStream: DirectoryStream[JPath]): Iterator[Path] =
    dirStream
      .iterator()
      .asScala
      .map(Path(_))

  def list(glob: String): Iterator[Path] = newDirectoryStream(path, glob)
  def list: Iterator[Path] = newDirectoryStream(path)

  /**
   * Append `suffix` to the basename of this [[Path]].
   */
  def +(suffix: String): Path = Path(toString + suffix)

  def /(basename: String): Path = Path(path.resolve(basename))

  def lines: Iterator[String] =
    Files
      .lines(path)
      .iterator
      .asScala

  def read: String = new String(readBytes)

  def readBytes: Array[Byte] = readAllBytes(path)

  def write(str: String): Unit = {
    val os = outputStream
    os.write(str.getBytes())
    os.close()
  }

  def writeLines(lines: Iterable[String]): Unit = {
    val pw = new PrintWriter(outputStream)
    lines.foreach(pw.println)
    pw.close()
  }
}

object Path {

  import FileSystems.init

  private def get(pathStr: String): JPath = {
    init()
    Paths.get(pathStr)
  }

  private def get(uri: URI): JPath = {
    init()
    Paths.get(uri)
  }

  def apply(pathStr: String): Path = {
    val uri = new URI(pathStr)
    if (uri.getScheme == null)
      new Path(get(pathStr))
    else
      Path(uri)
  }

  def apply(uri: URI): Path = Path(get(uri))

  implicit def toJava(path: Path): JPath = path.path

  implicit val parser: ArgParser[Path] =
    instance("path") {
      str ⇒
        Right(
          Path(str)
        )
    }
}
