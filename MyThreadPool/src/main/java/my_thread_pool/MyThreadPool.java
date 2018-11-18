package my_thread_pool;

import java.util.function.Supplier;

public interface MyThreadPool {
    <T> LightFuture<T> submit(Supplier<T> supplier);

    void shutdown();
}
