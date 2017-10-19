package io.github.morgaroth

import better.files.File

import scala.language.postfixOps

object Directories {
  def merge(src: File, target: File, includeHidden: Boolean = false) {
    if (!src.isDirectory || !target.isDirectory) {
      throw new IllegalArgumentException(s"one of [$src, $target] isn't a directory.")
    }

    src.list.filter(f => !f.isHidden || includeHidden).foreach { s =>
      val targetFile = target / s.name
      if (targetFile exists) {
        if (targetFile.isDirectory && s.isDirectory) merge(s, targetFile, includeHidden)
        else if (s.isSimilarContentAs(targetFile)) {
          println(s"$src and $targetFile, deleting source file?")
          s.delete()
        } else {
          println(s"Unable to move $s to $target")
        }
      } else {
        s.copyTo(targetFile)
        s.delete()
      }

    }
  }
}