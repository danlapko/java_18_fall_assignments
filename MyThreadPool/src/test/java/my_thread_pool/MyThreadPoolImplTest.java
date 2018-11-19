package my_thread_pool;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.function.Function;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MyThreadPoolImplTest {

    @Test
    public void withoutThreads() throws InterruptedException {
        MyThreadPool myThreadPool = new MyThreadPoolImpl(0);
        SummatorSupplier task1 = new SummatorSupplier(10, 2);
        SummatorSupplier task2 = new SummatorSupplier(10, 2);

        LightFuture<Integer> future1 = myThreadPool.submit(task1);
        LightFuture<Integer> future2 = myThreadPool.submit(task2);

        assertFalse(future1.isReady());
        assertFalse(future2.isReady());

        Thread.sleep(1000);

        assertFalse(future1.isReady());
        assertFalse(future2.isReady());
    }

    @Test
    public void oneThread() throws LightExecutionException, InterruptedException {
        MyThreadPool myThreadPool = new MyThreadPoolImpl(1);
        SummatorSupplier task1 = new SummatorSupplier(1_000, 2);
        SummatorSupplier task2 = new SummatorSupplier(1_000, 2);

        LightFuture<Integer> future1 = myThreadPool.submit(task1);
        LightFuture<Integer> future2 = myThreadPool.submit(task2);

        assertFalse(future1.isReady());
        assertFalse(future2.isReady());

        Thread.sleep(1000);

        assertTrue(future1.isReady());
        assertTrue(future2.isReady());

        assertEquals(1, future1.get().intValue());
        assertEquals(1, future2.get().intValue());

        SummatorSupplier task3 = new SummatorSupplier(1_000, 2);
        LightFuture<Integer> future3 = myThreadPool.submit(task3);
        assertFalse(future3.isReady());
        assertEquals(1, future3.get().intValue());
    }

    @Test
    public void oneThreadThenApply() throws LightExecutionException, InterruptedException {
        MyThreadPool myThreadPool = new MyThreadPoolImpl(1);

        SummatorSupplier task = new SummatorSupplier(10_000_000, 2);
        SummatorFunction sub_task1 = new SummatorFunction(10_000_000);
        SummatorFunction sub_task2 = new SummatorFunction(10_000_000);
        SummatorFunction sub_sub_task = new SummatorFunction(10_000_000);

        LightFuture<Integer> future = myThreadPool.submit(task);
        LightFuture<Integer> sub_future1 = future.thenApply(sub_task1);
        LightFuture<Integer> sub_future2 = future.thenApply(sub_task2);
        LightFuture<Integer> sub_sub_future = sub_future1.thenApply(sub_sub_task);

        assertFalse(future.isReady());
        assertFalse(sub_future1.isReady());
        assertFalse(sub_future2.isReady());
        assertFalse(sub_sub_future.isReady());

        Thread.sleep(1000);

        assertTrue(future.isReady());
        assertTrue(sub_future1.isReady());
        assertTrue(sub_future2.isReady());
        assertTrue(sub_sub_future.isReady());

        assertEquals(1, future.get().intValue());
        assertEquals(2, sub_future1.get().intValue());
        assertEquals(2, sub_future2.get().intValue());
        assertEquals(3, sub_sub_future.get().intValue());
    }


    @Test
    public void severalThreads() throws LightExecutionException, InterruptedException {
        MyThreadPool myThreadPool = new MyThreadPoolImpl(20);
        List<LightFuture<Integer>> futuresList = new ArrayList<>();

        for (int i = 0; i < 512; i++) {
            futuresList.add(myThreadPool.submit(new SummatorSupplier(1_000_000, 2)));
        }

        for (LightFuture<Integer> future : futuresList) {
            assertFalse(future.isReady());
        }

        Thread.sleep(500);


        int nCompleted = 0;
        int nNotCompleted = 0;
        for (LightFuture<Integer> future : futuresList) {
            if (future.isReady())
                nCompleted += 1;
            else
                nNotCompleted += 1;
        }
        assertTrue(nCompleted > 0);
        assertTrue(nNotCompleted > 0);

        // not all tasks finished yet but get() is working OK
        for (LightFuture<Integer> future : futuresList) {
            assertEquals(1, future.get().intValue());
        }
    }

    @Test
    public void severalThreadsThenApply() throws LightExecutionException, InterruptedException {
        MyThreadPool myThreadPool = new MyThreadPoolImpl(20);
        List<LightFuture<Integer>> futuresList = new ArrayList<>();

        for (int i = 0; i < 128; i++) { // 128 first stage tasks
            futuresList.add(myThreadPool.submit(new SummatorSupplier(1_000_000, 2)));
        }

        for (int i = 0; i < 64; i++) { // 128 second stage tasks
            futuresList.add(futuresList.get(i).thenApply(new SummatorFunction(1_000_000)));
            futuresList.add(futuresList.get(i).thenApply(new SummatorFunction(1_000_000)));
        }

        for (int i = 128; i < 128 + 64; i++) { // 128 third stage tasks
            futuresList.add(futuresList.get(i).thenApply(new SummatorFunction(1_000_000)));
            futuresList.add(futuresList.get(i).thenApply(new SummatorFunction(1_000_000)));
        }

        for (int i = 0; i < 384; i++) { // 384 1-2-3 stage tasks
            LightFuture<Integer> future1 = myThreadPool.submit(new SummatorSupplier(1_000_000, 2));
            LightFuture<Integer> future2 = future1.thenApply(new SummatorFunction(1_000_000));
            LightFuture<Integer> future3 = future2.thenApply(new SummatorFunction(1_000_000));
            futuresList.add(future1);
            futuresList.add(future2);
            futuresList.add(future3);
        }

        Thread.sleep(500);


        int nCompleted = 0;
        int nNotCompleted = 0;
        for (LightFuture<Integer> future : futuresList) {
            if (future.isReady())
                nCompleted += 1;
            else
                nNotCompleted += 1;
        }
        assertTrue(nCompleted > 0);
        assertTrue(nNotCompleted > 0);

        // not all tasks finished yet but get() is working OK

        for (int i = 0; i < 128; i++) { // 128 first stage tasks
            LightFuture<Integer> future = futuresList.get(i);
            assertEquals(1, future.get().intValue());
        }

        for (int i = 128; i < 256; i++) { // 128 second stage tasks
            LightFuture<Integer> future = futuresList.get(i);
            assertEquals(2, future.get().intValue());
        }

        for (int i = 256; i < 384; i++) { // 128 third stage tasks
            LightFuture<Integer> future = futuresList.get(i);
            assertEquals(3, future.get().intValue());
        }

        for (int i = 384, j = 0; i < 384 + 384; i++, j = (j + 1) % 3) { // 384 1-2-3 stage tasks
            LightFuture<Integer> future = futuresList.get(i);
            assertEquals(j + 1, future.get().intValue());
        }
    }

    @Test
    public void shutdown() throws LightExecutionException, InterruptedException {
        MyThreadPoolImpl myThreadPool = new MyThreadPoolImpl(20);
        List<LightFuture<Integer>> futuresList = new ArrayList<>();

        for (int i = 0; i < 512; i++) {
            LightFuture<Integer> future = myThreadPool.submit(new SummatorSupplier(1_000_000, 2));
            futuresList.add(future);
            future.thenApply(new SummatorFunction(1_000_000));
        }

        Thread.sleep(500);

        myThreadPool.shutdown();

        Thread.sleep(500);

        int nCompleted = 0;
        int nNotCompleted = 0;
        for (LightFuture<Integer> future : futuresList) {
            if (future.isReady())
                nCompleted += 1;
            else
                nNotCompleted += 1;
        }
        assertTrue(nCompleted > 0);
        assertTrue(nNotCompleted > 0);


        for (Thread thread : myThreadPool.getThreads()) {
            assertFalse(thread.isAlive());
        }

        // not all tasks finished yet but get() is working OK
        for (LightFuture<Integer> future : futuresList) {
            assertEquals(1, future.get().intValue());
        }
    }

    @Test
    public void throwLightFutureException() throws LightExecutionException, InterruptedException {
        MyThreadPoolImpl myThreadPool = new MyThreadPoolImpl(20);
        List<LightFuture<Integer>> futuresList = new ArrayList<>();

        for (int i = 0; i < 512; i++) {
            LightFuture<Integer> future = myThreadPool.submit(new SummatorFailSupplier(1_000_000, 2));
            futuresList.add(future);
            LightFuture<Integer> subFuture = future.thenApply(new SummatorFailFunction(1_000_000));
            futuresList.add(subFuture);
        }

        Thread.sleep(500);

        int nCompleted = 0;
        int nNotCompleted = 0;
        for (LightFuture<Integer> future : futuresList) {
            if (future.isReady())
                nCompleted += 1;
            else
                nNotCompleted += 1;
        }
        assertTrue(nCompleted > 0);
        assertTrue(nNotCompleted > 0);

        // not all tasks finished yet but get() is working OK
        for (int i = 0; i < futuresList.size(); i++) {

            LightFuture<Integer> future = futuresList.get(i);
            try {
                int result = future.get().intValue();
                assertEquals(i % 2 + 1, result);
            } catch (LightExecutionException ex) {
                assertTrue(ex.getCause() instanceof ArithmeticException);
            }
        }
    }
}

