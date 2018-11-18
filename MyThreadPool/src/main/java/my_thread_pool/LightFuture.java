package my_thread_pool;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

public interface LightFuture<T> {
    boolean isReady();

    T get() throws LightExecutionException;

    @NotNull
    <R> LightFuture<R> thenApply(@NotNull Function<T, R> function) throws InterruptedException;

}
