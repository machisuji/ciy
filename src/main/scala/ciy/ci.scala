package ciy

import scala.sys.process._
import scala.concurrent.SyncVar
import java.io.File

trait CI {
  private val process: SyncVar[(Process, FileProcessLogger)] = new SyncVar

  def pullCommand: SimpleCommand
  def testCommand: SimpleCommand
  def runCommand: CommandWithLog

  def flushLog(): Unit = {
    if (process.isSet) {
      process.get._2.flush()
    }
  }

  def newRevisionPushed(): Unit = {
    println("[CI] New revision pushed.")

    threadDo() {
      if (pull() && test()) {
        if (process.isSet) {
          println("[CI] Stopping current application ...")
          process.get._1.destroy()
          process.get._2.close()
          println("[CI] Application stopped")
        }
        process put run()
      }
    }
  }

  def pull(): Boolean = {
    pullCommand.deleteLog()
    pullCommand.run == 0
  }

  def test(): Boolean = {
    testCommand.deleteLog()
    testCommand.run == 0
  }
  def run(): (Process, FileProcessLogger) = {
    runCommand.deleteLog()
    runCommand.start
  }

  def threadDo(name: String = "", daemon: Boolean = true)(block: => Unit) {
    val run = new Runnable {
      override def run(): Unit = block
    }
    val thread = new Thread(run)

    if (name != "") {
      thread.setName(name)
    }
    thread.setDaemon(daemon)
    thread.start()
  }

  def threadDo(block: => Unit): Unit = threadDo()(block)
}

object CI extends CI {
  val cwd = sys.env.get("CIY_CWD")

  def pullCommand = SimpleCommand(
    cmd = sys.env.get("CIY_PULL_CMD").getOrElse("echo pull command"),
    cwd = sys.env.get("CIY_PULL_CWD").orElse(cwd).getOrElse("."),
    logFile = Some("pull_output.log"))

  def testCommand = SimpleCommand(
    cmd = sys.env.get("CIY_TEST_CMD").getOrElse("echo test command"),
    cwd = sys.env.get("CIY_TEST_CWD").orElse(cwd).getOrElse("."),
    logFile = Some("test_output.log"))

  def runCommand = CommandWithLog(
    cmd = sys.env.get("CIY_RUN_CMD").getOrElse("echo run command"),
    cwd = sys.env.get("CIY_RUN_CWD").orElse(cwd).getOrElse("."),
    logFile = "run_output.log")
}
