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
  def fail = throw new RuntimeException("Please configure pull, test and run commands.")

  val cfg = YAML.fromFile("ciy.yml").getOrElse(YamlMap.empty)
  val cwd = sys.env.get("CIY_CWD").orElse(cfg.string("cwd")).getOrElse(".")

  def pullCommand = SimpleCommand(
    cmd = sys.env.get("CIY_PULL_CMD").orElse(cfg.string("pull.cmd")).getOrElse(fail),
    cwd = sys.env.get("CIY_PULL_CWD").orElse(cwd).getOrElse(fail),
    logFile = Some("pull_output.log"))

  def testCommand = SimpleCommand(
    cmd = sys.env.get("CIY_TEST_CMD").orElse(cfg.string("test.cmd")).getOrElse(fail),
    cwd = sys.env.get("CIY_TEST_CWD").getOrElse(cwd),
    logFile = Some("test_output.log"))

  def runCommand = CommandWithLog(
    cmd = sys.env.get("CIY_RUN_CMD").orElse("run.cmd").getOrElse(fail),
    cwd = sys.env.get("CIY_RUN_CWD").getOrElse(cwd),
    logFile = "run_output.log")
}
