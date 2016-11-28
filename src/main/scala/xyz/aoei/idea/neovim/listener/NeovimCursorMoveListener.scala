package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.editor.{LogicalPosition, ScrollType}
import com.intellij.openapi.fileEditor.TextEditor
import xyz.aoei.idea.neovim.util.Util
import xyz.aoei.idea.neovim.{CursorMoveListener, NeovimState}

class NeovimCursorMoveListener extends CursorMoveListener {
  override def cursorMoved(state: NeovimState): Unit = {
    if (state.windows.length != state.cursors.length) return

    state.windows.zip(state.cursors).foreach {
      case (window, cursor) => {
        if (state.windowToEditor.contains(window)) {

          val editor = state.windowToEditor(window).getSelectedEditor.getEditors.head.asInstanceOf[TextEditor].getEditor
          val caretModel = editor.getCaretModel

          val curPos = new LogicalPosition(cursor._1, cursor._2)
          if (caretModel.getLogicalPosition != curPos) {
            Util.invokeLater(() => {
              caretModel.moveToLogicalPosition(curPos)
              editor.getScrollingModel.scrollToCaret(ScrollType.RELATIVE)
            })
          }
        }
      }
    }
  }
}
