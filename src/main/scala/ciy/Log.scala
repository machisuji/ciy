package ciy

object Log {
  def info(msg: String, tag: String = "info") = println(s"[$tag] $msg")
  def warn(msg: String, tag: String = "warning") = println(s"[$tag] $msg")
  def debug(msg: String, tag: String = "debug") = println(s"[$tag] $msg")
}
