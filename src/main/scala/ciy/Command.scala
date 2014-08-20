package ciy

import java.io._
import scala.sys.process._

case class Command(cmd: String, logFile: String, cwd: String = ".") {
  def start: RunningCommand = {
    println(s"Starting `$cmd` [$cwd] => $logFile")

    val log = new PrintWriter(new FileWriter(logFile))
    val pio = new ProcessIO(_ => (), Command.tail(_, log), Command.tail(_, log))
    val process = Process(cmd, new File(cwd)).run(pio)

    RunningCommand(this, process, log)
  }

  def run: Int = start.process.exitValue

  def deleteLog(): Unit = new File(logFile).delete()
}

object Command {
  def tail(in: InputStream, out: PrintWriter, log: Boolean = true) = {
    try {
      val reader = new BufferedReader(new InputStreamReader(in))
      Iterator.continually(reader.readLine).takeWhile(_ ne null).foreach { line =>
        out.println(line)
        out.flush()
        if (log) {
          println(s"[tail] $line")
        }
      }
    } finally {
      in.close()
    }
  }
}

case class RunningCommand(cmd: Command, process: Process, output: PrintWriter)
