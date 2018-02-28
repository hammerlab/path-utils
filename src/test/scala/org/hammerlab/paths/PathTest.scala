package org.hammerlab.paths

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }
import java.lang.Thread.currentThread
import java.net.URI
import java.nio.channels.SeekableByteChannel
import java.nio.file
import java.nio.file.Files.{ createDirectory, createTempDirectory }
import java.nio.file.attribute.{ BasicFileAttributes, FileAttribute, FileAttributeView }
import java.nio.file.spi.FileSystemProvider
import java.nio.file.{ AccessMode, CopyOption, DirectoryStream, FileStore, FileSystem, FileSystemException, Files, LinkOption, OpenOption }
import java.util

import com.sun.nio.zipfs
import org.hammerlab.paths.FileSystems._
import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer

class PathTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterAll {

  implicit def strToPath(str: String): Path = Path(str)

  // Use the import API
  import hammerlab.path._

  test("extensions") {
    "abc.def".extension should be("def")
    Path("abc.def") + ".ghi" should be(Path("abc.def.ghi"))
    'abc / 'ghi should be(Path("abc/ghi"))
    "abc.def" / 'ghi should be(Path("abc.def/ghi"))

    "/abc/def.gh.ij".extension should be("ij")
    "file:///foo/bar.baz".extension should be("baz")
    Path("file:///foo/bar.baz") + ".qux" should be(Path("file:///foo/bar.baz.qux"))
    Path("file:///foo/bar.baz") / "qux" should be(Path("file:///foo/bar.baz/qux"))

    val dir = tmpPath(suffix = ".foo")
    Files.createDirectory(dir)
    dir.exists should be(true)
    dir.extension should be("foo")
    dir.endsWith(".foo") should be(true)
    dir.endsWith("foo") should be(true)
    dir.endsWith("fo") should be(false)
    dir.delete(recursive = false)
    dir.exists should be(false)
  }

  test("basename") {
    val dir = tmpDir()

    def check(path: Path, withExtension: String, withoutExtension: String): Unit = {
      path.basename                    should be(withExtension)
      path.basename(extension =  true) should be(withExtension)
      path.basename(extension = false) should be(withoutExtension)
      path.basenameNoExtension         should be(withoutExtension)
    }

    check(dir / 'foo, "foo", "foo")
    check(dir / "bar.baz", "bar.baz", "bar")
    check(dir / 'abc / "def.ghi.jkl", "def.ghi.jkl", "def.ghi")
  }

  test("removals") {
    val dir = tmpDir()

    assert(dir.exists)

    val bar = dir / "bar"

    assert(!bar.exists)

    createDirectory(bar)

    assert(bar.exists)
    assert(bar.isDirectory)
    assert(!bar.isFile)

    val foo = dir / "foo"

    assert(!foo.exists)

    foo.basename should be("foo")

    foo.write("abc")

    assert(foo.exists)

    foo.read should be("abc")

    val baz = bar / "baz"

    assert(!baz.exists)

    baz.write("yay")

    assert(baz.exists)

    dir.list.toSet should be(
      Set(
        bar,
        foo
      )
    )

    val head :: tail = dir.walk.toList

    // The parent directory should be first…
    head should be(dir)

    // …but after that the order isn't enformced
    tail.toSet should be(
      Set(
        bar,
        baz,
        foo
      )
    )

    dir.delete(recursive = true)

    assert(!foo.exists)
    assert(!bar.exists)
    assert(!baz.exists)
    assert(!dir.exists)

    dir.walk.toList should be(Nil)
  }

  test("read/write string round-trip") {
    val path = tmpPath()
    path.write("yay")
    path.read should be("yay")
  }

  test("outputstream mkdirs") {
    val dir = tmpPath()
    val path = dir / 'a / 'b

    intercept[FileSystemException] {
      path.outputStream
    }

    intercept[FileSystemException] {
      path.outputStream(mkdirs = false)
    }

    val os = path.outputStream(mkdirs = true)
    os.write("yay".getBytes)
    os.close()
    path.read should be("yay")
  }

  test("printstream mkdirs") {
    val dir = tmpPath()
    val path = dir / 'a / 'b
    intercept[FileSystemException] {
      path.printStream
    }

    intercept[FileSystemException] {
      path.printStream(mkdirs = false)
    }

    val ps = path.printStream(mkdirs = true)
    ps.println("yay")
    ps.close()
    path.read should be("yay\n")

    // works fine when directories already exist
    val path2 = dir / 'a / 'c
    val ps2 = path.printStream(mkdirs = true)
    ps2.println("woo")
    ps2.close()
    path.read should be("woo\n")
  }

  test("read/write lines round-trip") {
    val lines =
      Seq(
        "abc",
        "def",
        "ghi"
      )

    val path = tmpPath()
    path.writeLines(lines)
    path.lines.toSeq should be(lines)
  }

  test("read empty last line") {
    val lines =
      Seq(
        "abc",
        "def",
        "ghi"
      )

    val path = tmpPath()
    path.writeLines(lines)
    path.read should be(
      """abc
        |def
        |ghi
        |"""
      .stripMargin
    )
  }

  def checkSerde(str: String, expectedBytes: Int): Unit =
    checkSerde(Path(str), expectedBytes)

  def checkSerde(path: Path, expectedBytes: Int = -1): Unit = {
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)

    oos.writeObject(path)

    val bytes = baos.toByteArray

    if (expectedBytes >= 0)
      bytes.length should be(expectedBytes)

    val bais = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bais)

    ois.readObject().asInstanceOf[Path] should be(path)
  }

