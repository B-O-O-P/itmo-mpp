package msqueue;

import kotlinx.atomicfu.AtomicRef;

public class MSQueue implements Queue {
    private AtomicRef<Node> head;
    private AtomicRef<Node> tail;
    private final Node dummy = new Node(0, null);

    public MSQueue() {
        this.head = new AtomicRef<>(dummy);
        this.tail = new AtomicRef<>(dummy);
    }

    @Override
    public void enqueue(int x) {
        Node newTail = new Node(x, null);
        while (true) {
            Node curTail = tail.getValue();
            Node tailNext = curTail.next.getValue();
            if (curTail == tail.getValue()) {
                if (tailNext != null) {
                    tail.compareAndSet(curTail, tailNext);
                } else {
                    if (curTail.next.compareAndSet(null, newTail)) {
                        tail.compareAndSet(curTail, newTail);
                        return;
                    }
                }
            }
        }
    }

    @Override
    public int dequeue() {
        while (true) {
            Node curHead = head.getValue();
            Node headNext = curHead.next.getValue();
            if (headNext == null) {
                return Integer.MIN_VALUE;
            }
            tail.compareAndSet(curHead, headNext);
            if (head.compareAndSet(curHead, headNext)) {
                return headNext.x;

            }
        }

    }

    @Override
    public int peek() {
        while (true) {
            Node curHead = head.getValue();
            Node curTail = tail.getValue();
            Node next = curHead.next.getValue();
            if (curHead == curTail)
                return Integer.MIN_VALUE;
            if (head.compareAndSet(curHead, curHead.next.getValue()))
                return next.x;
        }
    }

    private class Node {
        final int x;
        AtomicRef<Node> next;

        Node(int x, Node node) {
            this.x = x;
            this.next = new AtomicRef<>(node);
        }
    }
}
