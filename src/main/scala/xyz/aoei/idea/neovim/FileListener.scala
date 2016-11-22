package xyz.aoei.idea.neovim

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.{FileEditor, FileEditorManager, FileEditorManagerAdapter, FileEditorManagerEvent}
import com.intellij.openapi.vfs.VirtualFile
import xyz.aoei.neovim.{Neovim => Nvim}

class FileListener(val nvim: Nvim) extends FileEditorManagerAdapter {
  var selectedTextEditor: Editor = null

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
