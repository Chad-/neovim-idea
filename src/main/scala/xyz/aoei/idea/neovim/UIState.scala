package xyz.aoei.idea.neovim

case class ScrollRegion(top: Int, bot: Int, left: Int, right: Int)
case class Attributes(foreground: Int = -1, background: Int = -1, special: Int = -1,
                      italic: Boolean = false, bold: Boolean = false,
                      underline: Boolean = false, undercurl: Boolean = false)
case class Text(text: Char = ' ', attr: Attributes = Attributes())

case class UIState(cursor: (Int, Int) = (0,0),
                   grid: Array[Array[Text]] = Array(),
                   scrollRegion: ScrollRegion = ScrollRegion(0,0,0,0),
                   foreground: Int = 0,
                   background: Int = 0,
                   special: Int = 0,
                   highlightSet: Attributes = Attributes(),
                   mode: String = "normal",
                   dirty: Boolean = true
                  ) {
  override def toString: String = grid.foldLeft("")(_+_.foldLeft("")(_+_.text)+"\n").init
}

object UIState {
  private var uiState = UIState()

  def getInstance: UIState = uiState

  def updateState(args: List[Any]): Unit = {
    val updates = args.map(x => x.asInstanceOf[List[Any]])
      .map(x => (x.head, x.tail.asInstanceOf[List[List[Any]]].flatten))

    var dirty = false

    updates.foreach {
      case ("resize", List(width: Int, height: Int)) =>
        uiState = uiState.copy(grid = Array.ofDim(height, width))

      case ("clear", _) =>
        val attr = Attributes(
          foreground = uiState.foreground,
          background = uiState.background,
          special = uiState.special
        )

        val grid = Array.fill(uiState.grid.length)(Array.fill(uiState.grid.head.length)(Text(' ', attr)))
        uiState = uiState.copy(grid = grid)
        dirty = true

      case ("eol_clear", _) =>
        val newGrid = uiState.grid
        val cursor = uiState.cursor

        val attr = Attributes(
          foreground = uiState.foreground,
          background = uiState.background,
          special = uiState.special
        )

        for (col <- cursor._2 until newGrid.head.length)
          newGrid(cursor._1)(col) = Text(' ', attr)

        uiState = uiState.copy(grid = newGrid)
        dirty = true

      case ("cursor_goto", List(row: Int, col: Int)) =>
        uiState = uiState.copy(cursor = (row, col))
        dirty = true

      case ("update_fg", List(color: Int)) =>
        uiState = uiState.copy(foreground = color)

      case ("update_bg", List(color: Int)) =>
        uiState = uiState.copy(background = color)

      case ("update_sp", List(color: Int)) =>
        uiState = uiState.copy(special = color)

      // TODO: Find out why an extra parameter is placed infront of map
      case ("highlight_set", List(_, map: Map[String, Any])) =>
        uiState = highlightSet(uiState, map)
      case ("highlight_set", List(map: Map[String, Any])) =>
        uiState = highlightSet(uiState, map)

      case ("put", textList: List[_]) =>
        val text = textList.asInstanceOf[List[String]]
        val newGrid = uiState.grid.map(_.clone)
        val cursor = uiState.cursor

        def doPut(c: (Int, Int), t: List[String]): (Int, Int) = {
          if (t.nonEmpty) {
            newGrid(c._1)(c._2) = Text(t.head.head, uiState.highlightSet)
            doPut((c._1, c._2+1), t.tail)
          }
          else c
        }

        val newCursor = doPut(cursor, text)

        uiState = uiState.copy(cursor = newCursor, grid = newGrid)
        dirty = true

      case ("set_scroll_region", List(top: Int, bot: Int, left: Int, right: Int)) =>
        uiState = uiState.copy(scrollRegion = ScrollRegion(top, bot, left, right))

      case ("scroll", List(count: Int)) =>
        val newGrid = uiState.grid.map(_.clone)

        val sr = uiState.scrollRegion
        for {
          col <- sr.left to sr.right
          row <- sr.top to sr.bot
        } yield {
          val srcRow = row + count
          if (srcRow >= sr.top && srcRow <= sr.bot) {
            newGrid(row)(col) = uiState.grid(row+count)(col)
          } else {
            val attr = Attributes(
              foreground = uiState.foreground,
              background = uiState.background,
              special = uiState.special
            )

            newGrid(row)(col) = Text(' ', attr)
          }
        }

        uiState = uiState.copy(grid = newGrid)
        dirty = true

      case ("set_title", List(title)) =>
      case ("mouse_on", _) =>
      case ("mouse_off", _) =>
      case ("busy_on", _) =>
      case ("busy_off", _) =>
      case ("suspend", _) =>
      case ("bell", _) =>
      case ("visual_bell", _) =>
      case ("update_menu", _) =>
      case ("mode_change", List(mode: String)) =>
        uiState = uiState.copy(mode = mode)
        dirty = true

      case ("popupmenu_show", List(items: List[Any], selected: Int, row: Int, col: Int)) =>
//        popupmenuShow(items, selected, row, col)
      case ("popupmenu_select", List(selected: Int)) =>
//        popupmenuSelect(selected)
      case ("popupmenu_hide", _) =>
//        popupmenuHide()
      case x => println("Unhandled ui update: " + x)
    }

    uiState = uiState.copy(dirty = dirty)
  }

  def highlightSet(uiState: UIState, map: Map[String, Any]): UIState = {
    val fg = map.getOrElse("foreground", uiState.foreground).asInstanceOf[Int]
    val bg = map.getOrElse("background", uiState.background).asInstanceOf[Int]
    val reverse = map.getOrElse("reverse", false).asInstanceOf[Boolean]

    val attributes = Attributes(
      if (reverse) bg else fg,
      if (reverse) fg else bg,
      map.getOrElse("special", uiState.special).asInstanceOf[Int],
      map.getOrElse("italic", false).asInstanceOf[Boolean],
      map.getOrElse("bold", false).asInstanceOf[Boolean],
      map.getOrElse("underline", false).asInstanceOf[Boolean],
      map.getOrElse("undercurl", false).asInstanceOf[Boolean]
    )

    uiState.copy(highlightSet = attributes)
  }
}
