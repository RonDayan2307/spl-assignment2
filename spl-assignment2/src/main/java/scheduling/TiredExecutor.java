package scheduling;

import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

public class TiredExecutor {

    private final TiredThread[] workers;
    private final PriorityBlockingQueue<TiredThread> idleMinHeap = new PriorityBlockingQueue<>();
    private final AtomicInteger inFlight = new AtomicInteger(0);

    public TiredExecutor(int numThreads) {
        // TODO
        // creating workers
        this.workers = new TiredThread[numThreads];
        for (int i = 0; i < numThreads; i++) {
            double fatigueFactor = 0.5 + (Math.random());
            TiredThread worker = new TiredThread(i, fatigueFactor);
            this.workers[i] = worker;
            worker.start();
            idleMinHeap.add(worker);
        }
    }

    public void submit(Runnable task) {
        // TODO
        try {
            TiredThread selectedWorker = null;

            synchronized (this) {
                while (true) {
                    if (workers.length < 2) {
                        while (idleMinHeap.isEmpty()) {
                            this.wait();
                        }
                        selectedWorker = idleMinHeap.poll();
                        break;
                    }

                    if (idleMinHeap.size() == workers.length) {
                        selectedWorker = idleMinHeap.poll();
                        break;
                    }

                    double totalFatigue = 0;
                    for (TiredThread w : workers) {
                        totalFatigue += w.getFatigue();
                    }
                    double averageFatigue = totalFatigue / workers.length;
                    TiredThread bestAvailable = idleMinHeap.peek();

                    if (bestAvailable == null) {
                        this.wait();
                    } else if (bestAvailable.getFatigue() > averageFatigue) {
                        this.wait();
                    } else {
                        selectedWorker = idleMinHeap.poll();
                        break;
                    }
                }
            }

            inFlight.incrementAndGet();
            final TiredThread finalWorker = selectedWorker;

            Runnable wrapper = () -> {
                try {
                    task.run();
                } finally {
                    inFlight.decrementAndGet();
                    synchronized (this) {
                        idleMinHeap.add(finalWorker);
                        this.notifyAll(); 
                    }
                }
            };

            selectedWorker.newTask(wrapper);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void submitAll(Iterable<Runnable> tasks) {
        // TODO: submit tasks one by one and wait until all finish
        for (Runnable task : tasks) {
            submit(task);
        }
        // waiting for all tasks to finish
        synchronized (this) {
            while (inFlight.get() > 0) {
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    public void shutdown() throws InterruptedException {
        // TODO
        for (TiredThread worker : workers) {
            worker.shutdown();
        }
        for (TiredThread worker : workers) {
            worker.join();
        }
    }

    public synchronized String getWorkerReport() {
        // TODO: return readable statistics for each worker
        String report = "Worker Report:\n";
        for (TiredThread worker : this.workers) {
            report = report + "Worker " + worker.getWorkerId() + " Used: " + worker.getTimeUsed() + " Idle: " + 
                     worker.getTimeIdle() + " Fatigue: " + worker.getFatigue() + "\n";
        }
        return report;
    }
}
