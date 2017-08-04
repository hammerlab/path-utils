package org.hammerlab.paths

import java.io.{ ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream }
import java.nio.file.Files
import java.nio.file.Files.{ createDirectory, createTempDirectory }

import org.scalatest.{ BeforeAndAfterAll, FunSuite, Matchers }

import scala.collection.mutable.ArrayBuffer

class PathTest
  extends FunSuite
    with Matchers
    with BeforeAndAfterAll {

  implicit def strToPath(str: String): Path = Path(str)

  test("extensions") {
    "abc.def".extension should be("def")
    Path("abc.def") + ".ghi" should be(Path("abc.def.ghi"))
    Path("abc.def") / "ghi" should be(Path("abc.def/ghi"))

    "/abc/def.gh.ij".extension should be("ij")
    "file:///foo/bar.baz".extension should be("baz")
    Path("file:///foo/bar.baz") + ".qux" should be(Path("file:///foo/bar.baz.qux"))
    Path("file:///foo/bar.baz") / "qux" should be(Path("file:///foo/bar.baz/qux"))

    val dir = tmpPath(suffix = ".foo")
    Files.createDirectory(dir)
    dir.extension should be("foo")
  }

  test("removals") {
    val dir = tmpDir()

    assert(dir.exists)

    val bar = dir / "bar"

    assert(!bar.exists)

    createDirectory(bar)

    assert(bar.exists)

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

    dir.list.toSeq should be(
      Seq(
        bar,
        foo
      )
    )

    dir.walk.toSeq should be(
      Seq(
        dir,
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
  }

  test("read/write string round-trip") {
    val path = tmpPath()
    path.write("yay")
    path.read should be("yay")
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

  test("serialize path") {
    val path = Path("abc/def")
    val baos = new ByteArrayOutputStream()
    val oos = new ObjectOutputStream(baos)

    oos.writeObject(path)

    val bytes = baos.toByteArray
    bytes.length should be(94)

    val bais = new ByteArrayInputStream(bytes)
    val ois = new ObjectInputStream(bais)

    ois.readObject().asInstanceOf[Path] should be(path)
  }

  val dirs = ArrayBuffer[Path]()

  def tmpDir(prefix: String = this.getClass.getSimpleName): Path = {
    val f = Path(createTempDirectory(prefix))
    dirs += f
    f
  }

  /**
   * Return a [[Path]] to a temporary file that has not yet been created.
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
}
