import java.util.concurrent.atomic.*
import kotlin.coroutines.*

class BlockingStackImpl<E> : BlockingStack<E> {

    // ==========================
    // Segment Queue Synchronizer
    // ==========================

    private inner class SegmentQueue {
        private val size = 2
        private val dummyNode = Node()
        private val head = AtomicReference<Node>(dummyNode)
        private val tail = AtomicReference<Node>(dummyNode)

        private inner class Node() {
            val next = AtomicReference<Node>(null)
            val enqIdx = AtomicLong(0)
            val deqIdx = AtomicLong(0)
            val data = AtomicReferenceArray<Any>(size)

            constructor(x: Any?) :this() {
                if (x != null) {
                    data.set(0, x)
                    enqIdx.set(1)
                }
            }

            fun isEmpty(): Boolean {
                val deqIdx = deqIdx.get()
                val enqIdx = enqIdx.get()
                return deqIdx >= enqIdx || deqIdx >= size
            }
        }

        fun enqueue(x: Continuation<E>) {
            while (true) {
                val curTail = tail.get()
                val enqIdx = curTail.enqIdx.getAndIncrement()
                if (enqIdx >= size) {
                    val newTail = Node(x)
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail)
                        break
                    } else {
                        tail.compareAndSet(curTail, curTail.next.get())
                    }
                } else if (curTail.data.compareAndSet(enqIdx.toInt(), null, x)) {
                    break
                }
            }
        }

        fun dequeue(): Continuation<E> {
            while (true) {
                val first = head.get()
                val next = first.next.get()
                if (first.isEmpty() && next != null) {
                    head.compareAndSet(first, next)
                } else {
                    val deqIdx = first.deqIdx.getAndIncrement()
                    if (deqIdx >= size) {
                        continue
                    }
                    val res = first.data.getAndSet(deqIdx.toInt(), DONE)
                    if (res == null) {
                        continue
                    }
                    return res as Continuation<E>
                }
            }
        }
    }

    private val queue = SegmentQueue()

    private suspend fun suspend(): E {
        return suspendCoroutine { cont -> queue.enqueue(cont) }
    }

    private fun resume(element: E) {
        queue.dequeue().resume(element)
    }

    // ==============
    // Blocking Stack
    // ==============

    private val head = AtomicReference<Node<E?>?>()
    private val elements = AtomicInteger()

    override fun push(element: E) {
        val elements = this.elements.getAndIncrement()
        if (elements >= 0) {
            while (true) {
                val curHead = head.get()
                if (curHead != null && curHead.element == SUSPENDED) {
                    val cont = curHead.cont
                    if (head.compareAndSet(curHead, null)) {
                        cont?.resume(element)
                        break
                    }
                } else {
                    val newHead = Node(element, curHead, null)
                    if (head.compareAndSet(curHead, newHead)) {
                        return
                    }
                }
            }
        } else {
            resume(element)
        }
    }

    override suspend fun pop(): E {
        val elements = this.elements.getAndDecrement()
        if (elements > 0) {
            while (true) {
                val curHead = head.get()
                if (curHead == null) {
                    val result = suspendCoroutine<E?> { cont ->
                        val newHead = Node(SUSPENDED, null, cont)
                        if (!head.compareAndSet(null, newHead)) {
                            cont.resume(null)
                        }
                    }
                    if (result != null) {
                        return result
                    }
                } else {
                    val elem = curHead.element
                    if (head.compareAndSet(curHead, curHead.next.get())) {
                        return elem as E
                    }
                }
            }
        } else {
            return suspend()
        }
    }
}

private class Node<E>(val element: Any?, nextNode: Node<E>?, val cont: Continuation<E>?) {
    val next = AtomicReference<Node<E>>(nextNode)
}

private val SUSPENDED = Any()
private val DONE = Any()
