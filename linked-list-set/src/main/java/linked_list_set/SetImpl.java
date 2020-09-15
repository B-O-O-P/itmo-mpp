package linked_list_set;

import kotlinx.atomicfu.AtomicRef;

public class SetImpl implements Set {
    private final Node dummy = new Node(Integer.MAX_VALUE, new Pair(null, false));
    private final AtomicRef<Node> head = new AtomicRef<>(new Node(Integer.MIN_VALUE, new Pair(dummy, false)));

    private static class Node {
        AtomicRef<Pair> nr;
        int x;


        Node(int x, Pair nr) {
            this.nr = new AtomicRef<>(nr);
            this.x = x;
        }
    }

    private static class Window {
        Node cur, next;

    }

    private static class Pair {
        final Node next;
        final boolean removed;


        private Pair(Node reference, boolean mark) {
            this.next = reference;
            this.removed = mark;
        }

        static Pair of(Node reference, boolean removed) {
            return new Pair(reference, removed);
        }
    }

    /**
     * Returns the {@link Window}, where cur.x < x <= next.x
     */
    private Window findWindow(int x) {
        while (true) {
            Window w = new Window();
            w.cur = head.getValue();
            w.next = w.cur.nr.getValue().next;
            while (w.next.x < x) {
                Pair nextNR = w.next.nr.getValue();
                Pair curNR = w.cur.nr.getValue();
                if (curNR.removed || curNR.next != w.next) {
                    break;
                }

                if (nextNR.removed) {
                    if (!w.cur.nr.compareAndSet(nextNR, Pair.of(nextNR.next, false))) {
                        continue;
                    }
                    w.next = nextNR.next;
                } else {
                    w.cur = w.next;
                    w.next = w.cur.nr.getValue().next;
                }
            }
            if (validate(w, x)) {
                return w;
            }
        }
    }

    private boolean validate(Window w, int x) {
        Pair nextNR = w.next.nr.getValue();
        Pair curNR = w.cur.nr.getValue();
        if (curNR.removed || curNR.next != w.next) {
            return false;
        }
        if (nextNR.removed) {
            return false;
        }
        return w.next.x >= x;
    }

    @Override
    public boolean add(int x) {
        while (true) {
            Window w = findWindow(x);
            Pair cur = w.cur.nr.getValue();
            Node next = cur.next;

            if (w.next.x == x) {
                return false;
            }

            if (next == w.next && !cur.removed) {
                Node newNode = new Node(x, Pair.of(next, false));
                if (w.cur.nr.compareAndSet(cur, Pair.of(newNode, false))) {
                    return true;
                }
            }
        }
    }

    @Override
    public boolean remove(int x) {
        while (true) {
            Window w = findWindow(x);
            Pair cur = w.cur.nr.getValue();
            Node next = cur.next;

            Pair nextNR = w.next.nr.getValue();
            Node nextNext = w.next.nr.getValue().next;

            if (w.next.x != x) {
                return false;
            }

            if (!nextNR.removed) {
                if (next.nr.compareAndSet(nextNR, Pair.of(nextNext, true))) {
                    w.cur.nr.compareAndSet(cur, Pair.of(nextNext, false));
                    return true;
                }
            }
        }
    }

    @Override
    public boolean contains(int x) {
        Window w = findWindow(x);
        return w.next.x == x;
    }
}