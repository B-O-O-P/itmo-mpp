package dijkstra

import java.util.*
import kotlin.random.Random
import java.util.concurrent.Phaser
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.Comparator
import kotlin.concurrent.thread

private val NODE_DISTANCE_COMPARATOR = Comparator<Node> { o1, o2 -> o1!!.distance.compareTo(o2!!.distance) }


// Returns `Integer.MAX_VALUE` if a path has not been found.
fun shortestPathParallel(start: Node) {
    val workers = Runtime.getRuntime().availableProcessors()
    start.distance = 0
    val q = MultiQueue(workers, NODE_DISTANCE_COMPARATOR)
    q.add(start)
    val onFinish = Phaser(workers + 1) // `arrive()` should be invoked at the end by each worker
    val activeNodes = AtomicInteger()
    activeNodes.incrementAndGet()
    repeat(workers) {
        thread {
            while (activeNodes.get() > 0) {
                val cur: Node = q.poll() ?: continue
                for (e in cur.outgoingEdges) {
                    while (true) {
                        val toDistance = e.to.distance
                        val updateDistance = cur.distance + e.weight
                        if (toDistance > updateDistance) {
                            if (e.to.casDistance(toDistance, updateDistance)) {
                                q.add(e.to)
                                activeNodes.incrementAndGet()
                                break
                            } else {
                                continue
                            }
                        } else {
                            break
                        }
                    }
                }
                activeNodes.decrementAndGet()
            }
            onFinish.arrive()
        }
    }
    onFinish.arriveAndAwaitAdvance()
}

class MultiQueue<T>(n: Int, private val comparator: Comparator<T>) {
    private val numberOfQueues: Int = 2 * n
    private val queues = Array(numberOfQueues) { PQ(comparator) }

    fun add(element: T) {
        while (true) {
            val index = getRandomIndex()
            val randomQueue = queues[index]

            if (randomQueue.lock.tryLock()) {
                try {
                    randomQueue.priorityQueue.add(element)
                    return
                } finally {
                    randomQueue.lock.unlock()
                }
            }
        }
    }

    fun poll(): T? {
        while (true) {
            val firstIndex = getRandomIndex()
            val secondIndex = getRandomIndex()
            val firstRandomQueue = queues[firstIndex]
            val secondRandomQueue = queues[secondIndex]

            if (firstRandomQueue.lock.tryLock()) {
                try {
                    if (secondRandomQueue.lock.tryLock()) {
                        try {
                            val peekFirst = firstRandomQueue.priorityQueue.peek()
                            val peekSecond = secondRandomQueue.priorityQueue.peek()
                            return when {
                                peekFirst == null && peekSecond == null -> null
                                peekFirst != null && peekSecond == null -> firstRandomQueue.priorityQueue.poll()
                                peekFirst == null && peekSecond != null -> secondRandomQueue.priorityQueue.poll()
                                else -> if (comparator.compare(peekFirst, peekSecond) > 0) {
                                    secondRandomQueue.priorityQueue.poll()
                                } else {
                                    firstRandomQueue.priorityQueue.poll()
                                }
                            }
                        } finally {
                            secondRandomQueue.lock.unlock()
                        }
                    } else return firstRandomQueue.priorityQueue.poll()
                } finally {
                    firstRandomQueue.lock.unlock()
                }
            }
        }
    }

    private fun getRandomIndex(): Int {
        return Random.nextInt(numberOfQueues)
    }
}

class PQ<T>(comparator: Comparator<T>) {
    val priorityQueue: PriorityQueue<T> = PriorityQueue(comparator)
    val lock: Lock = ReentrantLock()
}