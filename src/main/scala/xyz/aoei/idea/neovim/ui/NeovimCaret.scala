package xyz.aoei.idea.neovim.ui

import java.awt.{Color, Graphics}
import javax.swing.text._

class NeovimCaret extends DefaultCaret {
  var mode = "normal"

  def setMode(m: String): Unit = mode = m

  def getMode: String = mode

  override def paint(g: Graphics): Unit = {
    val comp: JTextComponent = getComponent
    if (comp == null) return

    val dot = getDot
    val r = comp.modelToView(dot)

    if ((x != r.x) || (y != r.y)) {
      repaint()
      x = r.x
      y = r.y
      height = r.height
    }

    val char = comp.getText(dot, 1).head

    if (isVisible) {
      g.setColor(comp.getCaretColor)
      if (mode == "insert") {
        g.fillRect(r.x, r.y, 2, r.height)
      } else {
        val width = g.getFontMetrics.charWidth('_')

        g.fillRect(r.x, r.y, width, r.height)
        g.setColor(Color.BLACK)

        g.setFont(comp.getFont)
        g.drawChars(Array(char), 0, 1, r.x, r.y + r.height - g.getFontMetrics.getDescent)
      }
    }
  }
}
