package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerAdapter, FileEditorManagerEvent}
import com.intellij.openapi.vfs.VirtualFile
import xyz.aoei.neovim.{Neovim => Nvim}

class NeovimFileEditorManagerListener(val nvim: Nvim) extends FileEditorManagerAdapter {
  var selectedTextEditor: Editor = _

  override def fileClosed(source: FileEditorManager, file: VirtualFile): Unit = super.fileClosed(source, file)

  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = {
    // Intellij opening a file doesn't always mean the selection is changed to the file
    // such as when a project is first opened, so we only create a buffer for the file
    // without "editing" it
    nvim.command("badd " + file.getPath)
  }

  override def selectionChanged(event: FileEditorManagerEvent): Unit = {
    // Edit the newly selected file
    nvim.command("e! " + event.getNewFile.getPath)

    selectedTextEditor = event.getManager.getSelectedTextEditor
  }
}
