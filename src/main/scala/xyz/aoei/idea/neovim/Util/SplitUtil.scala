package xyz.aoei.idea.neovim.Util

import xyz.aoei.neovim.Window

object SplitUtil {
  private type Split = (Int, Int)

  sealed trait Node {val split: Split }
  case class Branch(dir: Int, split: Split, l: Node, r: Node) extends Node
  case class Leaf(split: Split, window: Window) extends Node

  def getTree(splits: List[(Split, Window)]): Node = {
    def find(splits: List[Node]): (Node, List[Node]) = {
      if (splits.length <= 1) (splits.head, Nil)
      else {
        val a = splits.head
        val b = splits.tail.head

        val r = find(splits.tail)

        if (a.split._1 == b.split._1)
          (Branch(1, a.split, a, r._1), r._2)
        else if (a.split._2 == b.split._2)
          (Branch(0, a.split, a, r._1), r._2)
        else
          (a, splits.tail)
      }
    }

    val leafs: List[Node] = splits.map(x => Leaf(x._1, x._2))

    var result: Node = leafs.head
    var remaining: List[Node] = leafs.tail

    do {
      val r = find(result :: remaining)

      result = r._1
      remaining = r._2
    } while(remaining.nonEmpty)

    result
  }
}
