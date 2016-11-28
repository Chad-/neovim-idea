package xyz.aoei.idea.neovim.listener

import java.awt.event.{InputEvent, KeyEvent}

import xyz.aoei.idea.neovim.InputEventListener
import xyz.aoei.idea.neovim.util.KeyCodes
import xyz.aoei.neovim.{Neovim => Nvim}

object NeovimInputEventListener {
  // Generate a nvim prefix for some modifier bitmask
  private def modPrefix(mod: Int): String =
    KeyCodes.nvimModifiers.filter {
      case (m, s) => (mod & m) != 0
    }.values.mkString

  // Convert a java keyevent into a string nvim unsterstands
  def convertKeyEvent(e: KeyEvent): String = {
    val code = e.getKeyCode
    val mods = e.getModifiersEx
    val char = e.getKeyChar

    if (KeyCodes.nvimSpecialKeys.contains(code))
      "<" + modPrefix(mods) + KeyCodes.nvimSpecialKeys(code) + ">"
    else {
      if (char == KeyEvent.CHAR_UNDEFINED) ""
      else if (mods == 0) char.toString
      else "<" + modPrefix(mods) + char + ">"
    }

  }
}

class NeovimInputEventListener(val nvim: Nvim) extends InputEventListener {
  override def inputEvent(e: InputEvent): Unit = {
    val key = NeovimInputEventListener.convertKeyEvent(e.asInstanceOf[KeyEvent])

    println(key + "\t\t\t::\t\t" + e)

    // Give nvim the key
    nvim.input(key)
  }
}
