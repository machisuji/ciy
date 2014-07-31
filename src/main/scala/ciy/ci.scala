package ciy

import scala.sys.process._
import scala.concurrent.SyncVar

class CI {
  private val process: SyncVar[(Process, FileProcessLogger)] = new SyncVar

  def flushLog(): Unit = {
    if (process.isSet) {
      process.get._2.flush()
    }
  }

  def newRevisionPushed(): Unit = {
    println("[CI] New revision pushed.")

    threadDo() {
      if (testBuild()) {
        if (process.isSet) {
          println("[CI] Stopping current application ...")
          process.get._1.destroy()
          process.get._2.close()
          println("[CI] Application stopped")
        }
        process put runApp()
      }
    }
  }

  def testBuild(): Boolean = {
    // substitue for actual implementation
    println("[CI] Testing build ...")

    val logger = new FileProcessLogger(testLogFile)
    val pb = Process(testCommand)
    val process = pb.run(logger)

    val result = process.exitValue == 0

    logger.flush()
    logger.close()

    result
  }

  def testCommand = "echo insert test command here"
  def testLogFile = new java.io.File("test_output.log")

  def runApp(): (Process, FileProcessLogger) = {
    println("[CI] Running app ...")

    val logger = new FileProcessLogger(runLogFile)
    val pb = Process(runCommand)
    val process = pb.run(logger)

    (process, logger)
  }

  def runCommand = "echo insert run command here"
  def runLogFile = new java.io.File("run_output.log")

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
  @volatile var activeTestCommand: String = "echo test command"
  override def testCommand = activeTestCommand

  @volatile var activeRunCommand: String = "echo run command"
  override def runCommand = activeRunCommand
}
