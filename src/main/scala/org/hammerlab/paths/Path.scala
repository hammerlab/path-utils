package org.hammerlab.paths

import java.io.{ InputStream, ObjectStreamException, OutputStream, PrintWriter }
import java.net.{ URI, URISyntaxException }
import java.nio.file.Files.{ newDirectoryStream, newInputStream, newOutputStream, readAllBytes }
import java.nio.file.spi.FileSystemProvider
import java.nio.file.{ DirectoryStream, FileSystemNotFoundException, Files, Paths, Path ⇒ JPath }

import caseapp.core.ArgParser
import caseapp.core.ArgParser.instance
import com.sun.nio.zipfs.ZipPath
import org.apache.commons.io.FilenameUtils.getExtension

import scala.collection.JavaConverters._

/**
 * Wrapper around [[java.nio.file.Path]] that adds useful methods, is [[Serializable]], and fixes some
 * provider/custom-scheme-loading issues (see [[FileSystems]]).
 */
case class Path(path: JPath) {

  /**
   * Delegate serialization to [[SerializablePath]]
   */
  @throws[ObjectStreamException]
  def writeReplace: Object = {
    val sp = new SerializablePath
    sp.str = toString
    sp
  }

  /**
   * In general, delegate to [[URI]]'s `toString`.
   *
   * In case of relative paths, take a short-cut to preserve relative-ness.
   */
  override def toString: String =
    if (path.isAbsolute)
      uri.toString
    else
      path match {
        case _: ZipPath ⇒
          uri.toString
        case _ ⇒
          path.toString
      }

  def uri: URI = path.toUri

  def extension: String = getExtension(basename)

  def exists: Boolean = Files.exists(path)

  def parent: Path = parentOpt.getOrElse(this)
  def parentOpt: Option[Path] = Option(path.getParent).map(Path(_))

  def depth: Int =
    if (path.isAbsolute)
      parentOpt
        .map(_.depth + 1)
        .getOrElse(0)
    else
      Path(path.toAbsolutePath).depth

  def basename: String = path.getFileName.toString

  def size: Long = Files.size(path)

  def endsWith(suffix: String): Boolean = basename.endsWith(suffix)

  def isFile: Boolean = Files.isRegularFile(path)
  def isDirectory: Boolean = Files.isDirectory(path)

  def walk: Iterator[Path] =
    if (!exists)
      Iterator()
    else
      Iterator(this) ++
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
  def +(suffix: String): Path = Path(path.resolveSibling(basename + suffix))

  def /(basename: String): Path = Path(path.resolve(basename))
  def /(basename: Symbol): Path = Path(path.resolve(basename.name))

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

  def providers: Seq[FileSystemProvider] = {
    init()
    FileSystemProvider
      .installedProviders()
      .asScala
  }

  /**
   * If a [[Path]] is instantiated from a "jar"-scheme [[URI]] before [[FileSystems.init()]] has been called, a
   * [[FileSystemNotFoundException]] can occur.
   *
   * Get out in front of that here, forcing a [[FileSystems.init()]] if necessary.
   */
  def registerJarFilesystem(uri: URI): Unit = {
    providers
      .find(_.getScheme == "jar")
      .foreach(
        p ⇒
          try {
            p.getFileSystem(uri)
          } catch {
            case _: FileSystemNotFoundException ⇒
              p.newFileSystem(
                uri,
                Map.empty[String, Any].asJava
              )
          }
      )
  }

  def apply(pathStr: String): Path =
    try {
      val uri = new URI(pathStr)
      uri.getScheme match {
        case null ⇒
          new Path(get(pathStr))
        case "jar" ⇒
          registerJarFilesystem(uri)
          Path(uri)
        case _ ⇒
          Path(uri)
      }
    } catch {
      case _: URISyntaxException ⇒
        new Path(get(pathStr))
    }

  def apply(uri: URI): Path =
    uri.getScheme match {
      case "jar" ⇒
        registerJarFilesystem(uri)
        Path(get(uri))
      case _ ⇒
        Path(get(uri))
    }

  implicit def toJava(path: Path): JPath = path.path

  implicit val parser: ArgParser[Path] =
    instance("path") {
      str ⇒
        Right(
          Path(str)
        )
    }
}
