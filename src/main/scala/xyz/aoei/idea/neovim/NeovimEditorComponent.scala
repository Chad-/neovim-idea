package xyz.aoei.idea.neovim

import java.awt.{Color, Font, Graphics}
import javax.swing.text._
import javax.swing.{JTextPane, SwingUtilities}

import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.ui.TypingTarget
import com.intellij.openapi.util.ActionCallback
import xyz.aoei.idea.neovim.ui.NeovimCaret
import xyz.aoei.idea.neovim.util.{BatchDocument, Util}

class NeovimEditorComponent extends JTextPane {
  setEditable(false)

  val caret = new NeovimCaret
  caret.setVisible(true)

  setCaret(caret)

  val fontScheme = EditorColorsManager.getInstance().getGlobalScheme
  setFont(new Font(fontScheme.getEditorFontName, Font.PLAIN, fontScheme.getEditorFontSize))

  var styles: Map[Attributes, SimpleAttributeSet] = Map()

  def setNeovimText(state: UIState) = {

    val bdoc = new BatchDocument()

    for {
      row <- state.grid.map(r => r ++ List(Text('\n', Attributes())))
      col <- row
    } yield {
      val style = getStyle(col.attr)

      if (col.text == '\n') bdoc.appendBatchLineFeed(style)
      else bdoc.appendBatchString(col.text.toString, style)
    }
    bdoc.processBatchUpdates(0)

    SwingUtilities.invokeLater(new Runnable() {
      override def run(): Unit = {
        setDocument(bdoc)
        setCaretPosition((state.grid.head.length+1) * state.cursor._1 + state.cursor._2)
        caret.setVisible(true)
        caret.setMode(state.mode)
      }
    })
  }

  // Get text style from cache or create it
  def getStyle(attr: Attributes): SimpleAttributeSet = {
    if (!styles.contains(attr)) {
      def intToColor(color: Int): Color = {
        val rgb = Util.colorToRgb(color)
        new Color(rgb.head, rgb(1), rgb(2))
      }

      val style = new SimpleAttributeSet()
      if (attr.foreground > 0) StyleConstants.setForeground(style, intToColor(attr.foreground))
      if (attr.background > 0) StyleConstants.setBackground(style, intToColor(attr.background))

      styles += (attr -> style)
    }

    styles(attr)
  }
}
