package xyz.aoei.idea.neovim.ui

import java.awt.BorderLayout
import javax.swing.border.EmptyBorder
import javax.swing.{DefaultListModel, JPanel, ListSelectionModel}

import com.intellij.codeInsight.hint.HintManagerImpl
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.{Editor, EditorFactory}
import com.intellij.ui.{LightweightHint, ListScrollingUtil, ScrollPaneFactory, ScrollingUtil}
import com.intellij.ui.components.JBList

class Popupmenu() extends LightweightHint(new JPanel(new BorderLayout())) {
  setForceShowAsPopup(true)

  val list = new JBList[String](new DefaultListModel())
  list.setFocusable(false)
  list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

  val scrollPane = ScrollPaneFactory.createScrollPane(list)
  scrollPane.setViewportBorder(new EmptyBorder(0,0,0,0))
  scrollPane.setBorder(null)

  getComponent.add(scrollPane)

  def setItems(items: List[String]): Unit = {
    list.setListData(items.toArray)
  }

  def setSelected(selected: Int): Unit = {
    list.setSelectedIndex(selected)
    ScrollingUtil.selectItem(list, selected)
  }

  def show(): Unit = {
    ApplicationManager.getApplication.assertIsDispatchThread()

    val editor = EditorFactory.getInstance().getAllEditors.head

    val hintManager = HintManagerImpl.getInstanceImpl
    val pos = editor.getCaretModel.getLogicalPosition
    val p = hintManager.getHintPosition(this, editor, 0)
    hintManager.showEditorHint(this, editor, p, 0, 0, false)
  }
}
