package xyz.aoei.idea.neovim

import java.awt.BorderLayout
import java.awt.event.{KeyEvent, KeyListener}
import java.beans.{PropertyChangeListener, PropertyChangeSupport}
import javax.swing.{JComponent, JPanel, JScrollPane}

import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.ex.ActionManagerEx
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.openapi.fileEditor._
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.pom.Navigatable
import com.intellij.util.ui.UIUtil
import xyz.aoei.idea.neovim.listener.NeovimInputEventListener

class NeovimEditor(val project: Project, val virtualFile: VirtualFile) extends UserDataHolderBase with FileEditor with TextEditor {
  private val providers = FileEditorProviderManager.getInstance()
    .getProviders(project, virtualFile)
    .filterNot(_.isInstanceOf[NeovimEditorProvider])

  private val doc = EditorFactory.getInstance().createDocument("")
  private val editor = EditorFactory.getInstance().createEditor(doc, project, virtualFile.getFileType, false)


  private val component = new NeovimEditorComponent
  ShortcutKeyAction.instance.registerCustomShortcutSet(new CustomShortcutSet(ShortcutKeyAction.shortcuts:_*), component)

  private val panel = new JPanel(new BorderLayout())

//  scrollPane.setViewportView(component)

  private val noWrapPanel = new JPanel(new BorderLayout())
  noWrapPanel.add(component)

  private val scrollPane = new JScrollPane()
  scrollPane.setViewportView(noWrapPanel)

  panel.add(scrollPane)

  private val propertyChangeSupport = new PropertyChangeSupport(this)

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

  override def getComponent: JComponent = panel

  override def getPreferredFocusedComponent: JComponent = panel

  override def addPropertyChangeListener(propertyChangeListener: PropertyChangeListener): Unit =
    propertyChangeSupport.addPropertyChangeListener(propertyChangeListener)

  override def removePropertyChangeListener(propertyChangeListener: PropertyChangeListener): Unit =
    propertyChangeSupport.removePropertyChangeListener(propertyChangeListener)

  override def isValid: Boolean = true

  override def setState(fileEditorState: FileEditorState): Unit = {}

  override def getEditor: Editor = null

  override def navigateTo(navigatable: Navigatable): Unit = {}

  override def canNavigateTo(navigatable: Navigatable): Boolean = true

  override def dispose(): Unit = {}
}
