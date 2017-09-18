package org.hammerlab.paths

import java.lang.reflect.Field
import java.nio.file.spi.FileSystemProvider
import java.util

import com.sun.nio.zipfs
import com.sun.nio.zipfs.{ ZipFileSystem, ZipFileSystemProvider }
import grizzled.slf4j.Logging

import scala.collection.JavaConverters._

/**
 * Hooks for augmenting the [[FileSystemProvider]]s that the JDK loads by default.
 *
 *   - Scala fails to pick up user-supplied providers: https://issues.scala-lang.org/browse/SI-10247
 *   - Default [[zipfs.JarFileSystemProvider]] implementation is replaced with [[JarFileSystemProvider]], which defines
 *     a missing method.
 */
object FileSystems
  extends Logging {

  val cls = classOf[FileSystemProvider]

  def getField(name: String): Field = {
    val field = cls.getDeclaredField(name)
    field.setAccessible(true)
    field
  }

  val lockField = getField("lock")
  val lock = lockField.get(null)

  val loadingProvidersField = getField("loadingProviders")
  val installedProvidersField = getField("installedProviders")

  val zipFilesystemsField = {
    val field = classOf[ZipFileSystemProvider].getDeclaredField("filesystems")
    field.setAccessible(true)
    field
  }

  private var _filesystemsInitialized = false
  def init(): Unit =
    this.synchronized {
      if (!_filesystemsInitialized) {
        lock.synchronized {
          load()
          _filesystemsInitialized = true
        }
      }
    }

  private def load(): Unit = {

    val existingProviders =
      if (loadingProvidersField.get(null).asInstanceOf[Boolean]) {
        info("FileSystems already loaded! forcing reload")
        loadingProvidersField.set(null, false)
        val providers =
          installedProvidersField
            .get(null)
            .asInstanceOf[util.List[FileSystemProvider]]

        // null out the field for now so that it will register as needing to be reloaded later
        installedProvidersField.set(null, null)

        providers.asScala
      } else {
        val providers = FileSystemProvider.installedProviders().asScala
        info(s"Loading filesystems, starting with: ${providers.map(_.getScheme).mkString(",")}")
        providers
      }

    /**
     * Replace the system classloader with the current thread's, for use during a second round of loading installed
     * providers.
     *
     * Hack to pick up [[FileSystemProvider]] implementations; see https://issues.scala-lang.org/browse/SI-10247.
     */
    val scl = classOf[ClassLoader].getDeclaredField("scl")
    scl.setAccessible(true)
    val prevClassLoader = ClassLoader.getSystemClassLoader
    scl.set(null, Thread.currentThread().getContextClassLoader)

    var newClassLoaderProviders =
      FileSystemProvider
        .installedProviders()
        .asScala
        .map(
          p ⇒
            p.getScheme →
              p
        )
        .toMap

    val existingProvidersMap =
      existingProviders
        .map(
          p ⇒
            p.getScheme →
              p
        )
        .toMap

    val addedProviders =
      newClassLoaderProviders
        .filterKeys(
          !existingProvidersMap.contains(_)
        )

    if (addedProviders.nonEmpty) {
      info(s"Adding new providers: ${addedProviders.keys.mkString(",")}")
    }

    val newProviders =
      (existingProviders ++ addedProviders.values)
        /**
         * If a [[ZipFileSystemProvider]] is installed for the "jar" scheme, replace it with a [[JarFileSystemProvider]]
         * (porting over any jar-[[ZipFileSystem]]s that had been created.
         */
        .map {
          case p: JarFileSystemProvider ⇒ p
          case p: ZipFileSystemProvider ⇒
            val newP = new JarFileSystemProvider
            info(s"Replacing default 'jar'-provider")
            zipFilesystemsField.set(
              newP,
              zipFilesystemsField.get(p)
            )
            newP
          case p ⇒ p
        }

    installedProvidersField.set(
      null,
      newProviders
        .asJava
        .asInstanceOf[util.List[FileSystemProvider]]
    )

    scl.set(null, prevClassLoader)

    info(
      s"Filesystems exist for schemes: ${FileSystemProvider.installedProviders().asScala.map(_.getScheme).mkString(",")}"
    )
  }
}
