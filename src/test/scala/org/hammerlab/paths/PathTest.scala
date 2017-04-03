package org.hammerlab.paths

import java.nio.file.Files.createDirectory

import org.hammerlab.test.Suite
import org.hammerlab.test.matchers.seqs.SeqMatcher.seqMatch

class PathTest
  extends Suite {
  implicit def strToPath(str: String): Path = Path(str)

  test("extensions") {
    "abc.def".extension should be("def")
    "/abc/def.gh.ij".extension should be("ij")
    "hdfs://foo/bar.baz".extension should be("baz")
  }

  test("removals") {
    val dir = Path(tmpDir())

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

    dir.list.toSeq should seqMatch(
      Seq(
        bar,
        foo
      )
    )

    dir.walk.toSeq should seqMatch(
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

    val path = Path(tmpPath())
    path.writeLines(lines)
    path.lines.toSeq should be(lines)
  }
}
