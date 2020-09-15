import java.util.concurrent.atomic.*

class Solution(val env: Environment) : Lock<Solution.Node> {
    val tail: AtomicReference<Node> = AtomicReference(Node())
    // todo: необходимые поля (val, используем AtomicReference)

    override fun lock(): Node {
        val my = Node() // сделали узел
        val pred = tail.getAndSet(my)
        if (pred != null) {
            pred.next?.set(my)
            while (my.locked.get()) {

            }
        }
        // todo: алгоритм
        return my // вернули узел
    }

    override fun unlock(node: Node) {
        if (my?.get()?.next == null) {
            if (tail.compareAndSet(my?.get(), null)) {
                return
            } else {
                while (my?.get()?.next == null) {
                }
            }
        }
        my.get().locked.set(false)
    }

    class Node {
        val thread = Thread.currentThread() // запоминаем поток, которые создал узел
        val locked: AtomicReference<Boolean> = AtomicReference(false)
        var next: AtomicReference<Node>? = null
        // todo: необходимые поля (val, используем AtomicReference)
    }
}