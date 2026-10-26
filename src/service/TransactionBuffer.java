package service;

import model.Command;

/** Practical 10: fixed-size shared buffer, coordinated with wait() and notify(). */
public class TransactionBuffer {
    private final Command[] items;
    private int head = 0;
    private int tail = 0;
    private int count = 0;

    public TransactionBuffer(int capacity) {
        this.items = new Command[capacity];
    }

    public synchronized void put(Command command) throws InterruptedException {
        while (count == items.length) {
            wait(); // buffer full: wait for the consumer
        }
        items[tail] = command;
        tail = (tail + 1) % items.length;
        count++;
        notifyAll();
    }

    public synchronized Command take() throws InterruptedException {
        while (count == 0) {
            wait(); // buffer empty: wait for the producer
        }
        Command command = items[head];
        head = (head + 1) % items.length;
        count--;
        notifyAll();
        return command;
    }
}
