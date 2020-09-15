import java.util.concurrent.atomic.AtomicReference
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

const val RETRY: Int = 100

class SynchronousQueueMS<E> : SynchronousQueue<E> {
    private enum class Type {
        SENDER, RECEIVER
    }

    private inner class Node(
        type: Type? = null,
        element: E? = null
    ) {
        val next: AtomicReference<Node?> = AtomicReference(null)
        val type: AtomicReference<Type?> = AtomicReference(type)
        val element: AtomicReference<E?> = AtomicReference(element)
        val continuation: AtomicReference<Continuation<Any?>?> = AtomicReference(null)

        fun isSender() = type.get() == Type.SENDER
        fun isReceiver() = type.get() == Type.RECEIVER
    }

    private val dummy = Node()
    private var head: AtomicReference<Node> = AtomicReference(dummy)
    private var tail: AtomicReference<Node> = AtomicReference(dummy)

    private suspend fun enqueueAndSuspend(tail:  Node, node: Node): Boolean {
        val res = suspendCoroutine<Any?> sc@{ cont ->
            node.continuation.set(cont)
            if (!tail.next.compareAndSet(null, node)) {
                this.tail.compareAndSet(tail, tail.next.get())
                cont.resume(RETRY)
                return@sc
            } else {
                this.tail.compareAndSet(tail, node)
            }
        }
        return res != RETRY
    }


    private fun dequeueAndResume(head: Node, element: E?): Boolean {
        val next = head.next.get() ?: return false

        return if (this.head.compareAndSet(head, next)) {
            if (element != null) {
                next.element.set(element)
            }
            next.continuation.get()?.resume(null) ?: throw NullPointerException()
            true
        } else {
            false
        }
    }

    override suspend fun send(element: E) {
        while (true) {
            val head = this.head.get()
            val tail = this.tail.get()

            val node = Node(Type.SENDER, element)

            if (tail == this.tail.get()) {
                if (tail == head || tail.isSender()) {
                    if (enqueueAndSuspend(tail, node))
                        return
                } else {
                    if (tail == this.tail.get()) {
                        if (dequeueAndResume(head, element))
                            return
                    }
                }
            }
        }
    }

    override suspend fun receive(): E {
        while (true) {
            val head = this.head.get()
            val tail = this.tail.get()

            val node = Node(Type.RECEIVER)

            if (tail == this.tail.get()) {
                if (tail == head || tail.isReceiver()) {
                    if (enqueueAndSuspend(tail, node)) {
                        return node.element.get() ?: throw IllegalArgumentException()
                    }
                } else {
                    if (tail == this.tail.get()) {
                        if (dequeueAndResume(head, null)) {
                            return head.next.get()?.element?.get() ?: throw NullPointerException()
                        }
                    }
                }
            }
        }
    }
}