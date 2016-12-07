package xyz.aoei.idea.neovim

import com.intellij.openapi.fileEditor._
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class NeovimEditorProvider extends FileEditorProvider {
  override def getEditorTypeId: String = "neovim-editor"

  override def getPolicy: FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR

  override def createEditor(project: Project, file: VirtualFile): FileEditor = {
//    val doc = FileDocumentManager.getInstance().getDocument(file)
//    FileEditorManager.getInstance(project).openFile(file, false).head
    new NeovimEditor(project, file)
  }

  override def accept(project: Project, file: VirtualFile): Boolean = true
}
