package xyz.aoei.idea.neovim

import com.intellij.openapi.actionSystem.{DataContext, KeyboardShortcut}
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.actionSystem.TypedActionHandler

class NeovimTypedActionHandler extends TypedActionHandler {
  // create stream
  override def execute(editor: Editor, c: Char, dataContext: DataContext): Unit = {
    // add char to stream and consume it
  }
}
