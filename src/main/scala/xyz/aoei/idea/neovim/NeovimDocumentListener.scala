package xyz.aoei.idea.neovim

import com.intellij.openapi.editor.event.{DocumentEvent, DocumentListener}
import com.intellij.openapi.fileEditor.FileDocumentManager

class NeovimDocumentListener(fileChangeHandler: (String) => Unit) extends DocumentListener {
  override def documentChanged(documentEvent: DocumentEvent): Unit = {
    println(documentEvent.getDocument.getLineCount)
  }

  override def beforeDocumentChange(documentEvent: DocumentEvent): Unit = {
    val doc = documentEvent.getDocument
    val file = FileDocumentManager.getInstance().getFile(doc)

    fileChangeHandler(file.getPath)
  }
}
