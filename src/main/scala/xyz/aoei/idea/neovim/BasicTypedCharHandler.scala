package xyz.aoei.idea.neovim

import com.intellij.openapi.actionSystem.{DataContext, KeyboardShortcut}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

import xyz.aoei.neovim.{Neovim => Nvim}

class BasicTypedCharHandler(val nvim: Nvim) extends TypedActionHandler {
  override def execute(editor: Editor, c: Char, dataContext: DataContext): Unit = {
    nvim.feedkeys(c.toString, "t", true)
  }
}
