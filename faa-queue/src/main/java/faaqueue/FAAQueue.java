package faaqueue;

import kotlinx.atomicfu.*;

import static faaqueue.FAAQueue.Node.NODE_SIZE;


public class FAAQueue<T> implements Queue<T> {
    private static final Object DONE = new Object(); // Marker for the "DONE" slot state; to avoid memory leaks

    private AtomicRef<Node> head; // Head pointer, similarly to the Michael-Scott queue (but the first node is _not_ sentinel)
    private AtomicRef<Node> tail; // Tail pointer, similarly to the Michael-Scott queue

    public FAAQueue() {
        Node firstNode = new Node();
        head = new AtomicRef<>(firstNode);
        tail = new AtomicRef<>(firstNode);
    }

    @Override
    public void enqueue(T x) {
        while (true) {
            Node tail = this.tail.getValue();
            int enqIdx = tail.enqIdx.getAndIncrement();
            if (enqIdx >= NODE_SIZE) {
                Node newTail = new Node(x);
                if (tail.next.compareAndSet(null, newTail)) {
                    if (this.tail.compareAndSet(tail, newTail)) {
                        return;
                    }
                }
            } else {
                if (tail.data.get(enqIdx).compareAndSet(null, x)) {
                    return;
                }
            }
        }
    }

    @Override
    public T dequeue() {
        while (true) {
            Node head = this.head.getValue();
            if (head.isEmpty()) {
                Node headNext = head.next.getValue();
                if (headNext == null) {
                    return null;
                } else {
                    this.head.compareAndSet(head, headNext);
                }
            } else {
                int deaIdx = head.deqIdx.getAndIncrement();
                if (deaIdx >= NODE_SIZE) {
                    continue;
                }
                Object res = head.data.get(deaIdx).getAndSet(DONE);
                if (res == null) {
                    continue;
                }
                return (T) res;
            }
        }
    }

    static class Node {
        static final int NODE_SIZE = 2; // CHANGE ME FOR BENCHMARKING ONLY

        private AtomicRef<Node> next = new AtomicRef<>(null);
        private AtomicInt enqIdx = new AtomicInt(0); // index for the next enqueue operation
        private AtomicInt deqIdx = new AtomicInt(0); // index for the next dequeue operation
        private final AtomicArray<Object> data = new AtomicArray<>(NODE_SIZE);

        Node() {
        }

        Node(Object x) {
            this.enqIdx.setValue(1);
            this.data.get(0).setValue(x);
        }

        private boolean isEmpty() {
            return this.deqIdx.getValue() >= this.enqIdx.getValue() || this.deqIdx.getValue() >= NODE_SIZE;
        }
    }
}