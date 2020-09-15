package stack;

import java.util.Random;

import kotlinx.atomicfu.AtomicRef;

public class StackImpl implements Stack {
    private final int ELIMINATION_SIZE = 8;
    private final int NEIGHBOURS_RANGE = 2;
    private final int TRIES = 4;
    private final int WAIT_TIME = 8;

    // head pointer
    private AtomicRef<Node> head = new AtomicRef<>(null);
    // elimination array
    private EliminationArray eliminationArray = new EliminationArray(ELIMINATION_SIZE);

    private enum STATES {
        FREE, BUSY, DONE
    }

    private static class Node {
        final AtomicRef<Node> next;

        final int x;

        Node(int x, Node next) {
            this.next = new AtomicRef<>(next);
            this.x = x;
        }

    }

    private static class ArrayCell {
        STATES state;
        volatile int value;

        ArrayCell() {
            this.state = STATES.FREE;
            this.value = 0;
        }

        ArrayCell(STATES state, int value) {
            this.state = state;
            this.value = value;
        }
    }

    private static class EliminationArray {
        AtomicRef<ArrayCell>[] array;
        private Random random;

        EliminationArray(int length) {
            random = new Random();
            array = new AtomicRef[length];
            for (int i = 0; i < length; ++i) {
                array[i] = new AtomicRef<>(new ArrayCell());
            }
        }

        int getLeftNeighBour(int id, int range) {
            return Math.max(0, id - range);
        }

        int getRightNeighBour(int id, int range) {
            return Math.min(array.length, id + range);
        }

    }

    private boolean tryPush(int x) {
        int id = eliminationArray.random.nextInt(eliminationArray.array.length);
        int left = eliminationArray.getLeftNeighBour(id, NEIGHBOURS_RANGE);
        int right = eliminationArray.getRightNeighBour(id, NEIGHBOURS_RANGE);
        ArrayCell busyCell = new ArrayCell(STATES.BUSY, x);
        for (int i = left; i < right; ++i) {
            ArrayCell cellValue = eliminationArray.array[i].getValue();

            if (cellValue.state == STATES.FREE) {
                if (eliminationArray.array[id].compareAndSet(cellValue, busyCell)) {
                    return spinWait(id);
                }
            }
        }
        return false;
    }

    private boolean spinWait(int id) {
        ArrayCell freeCell = new ArrayCell();
        for (int j = 0; j < WAIT_TIME; ++j) {
            ArrayCell cell2 = eliminationArray.array[id].getValue();
            if (cell2.state == STATES.DONE) {
                if (eliminationArray.array[id].compareAndSet(
                        cell2, freeCell)) {
                    return true;
                }
            }
        }
        ArrayCell freeCellAccepted = eliminationArray.array[id].getAndSet(freeCell);
        return freeCellAccepted.state == STATES.DONE;
    }

    @Override
    public void push(int x) {
        //looking for partner
        if (!tryPush(x)) {
            //base version
            while (true) {
                Node H = head.getValue();
                Node newHead = new Node(x, H);
                if (head.compareAndSet(H, newHead)) {
                    return;
                }
            }
        }
    }

    @Override
    public int pop() {
        //looking for partner
        for (int i = 0; i < TRIES; ++i) {
            int id = eliminationArray.random.nextInt(eliminationArray.array.length);
            AtomicRef<ArrayCell> cell = eliminationArray.array[id];
            ArrayCell cellValue = cell.getValue();
            if (cellValue.state == STATES.BUSY) {
                if (cell.compareAndSet(cellValue, new ArrayCell(STATES.DONE, cellValue.value))) {
                    return cellValue.value;
                }
            }
        }

        //base version
        while (true) {
            Node H = head.getValue();
            if (H == null) {
                return Integer.MIN_VALUE;
            }
            if (head.compareAndSet(H, H.next.getValue())) {
                return H.x;
            }
        }
    }
}
