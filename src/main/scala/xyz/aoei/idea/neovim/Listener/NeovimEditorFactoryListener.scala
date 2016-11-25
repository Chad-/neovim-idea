package xyz.aoei.idea.neovim.Listener

import com.intellij.openapi.editor.event.{EditorFactoryEvent, EditorFactoryListener}
import xyz.aoei.idea.neovim.ShortcutKeyAction

class NeovimEditorFactoryListener extends EditorFactoryListener {
  override def editorCreated(event: EditorFactoryEvent): Unit = {
    // Every editor needs to have have all shortcutkeys be registered
    ShortcutKeyAction.registerEditor(event.getEditor)
  }

  override def editorReleased(event: EditorFactoryEvent): Unit = {

  }
}
