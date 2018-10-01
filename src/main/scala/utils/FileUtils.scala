package utils

import better.files.File

object FileUtils {
  private lazy val cwd = File.currentWorkingDirectory

  def getFileAsRelativeStr(file: File, toRelativizeBy: File = cwd): String = {
    toRelativizeBy.relativize(file).toString
  }
}