  test("serialize path") {
    checkSerde("abc/def", 94)
  }

  val dirs = ArrayBuffer[Path]()

  def tmpDir(prefix: String = this.getClass.getSimpleName): Path = {
    val f = Path(createTempDirectory(prefix))
    dirs += f
    f
  }

  /**
   * Return a [[Path]] to a temporary file that has not yet been created.
   *
   * Duplicated from org.hammerlab.test.Suite because org.hammerlab:base depends on this module
   */
  def tmpPath(prefix: String = this.getClass.getSimpleName,
              suffix: String = ""): Path =
    tmpDir() / (prefix + suffix)

  override def afterAll(): Unit = {
    super.afterAll()
    dirs.foreach(d ⇒ if (d.exists) d.delete(true))
  }

  test("parentOpt") {
    Path("/").parentOpt should be(None)
    Path("/a").parentOpt should be(Some(Path("/")))
    Path("/a/b").parentOpt should be(Some(Path("/a")))
    Path("a").parentOpt should be(None)
    Path("a/b").parentOpt should be(Some(Path("a")))
  }

  test("parent") {
    Path("/").parent should be(Path("/"))
    Path("/a").parent should be(Path("/"))
    Path("/a/b").parent should be(Path("/a"))
    Path("a").parent should be(Path("a"))
    Path("a/b").parent should be(Path("a"))
  }

  val testDirDepth = Path(".").depth

  test("depth") {
    Path("/").depth should be(0)
    Path("/a").depth should be(1)
    Path("/a/b").depth should be(2)

    Path("a").depth should be(testDirDepth)
    Path("a/b").depth should be(testDirDepth + 1)
  }

  test("with spaces") {
    Path("a b").depth should be(testDirDepth)
    Path("a b/c d").depth should be(testDirDepth + 1)
    Path("/a b").depth should be(1)
    Path("/a b/c d").depth should be(2)

    // Invalid characters for URI, interpret as local file
    Path("hdfs:///a b").uri.getScheme should be("file")
  }

  test("JarFilesystemProvider") {
    FileSystemProvider.installedProviders().asScala.find(_.getScheme == "jar") match {
      case Some(p: JarFileSystemProvider) ⇒
      case Some(p) ⇒ fail(s"Expected JarFileSystemProvider, found $p")
      case None ⇒ fail("Found no FileSystemProvider for 'jar' scheme")
    }
  }

  def resource(name: String): Path =
    Path(
      currentThread
        .getContextClassLoader
        .getResource(name)
        .toURI
    )