class SummatorSupplier implements Supplier<Integer> {
    private final int n;
    private final int upperBound;
    private final Random rnd = new Random();

    SummatorSupplier(int n, int upperBound) {
        this.n = n;
        this.upperBound = upperBound;
    }

    @Override
    public Integer get() {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += rnd.nextInt(upperBound);
        }
        return (sum + 1) / (sum + 1);
    }
}

class SummatorFunction implements Function<Integer, Integer> {
    private final Random rnd = new Random();
    private final int n;

    SummatorFunction(int n) {
        this.n = n;
    }

    @Override
    public Integer apply(Integer upperBound) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += rnd.nextInt(upperBound);
        }

        return (sum + 1) / (sum + 1) * (upperBound + 1);
    }
}

class SummatorFailSupplier implements Supplier<Integer> {
    private final int n;
    private final int upperBound;
    private final Random rnd = new Random();

    SummatorFailSupplier(int n, int upperBound) {
        this.n = n;
        this.upperBound = upperBound;
    }

    @Override
    public Integer get() {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += rnd.nextInt(upperBound);
        }
        if (rnd.nextFloat() > 0.9) {
            sum += sum / 0;
        }
        return (sum + 1) / (sum + 1);
    }
}

class SummatorFailFunction implements Function<Integer, Integer> {
    private final Random rnd = new Random();
    private final int n;

    SummatorFailFunction(int n) {
        this.n = n;
    }

    @Override
    public Integer apply(Integer upperBound) {
        int sum = 0;
        for (int i = 0; i < n; i++) {
            sum += rnd.nextInt(upperBound);
        }
        if (rnd.nextFloat() > 0.9) {
            sum += sum / 0;
        }
        return (sum + 1) / (sum + 1) * (upperBound + 1);
    }
}
