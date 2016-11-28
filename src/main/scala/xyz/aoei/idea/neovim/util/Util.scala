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
}
