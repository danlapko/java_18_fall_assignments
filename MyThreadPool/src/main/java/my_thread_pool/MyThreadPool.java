package my_thread_pool;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public interface MyThreadPool {
    @NotNull
    <T> LightFuture<T> submit(@NotNull Supplier<T> supplier);

    void shutdown();
}
