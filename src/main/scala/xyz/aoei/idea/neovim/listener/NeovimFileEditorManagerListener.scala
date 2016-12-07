package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerAdapter, FileEditorManagerEvent}
import com.intellij.openapi.vfs.VirtualFile
import xyz.aoei.idea.neovim.NeovimProcess
import xyz.aoei.neovim.{Neovim => Nvim}

class NeovimFileEditorManagerListener() extends FileEditorManagerAdapter {
  override def fileClosed(source: FileEditorManager, file: VirtualFile): Unit = super.fileClosed(source, file)

  override def fileOpened(source: FileEditorManager, file: VirtualFile): Unit = {
    // Intellij opening a file doesn't always mean the selection is changed to the file
    // such as when a project is first opened, so we only create a buffer for the file
    // without "editing" it
    NeovimProcess.getInstance().command("badd " + file.getPath)
  }

  override def selectionChanged(event: FileEditorManagerEvent): Unit = {
    // Edit the newly selected file
    NeovimProcess.getInstance().command("buffer! " + event.getNewFile.getPath)
//    val editor = event.getNewEditor
//    if (editor.isInstanceOf[NeovimEditor]) {
//      val component = editor.getComponent
//      val metrics = component.getFontMetrics(component.getFont)
//
//      val charWidth = metrics.charWidth('_')
//      val charHeight = metrics.getHeight
//
//      val size = component.getSize()
//
//      nvim.uiTryResize(Math.floor(size.width / charWidth).asInstanceOf[Int],
//        Math.floor(size.height / charHeight).asInstanceOf[Int])
//    }
  }
}
