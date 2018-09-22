package utils

import better.files.File

object FileUtils {
  private lazy val cwd = File.currentWorkingDirectory

  def getFileAsRelativeStr(file: File): String = {
    cwd.relativize(file).toString
  }
}
