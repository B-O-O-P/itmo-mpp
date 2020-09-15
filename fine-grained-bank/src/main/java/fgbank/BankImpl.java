package fgbank;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Bank implementation.
 *
 * <p>:TODO: This implementation has to be made thread-safe.
 */
public class BankImpl implements Bank {
    /**
     * An array of accounts by index.
     */
    private final Account[] accounts;

    /**
     * Creates new bank instance.
     *
     * @param n the number of accounts (numbered from 0 to n-1).
     */
    public BankImpl(int n) {
        accounts = new Account[n];
        for (int i = 0; i < n; i++) {
            accounts[i] = new Account();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getNumberOfAccounts() {
        return accounts.length;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getAmount(int index) {
        accounts[index].lock.lock();
        try {
            return accounts[index].amount;
        } finally {
            accounts[index].lock.unlock();
        }
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long getTotalAmount() {
        long sum = 0;

        for (Account account : accounts) {
            account.lock.lock();
        }
        try {
            for (Account account : accounts) {
                sum += account.amount;
            }
        } finally {
            for (Account account : accounts) {
                account.lock.unlock();
            }
        }

        return sum;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long deposit(final int index, final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount);
        }

        accounts[index].lock.lock();
        long local;
        try {
            Account account = accounts[index];
            if (amount > MAX_AMOUNT || account.amount + amount > MAX_AMOUNT) {
                throw new IllegalStateException("Overflow");
            }

            account.amount += amount;
            local = account.amount;
        } finally {
            accounts[index].lock.unlock();
        }

        return local;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public long withdraw(final int index, final long amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Invalid amount: " + amount + "index is " + index);
        }

        accounts[index].lock.lock();
        long local;
        try {
            Account account = accounts[index];
            if (account.amount - amount < 0) {
                throw new IllegalStateException("Underflow");
            }

            account.amount -= amount;
            local = account.amount;
        } finally {
            accounts[index].lock.unlock();
        }

        return local;
    }

    /**
     * {@inheritDoc}
     * <p>:TODO: This method has to be made thread-safe.
     */
    @Override
    public void transfer(int fromIndex, int toIndex, long amount) {
        if (amount <= 0)
            throw new IllegalArgumentException("Invalid amount: " + amount);
        if (fromIndex == toIndex)
            throw new IllegalArgumentException("fromIndex == toIndex");
        if (fromIndex < toIndex) {
            accounts[fromIndex].lock.lock();
            accounts[toIndex].lock.lock();
        } else {
            accounts[toIndex].lock.lock();
            accounts[fromIndex].lock.lock();
        }
        try {
            Account to = accounts[toIndex];
            try {
                Account from = accounts[fromIndex];
                if (amount > from.amount)
                    throw new IllegalStateException("Underflow");
                else if (amount > MAX_AMOUNT || to.amount + amount > MAX_AMOUNT)
                    throw new IllegalStateException("Overflow");
                from.amount -= amount;
            } finally {
                accounts[fromIndex].lock.unlock();
            }
            to.amount += amount;
        } finally {
            accounts[toIndex].lock.unlock();
        }
    }

    /**
     * Private account data structure.
     */
    private static class Account {
        /**
         * Amount of funds in this account.
         */
        long amount;
        Lock lock = new ReentrantLock();
    }
}
