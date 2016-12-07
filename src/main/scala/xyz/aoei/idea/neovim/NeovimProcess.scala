package xyz.aoei.idea.neovim

import java.net.Socket

import xyz.aoei.neovim.{Neovim => Nvim}

object NeovimProcess {
  val nvim = {
    val socket = new Socket("127.0.0.1", 8888)
    new Nvim(socket.getInputStream, socket.getOutputStream)
  }

  def getInstance(): Nvim = nvim
}