  test("jar files") {
    val path = resource("10.txt")

    path.exists should be(true)
    path.lines.map(_.toInt).toList should be(1 to 10)
    path.size should be(21)

    /** Use [[JarFileSystemProvider.newByteChannel]] */
    val ch = Files.newByteChannel(path)
    ch.size() should be(21)
    ch.close()

    val twice = path + ".twice"
    twice.exists should be(true)
    twice.lines.map(_.toInt).toList should be((1 to 10) ++ (1 to 10))

    val nonExistent = path + ".thrice"
    nonExistent.exists should be(false)

    checkSerde(twice)
    checkSerde(nonExistent)
  }

  test("glob") {
    resource("test.jar")
      .list("10*")
      .map(_.basename)
      .toSet should be(
      Set(
        "10.txt",
        "10.txt.twice"
      )
    )
  }

  /**
   * Force-reload [[FileSystemProvider]]s without [[FileSystems]] fixes
   */
  def defaultProviders: Seq[FileSystemProvider] = {
    clearProviders
    installedProviders
  }

  test("augment providers") {

    /** Reset initialization state of [[FileSystems]] singleton */
    val fcl = FileSystems.getClass
    val field = fcl.getDeclaredField("_filesystemsInitialized")
    field.setAccessible(true)
    field.set(FileSystems, false)

    val beforeProviders = defaultProviders

    init()

    val afterProviders = installedProviders

    beforeProviders.find(_.getScheme == "jar") match {
      case None ⇒
      case Some(_: JarFileSystemProvider) ⇒
        fail("Expected default 'jar'-scheme provider to not be org.hammerlab.paths.JarFileSystemProvider")
      case Some(_: zipfs.ZipFileSystemProvider) ⇒
      case Some(p) ⇒
        fail(s"Unexpected 'jar'-scheme provider: $p")
    }

    afterProviders.find(_.getScheme == "jar") match {
      case Some(_: JarFileSystemProvider) ⇒
      case p ⇒
        fail(s"Unexpected 'jar'-scheme provider: $p")
    }

    afterProviders.map(_.getScheme).toSet should be(
      Set(
        "file",
        "jar",
        "foo"
      )
    )
  }

  test("printstream append") {
    val dir = tmpDir()
    val path = dir / 'abc / 'def

    {
      val ps = path.printStream(append = true, mkdirs = true)
      ps.println("yay")
      ps.close()
    }

    path.read should be("yay\n")

    {
      val ps = path.printStream(append = true)
      ps.println("yay2")
      ps.close()
    }

    path.read should be("yay\nyay2\n")
  }
}

class FooFileSystemProvider extends FileSystemProvider {
  override def getScheme: String = "foo"

  override def newByteChannel(path: file.Path, options: util.Set[_ <: OpenOption], attrs: FileAttribute[_]*): SeekableByteChannel = ???
  override def isSameFile(path: file.Path, path2: file.Path): Boolean = ???
  override def getFileStore(path: file.Path): FileStore = ???
  override def getFileAttributeView[V <: FileAttributeView](path: file.Path, `type`: Class[V], options: LinkOption*): V = ???
  override def delete(path: file.Path): Unit = ???
  override def setAttribute(path: file.Path, attribute: String, value: scala.Any, options: LinkOption*): Unit = ???
  override def readAttributes[A <: BasicFileAttributes](path: file.Path, `type`: Class[A], options: LinkOption*): A = ???
  override def readAttributes(path: file.Path, attributes: String, options: LinkOption*): util.Map[String, AnyRef] = ???
  override def getPath(uri: URI): file.Path = ???
  override def createDirectory(dir: file.Path, attrs: FileAttribute[_]*): Unit = ???
  override def copy(source: file.Path, target: file.Path, options: CopyOption*): Unit = ???
  override def move(source: file.Path, target: file.Path, options: CopyOption*): Unit = ???
  override def newFileSystem(uri: URI, env: util.Map[String, _]): FileSystem = ???
  override def newDirectoryStream(dir: file.Path, filter: DirectoryStream.Filter[_ >: file.Path]): DirectoryStream[file.Path] = ???
  override def getFileSystem(uri: URI): FileSystem = ???
  override def checkAccess(path: file.Path, modes: AccessMode*): Unit = ???
  override def isHidden(path: file.Path): Boolean = ???
}
