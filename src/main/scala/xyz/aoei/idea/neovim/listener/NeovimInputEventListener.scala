package xyz.aoei.idea.neovim.listener

import java.awt.event.{InputEvent, KeyEvent, KeyListener}

import xyz.aoei.idea.neovim.util.KeyCodes
import xyz.aoei.idea.neovim.{InputEventListener, NeovimProcess}
import xyz.aoei.neovim.{Neovim => Nvim}

object NeovimInputEventListener {
  // Generate a nvim prefix for some modifier bitmask
  private def modPrefix(mod: Int): String =
    KeyCodes.nvimModifiers.filter {
      case (m, s) => (mod & m) != 0
    }.values.mkString

  // Convert a java keyevent into a string nvim understands
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

class NeovimInputEventListener() extends InputEventListener with KeyListener {
  override def inputEvent(e: InputEvent): Unit = {
    keyTyped(e.asInstanceOf[KeyEvent])
  }

  override def keyTyped(e: KeyEvent): Unit = {
    val key = NeovimInputEventListener.convertKeyEvent(e)

    // Give nvim the key
    NeovimProcess.getInstance().input(key)
  }

  override def keyPressed(e: KeyEvent): Unit = {}
  override def keyReleased(e: KeyEvent): Unit = {}
}
