package my_thread_pool;

import org.junit.Test;

import java.util.Random;
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
        assertEquals(1, sub_future1.get().intValue());
        assertEquals(1, sub_future2.get().intValue());
        assertEquals(1, sub_sub_future.get().intValue());
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
        return (sum + 1) / (sum + 1);
    }
}