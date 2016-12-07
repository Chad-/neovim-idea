package xyz.aoei.idea.neovim.util

import com.intellij.openapi.application.ApplicationManager

object Util {
  def invokeLater(func: () => Unit): Unit = {
    ApplicationManager.getApplication.invokeLater(new Runnable {
      override def run(): Unit = {
        func()
      }
    })
  }

  def colorToRgb(color: Int): List[Int] = {
    List(16, 8, 0).map {
      shift => {
        val mask = 0xff << shift
        (color & mask) >> shift
      }
    }
  }
}
