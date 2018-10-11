package utils

import java.nio.file.Path

import actors.FileHandlerActor.LinesOption
import actors.Messages.FileLines
import better.files.File

object FileUtils {
  private lazy val cwd = File.currentWorkingDirectory

  /**
    * Get the [[Path]] a [[File]] relative to another file.
    *
    * @param file           The [[File]] to get the [[Path]] to.
    * @param toRelativizeBy The [[File]] to relativize the path by.
    * @return the [[Path]] a file relative to toRelativizeBy.
    */
  def getFileAsRelativeStr(file: File, toRelativizeBy: File): String = {
    toRelativizeBy.relativize(file).toString
  }

  /**
    * Get a [[Map]] representing the non-directory files in a directory (and subdirectories).
    * For example, for the directory with the following structure:
    * - dir
    *   - file1.txt (content: "a line in file1"
    *   - innerDir1
    *       - file2.txt (empty content)
    *   - innerDir2
    *       - file3.txt (content: "first line in file3 \n second line in file3")
    *
    * the following map will be returned:
    * {{{
    * Map(Path("file1.txt") -> Traversable("a line in file1"),
    *     Path("innerDir1/file2.txt") -> Traversable.empty,
    *     Path("innerDir2/file3.txt") -> Traversable("first line in file3", "second line in file3"))
    * }}}
    *
    * @param dir The directory who's files should be in the map.
    * @return A [[Map]]@tparam[ [[Path]],[[FileLines]] ] in which each entry is in the form:
    *         ([[Path]] of a file in dir, relative to dir -> [[Traversable]](lines in that file)
    */
  def dirToMap(dir: File): Map[Path, FileLines] = {
    if (!dir.exists) throw new Exception(s"file $dir does not exist!")
    if (!dir.isDirectory) throw new Exception(s"file $dir is not a directory!")

    val map = dir.walk()
      .withFilter(!_.isDirectory) // ignore directories
      .map(file => (dir.relativize(file), file.lines))
      .toMap

    map
  }
}
