package xyz.aoei.idea.neovim

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.{FileEditorManager, FileEditorManagerListener}
import com.intellij.openapi.project.Project
import xyz.aoei.idea.neovim.listener._
import xyz.aoei.idea.neovim.ui.Popupmenu
import xyz.aoei.idea.neovim.util.Util
import xyz.aoei.neovim.{Neovim => Nvim}

class Neovim(project: Project) extends ProjectComponent {
  val nvim = NeovimProcess.getInstance()

  nvim.onNotification((method, args) => method match {
    case "redraw" => {
      UIState.updateState(args)

      if (UIState.getInstance.dirty) {
        val documents = FileEditorManager.getInstance(project)
          .getAllEditors
          .filter(_.isInstanceOf[NeovimEditor])
          .map(_.asInstanceOf[NeovimEditor])
          //        .map(_.getEditor.getDocument)
          .foreach(_.updateState())
      }
    }
    case x => println("Notification: " + x)
  })

  override def getComponentName: String = "Neovim"

  override def projectOpened(): Unit = {}
  override def projectClosed(): Unit = {}

  override def initComponent(): Unit = {
    EditorFactory.getInstance().addEditorFactoryListener(new NeovimEditorFactoryListener, new Disposable(){
      override def dispose(): Unit = {}
    })

    val messageBus = project.getMessageBus.connect()
    messageBus.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, new NeovimFileEditorManagerListener())
    messageBus.subscribe(InputEventListener.INPUT_EVENT, new NeovimInputEventListener())

    nvim.uiAttach(100, 30, Map(
      "rgb" -> true,
      "popupmenu_external" -> true
    ))
    nvim.setCurrentDir(project.getBaseDir.getPath)


  }

  override def disposeComponent(): Unit = {
    nvim.uiDetach()
//    nvim.quit()
  }

  val popupmenu = new Popupmenu
  private def popupmenuShow(items: List[Any], selected: Int, row: Int, col: Int): Unit = {
    val data = items.asInstanceOf[List[List[String]]].map(_.mkString("\t"))
    popupmenu.setItems(data)
    popupmenu.setSelected(selected)

    Util.invokeLater(() => {
      popupmenu.show()
    })
  }

  private def popupmenuSelect(selected: Int): Unit = {
    popupmenu.setSelected(selected)
  }

  private def popupmenuHide(): Unit = {
    Util.invokeLater(() => {
      popupmenu.hide()
    })
  }
}
