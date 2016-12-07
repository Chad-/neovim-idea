package xyz.aoei.idea.neovim

import java.beans.PropertyChangeListener
import javax.swing.JComponent

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.fileEditor._
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import xyz.aoei.idea.neovim.listener.NeovimInputEventListener

class NeovimEditor(val project: Project, val virtualFile: VirtualFile) extends UserDataHolderBase with FileEditor with TextEditor {
  private val providers = FileEditorProviderManager.getInstance()
    .getProviders(project, virtualFile)
    .filterNot(_.isInstanceOf[NeovimEditorProvider])

  private val doc = EditorFactory.getInstance().createDocument("")
  private val editor = EditorFactory.getInstance().createEditor(doc, project, virtualFile.getFileType, false)

  private val component = new NeovimEditorComponent

  component.addKeyListener(new NeovimInputEventListener())

  updateState()

  def updateState(): Unit = {
    component.setNeovimText(UIState.getInstance)
  }

  override def isModified: Boolean = false

  override def deselectNotify(): Unit = {}

  override def getName: String = "Neovim"

  override def selectNotify(): Unit = {}

  override def getCurrentLocation: FileEditorLocation = null

  override def getBackgroundHighlighter: BackgroundEditorHighlighter = null

  override def removePropertyChangeListener(propertyChangeListener: PropertyChangeListener): Unit = {}

  override def getComponent: JComponent = component

  override def getPreferredFocusedComponent: JComponent = component

  override def addPropertyChangeListener(propertyChangeListener: PropertyChangeListener): Unit = {}

  override def isValid: Boolean = true

  override def setState(fileEditorState: FileEditorState): Unit = {}

  override def getEditor: Editor = editor

  override def navigateTo(navigatable: Navigatable): Unit = {}

  override def canNavigateTo(navigatable: Navigatable): Boolean = true

  override def dispose(): Unit = {}
}
