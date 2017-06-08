package org.hammerlab.paths

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
}
