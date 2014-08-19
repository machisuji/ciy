package ciy

import java.io.File
import scala.sys.process._

trait Command {
  def defaultCwd = new File(".")

  def startWithLog(cmd: String, cwd: File = defaultCwd, logFile: File): (Process, FileProcessLogger) = {
    val pb = Process(cmd, cwd)
    val logger = new FileProcessLogger(logFile)
    val process = pb.run(logger)

    (process, logger)
  }
  def runWithLog(cmd: String, cwd: File = defaultCwd, logFile: File): (Int, FileProcessLogger) = {
    val (process, logger) = startWithLog(cmd, cwd, logFile)

    (process.exitValue, logger)
  }

  def start(cmd: String, cwd: File = defaultCwd): Process = Process(cmd, cwd).run()
  def run(cmd: String, cwd: File = defaultCwd): Int = start(cmd, cwd).exitValue
}

trait Papertrail extends Command {
  override def startWithLog(cmd: String, cwd: File, logFile: File): (Process, FileProcessLogger) = {
    Log.info(s"Starting `$cmd > ${logFile.getName}` in '$cwd'")
    super.startWithLog(cmd, cwd, logFile)
  }

  override def start(cmd: String, cwd: File): Process = {
    Log.info(s"Starting `$cmd` in '$cwd'")
    super.start(cmd, cwd)
  }

  override def runWithLog(cmd: String, cwd: File = defaultCwd, logFile: File): (Int, FileProcessLogger) = {
    val (result, logger) = super.runWithLog(cmd, cwd, logFile)
    Log.info(s"exit code: $result")

    (result, logger)
  }

  override def run(cmd: String, cwd: File = defaultCwd): Int = {
    val result = super.run(cmd, cwd)
    Log.info(s"exit code: $result")

    result
  }
}

object Command extends Command with Papertrail

case class SimpleCommand(cmd: String, cwd: String = ".", logFile: Option[String]) {
  def run: Int = logFile.map(new File(_)).map { file =>
    val (ret, logger) = Command.runWithLog(cmd, new File(cwd), file)

    logger.flush()
    logger.close()

    ret
  }.getOrElse {
    Command.run(cmd, new File(cwd))
  }

  def deleteLog(): Unit = logFile.map(new File(_)).foreach(_.delete())
}

case class CommandWithLog(cmd: String, logFile: String, cwd: String = ".") {
  def start: (Process, FileProcessLogger) =
    Command.startWithLog(cmd, new File(cwd), new File(logFile))

  def deleteLog(): Unit = new File(logFile).delete()
}

case class TailingCommand(cmd: String, logFile: String, cwd: String = ".") {
  import java.io._

  val start: Process = {
    def tail(in: InputStream, out: PrintWriter) = {
      val reader = new BufferedReader(new InputStreamReader(in))
      println(s"Tailing `$cmd`")
      Iterator.continually(reader.readLine).takeWhile(_ ne null).foreach { line =>
        out.println(line)
        out.flush()
        println(s"[tail] $line")
      }
      println("Command finished")
      reader.close()
    }

    val log = new PrintWriter(new FileWriter(logFile))
    val pio = new ProcessIO(_ => (), tail(_, log), tail(_, log))

    Process(cmd, new File(cwd)).run(pio)
  }

  def deleteLog(): Unit = new File(logFile).delete()
}
