package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.impl.{EditorWindow, FileEditorManagerImpl}
import com.intellij.openapi.vfs.LocalFileSystem
import xyz.aoei.idea.neovim.util.{SplitUtil, Util}
import xyz.aoei.idea.neovim.{NeovimState, WindowsChangeListener}

import scala.concurrent.ExecutionContext.Implicits.global

class NeovimWindowsChangeListener extends WindowsChangeListener {
  override def windowsChanged(state: NeovimState): Unit = {
    val fileEditorManager = FileEditorManager.getInstance(state.project).asInstanceOf[FileEditorManagerImpl]
    val splitters = fileEditorManager.getSplitters

    Util.invokeLater(() => {
      // Close all windows except one
      val current = fileEditorManager.getCurrentWindow
      for (win <- fileEditorManager.getWindows) {
        if (win != current) win.closeAllExcept(null)
      }

      val editorWindow = splitters.getWindows.head

      val splitTree = SplitUtil.getTree(state.windowPositions.zip(state.windows))

      state.windowToEditor = Map()
      split(splitTree, editorWindow, fileEditorManager, state)
    })
  }

  def split(tree: SplitUtil.Node, win: EditorWindow, fileEditorManager: FileEditorManagerImpl, state: NeovimState): Unit = {
    tree match {
      case SplitUtil.Branch(dir, _, l, r) =>
        val newWin = win.split(dir, true, null, true)
        split(l, win, fileEditorManager, state)
        split(r, newWin, fileEditorManager, state)
      case SplitUtil.Leaf(_, w) =>
        state.windowToEditor += (w -> win)

        for {
          buf <- w.getBuf
          name <- buf.getName
        } yield {
          Util.invokeLater(() => {
            val virtualFile = LocalFileSystem.getInstance().findFileByPath(name)
            fileEditorManager.setCurrentWindow(win)
            fileEditorManager.openFile(virtualFile, true)
            win.closeAllExcept(virtualFile)
          })
        }
    }
  }

  override def selectedChanged(state: NeovimState): Unit = {
    if (state.windowToEditor.contains(state.selectedWindow)) {
      Util.invokeLater(() => {
        state.windowToEditor(state.selectedWindow).setAsCurrentWindow(true)
      })
    }
  }
}
