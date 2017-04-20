package org.hammerlab.paths

import java.lang.reflect.Field
import java.nio.file.spi.FileSystemProvider

import grizzled.slf4j.Logging

import scala.collection.JavaConverters._

object FileSystems
  extends Logging {

  private var _filesystemsInitialized = false
  def init(): Unit = {

    this.synchronized {
      if (!_filesystemsInitialized) {
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

        lock.synchronized {
          if (loadingProvidersField.get(null).asInstanceOf[Boolean]) {
            logger.info("FileSystems already loaded! forcing reload")
            loadingProvidersField.set(null, false)
            installedProvidersField.set(null, null)
          } else {
            logger.info("Loading filesystems")
          }
          load()
          _filesystemsInitialized = true

        }
      }
    }
  }

  private def load(): Unit = {
    /** Hack to pick up [[FileSystemProvider]] implementations; see https://issues.scala-lang.org/browse/SI-10247. */
    val scl = classOf[ClassLoader].getDeclaredField("scl")
    scl.setAccessible(true)
    val prevClassLoader = ClassLoader.getSystemClassLoader
    scl.set(null, Thread.currentThread().getContextClassLoader)

    logger.info(
      s"Loaded filesystems for schemes: ${FileSystemProvider.installedProviders().asScala.map(_.getScheme).mkString(",")}"
    )

    scl.set(null, prevClassLoader)
  }
}
