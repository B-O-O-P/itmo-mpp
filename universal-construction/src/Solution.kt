class Solution : AtomicCounter {
    // объявите здесь нужные вам поля
    private val root: Node = Node(0)
    private val last: ThreadLocal<Node> = ThreadLocal.withInitial { root }

    override fun getAndAdd(x: Int): Int {
        while (true) {
            val old = last.get().value
            val res = old + x
            val node = Node(res)
            val lastNode = last.get().next.decide(node)
            last.set(lastNode)
            if (lastNode == node) {
                return old
            }
        }
    }

    // вам наверняка потребуется дополнительный класс
    class Node(val value: Int, val next: Consensus<Node> = Consensus())
}
