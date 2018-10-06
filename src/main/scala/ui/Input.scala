package ui

trait Input {
  def read(): String = scala.io.StdIn.readLine()
}
