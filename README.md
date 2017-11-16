# paths
Serializable wrapper for java.nio.file.Path

[![Build Status](https://travis-ci.org/hammerlab/path-utils.svg?branch=master)](https://travis-ci.org/hammerlab/path-utils)
[![Coverage Status](https://coveralls.io/repos/github/hammerlab/path-utils/badge.svg?branch=master)](https://coveralls.io/github/hammerlab/path-utils?branch=master)

[![org.hammerlab:paths_2.11 on Maven Central](https://img.shields.io/maven-central/v/org.hammerlab/paths_2.11.svg?maxAge=600&label=org.hammerlab:paths_2.11)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.hammerlab%22%20AND%20a%3A%22paths_2.11%22)
[![org.hammerlab:paths_2.12 on Maven Central](https://img.shields.io/maven-central/v/org.hammerlab/paths_2.12.svg?maxAge=600&label=org.hammerlab:paths_2.12)](http://search.maven.org/#search%7Cga%7C1%7Cg%3A%22org.hammerlab%22%20AND%20a%3A%22paths_2.12%22)

Filesystem-path convenience methods and bug-fixes:

```scala
import hammerlab.path._
val path = Path("")    // current directory; URIs and Strings accepted
path.exists            // true
path / 'src list       // list children of src dir: Iterator(src/main,src/test)
```

## Examples

### Existence / Child-resolution

```scala
val src = path / 'src  // "src" subdir
src.exists             // true
path / 'foo exists     // false
```

### Directory-traversal

```scala
(path / 'src / 'test walk) filter (_.isFile)
// Iterator(
//   src/test/resources/log4j.properties,
//   src/test/resources/META-INF/services/java.nio.file.spi.FileSystemProvider,
//   src/test/scala/org/hammerlab/paths/PathTest.scala
// )
```

### IO

Self-explanatory methods:

- `def inputStream: InputStream`
- `def outputStream: OutputStream`
- `def read: String`
- `def readBytes: Array[Byte]`
- `def write(String)`
- `def writeLines(Iterable[String])`
- `def lines: Iterator[String]`
- `def size: Long`

### Serializability

`Path`s are `Serializable`, delegating to the [`SerializablePath`](src/main/scala/org/hammerlab/paths/SerializablePath.scala) proxy, which serializes them as `URI`s.

### JSR203 FileSystemProvider improvements

Interacting with Java NIO FileSystemProviders from Scala is broken; see [scala/bug#10247](https://github.com/scala/bug/issues/10247).

`Path`s from this repo have hooks to lazily initialize and tweak the FileSystemProvider initialization code to resolve these problems; see [FileSystems.scala](src/main/scala/org/hammerlab/paths/FileSystems.scala).

There is also fix for a missing `newByteChannel` implementation in the JRE's default implementation of `JarFileSystemProvider`; use of `Path`s replaces it with [a custom `JarFileSystemProvider`](src/main/scala/org/hammerlab/paths/JarFileSystemProvider.scala) with this issue resolved.

As an example, try downloading the Google Cloud Storage NIO connector JAR, and querying for the existence and size of [a public file](https://console.cloud.google.com/storage/browser/gcp-public-data-landsat):

```bash
wget -O lib/gcs-nio.jar http://search.maven.org/remotecontent?filepath=com/google/cloud/google-cloud-nio/0.28.0-alpha/google-cloud-nio-0.28.0-alpha-shaded.jar
sbt console
```
```scala
import hammerlab.path._
val index = Path("gs://gcp-public-data-landsat/index.csv.gz")

index.exists  // true
index.size    // 476118237
```

In contrast, trying the equivalent using vanilla Java NIO paths fails:

```scala
import java._, nio.file._, net._
val uri = new URI("gs://gcp-public-data-landsat/index.csv.gz")

Paths.get(uri)
// java.nio.file.FileSystemNotFoundException: Provider "gs" not installed
//   at java.nio.file.Paths.get(Paths.java:147)
//   ... 42 elided
```

(test this in a fresh `sbt console`, as initializing a hammerlab `Path` fixes the providers in that JVM)
