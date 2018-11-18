package my_thread_pool;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Supplier;

public class MyThreadPoolImpl implements MyThreadPool {
    private final Queue<LightFutureImpl> taskQueue = new LinkedList<>();
    private final List<Thread> workers = new LinkedList<>();

    public MyThreadPoolImpl(int nThreads) {
        for (int i = 0; i < nThreads; i++) {
            workers.add(new Thread(new TaskWorker()));
        }
        for (Thread worker : workers) {
            worker.start();
        }
    }

    @Override
    public <T> LightFuture<T> submit(Supplier<T> supplier) {

        LightFutureImpl<T> lightFuture = new LightFutureImpl<>(supplier, this);
        synchronized (taskQueue) {
            while (!taskQueue.offer(lightFuture)) ;
            taskQueue.notify();

        }

        return lightFuture;
    }

    void submit(LightFutureImpl lightFuture) {
        synchronized (taskQueue) {
            while (!taskQueue.offer(lightFuture)) ;
            taskQueue.notify();

        }
    }


    /**
     * Метод shutdown должен завершить работу потоков.
     * Для того, чтобы прервать работу потока рекомендуется пользоваться методом Thread.interrupt()
     */

    @Override
    public void shutdown() {
        for (Thread thread : workers) {
            thread.interrupt();
        }
    }

    private final class TaskWorker implements Runnable {
        @Override
        public void run() {
            while (true) {
                try {
                    consumeTask();
                } catch (InterruptedException e) {
                    // TODO correct finish on InterruptedException
                    System.out.println("Thread " + Thread.currentThread().getId() + " interrupted!");
                    break;
                }
            }
        }

        private void consumeTask() throws InterruptedException {
            LightFutureImpl nextTask;

            synchronized (taskQueue) {
                nextTask = taskQueue.poll();
                while (nextTask == null) {
                    taskQueue.wait();
                    nextTask = taskQueue.poll();
                }
            }

            if (nextTask.readyToPerform()) {
                nextTask.perform();
            } else {
                submit(nextTask);
            }
        }
    }
}
