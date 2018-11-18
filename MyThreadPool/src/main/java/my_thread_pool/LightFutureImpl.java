package my_thread_pool;

import java.util.function.Function;
import java.util.function.Supplier;

public class LightFutureImpl<T> implements LightFuture<T> {
    private final MyThreadPoolImpl owner;
    private volatile Supplier<T> supplier;
    private volatile T cachedResult;
    private final Object lock_ready;
    private volatile LightFuture parentTask;


    LightFutureImpl(Supplier<T> supplier, MyThreadPoolImpl owner) {
        this.owner = owner;
        this.supplier = supplier;
        this.lock_ready = new Object();
        parentTask = null;
    }

    private LightFutureImpl(Supplier<T> supplier, MyThreadPoolImpl owner, LightFuture parentTask) {
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
        perform();
        return cachedResult;
    }

    @Override
    public <R> LightFuture<R> thenApply(Function<T, R> function) {
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
        if (supplier != null) {
            cachedResult = supplier.get();
        }

        synchronized (lock_ready) {
            supplier = null;
            lock_ready.notifyAll();
        }
    }
}

