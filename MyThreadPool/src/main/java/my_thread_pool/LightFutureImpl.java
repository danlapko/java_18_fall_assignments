package my_thread_pool;

import org.jetbrains.annotations.*;
import java.util.function.Function;
import java.util.function.Supplier;

public class LightFutureImpl<T> implements LightFuture<T> {
    private final MyThreadPoolImpl owner;
    private volatile Supplier<T> supplier;
    private volatile T cachedResult;
    private final Object lock_ready;
    private volatile LightFuture parentTask;
    private volatile LightExecutionException exception = null;


    LightFutureImpl(@NotNull Supplier<T> supplier,@NotNull MyThreadPoolImpl owner) {
        this.owner = owner;
        this.supplier = supplier;
        this.lock_ready = new Object();
        parentTask = null;
    }

    private LightFutureImpl(@NotNull Supplier<T> supplier,@NotNull MyThreadPoolImpl owner,@NotNull LightFuture parentTask) {
        this.owner = owner;
        this.supplier = supplier;
        this.lock_ready = new Object();
        this.parentTask = parentTask;
    }

    boolean readyToPerform() {
        if (parentTask == null)
            return true;
        else return parentTask.isReady();
    }

    @Override
    public boolean isReady() {
        return supplier == null;
    }

    @Override
    public T get() throws LightExecutionException {
        if (exception != null) {
            throw exception;
        }
        perform();
        return cachedResult;
    }

    @Override
    @NotNull
    public <R> LightFuture<R> thenApply(@NotNull Function<T, R> function) {
        if (isReady()) {
            return owner.submit(() -> function.apply(cachedResult));
        }

        synchronized (lock_ready) {
            if (isReady()) {
                return owner.submit(() -> function.apply(cachedResult));
            }

            LightFutureImpl<R> childLightFuturen = new LightFutureImpl<>(() -> function.apply(cachedResult), owner, this);
            owner.submit(childLightFuturen);
            return childLightFuturen;
        }

    }

    synchronized void perform() {
        try {

            if (supplier != null) {
                cachedResult = supplier.get();
            }
        } catch (Exception e) {
            exception = new LightExecutionException(e);
            return;
        }

        synchronized (lock_ready) {
            supplier = null;
            lock_ready.notifyAll();
        }
    }
}

