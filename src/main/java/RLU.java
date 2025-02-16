import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RLU<T> {
    private final MainLog<T> mainLog;
    private final ConcurrentHashMap<Long, LocalLog<T>> localLogs;
    private final AtomicLong transactionCounter = new AtomicLong(0);
    private final ThreadLocal<Long> transactionId;
    private final boolean batchCommit;

    public RLU() {
        this.mainLog = new MainLog<>();
        this.localLogs = new ConcurrentHashMap<>();
        this.transactionId =
                ThreadLocal.withInitial(() -> transactionCounter.incrementAndGet());
        this.batchCommit = true;
    }
    public RLU(boolean batchCommit) {
        this.mainLog = new MainLog<>();
        this.batchCommit = batchCommit;
        this.localLogs = new ConcurrentHashMap<>();
        this.transactionId =
                ThreadLocal.withInitial(() -> transactionCounter.incrementAndGet());
    }

    private LocalLog<T> getLocalLog() {
        return localLogs.computeIfAbsent(transactionId.get(), k -> new LocalLog<>());
    }
    public boolean write(T key, T value) {
        int version = mainLog.getVersion(key);
        getLocalLog().write(key, value,version);
        return true;
    }

    public boolean remove(T key) {
        LogEntry<T> entry = mainLog.getEntry(key);
        if (entry == null) return false;
        int version = entry.getVersion();
        getLocalLog().remove(key, version);
        return true;
    }

    public T read(T key) {
        return getLocalLog().read(key, mainLog);
    }

    public void discard(T key) {
        getLocalLog().discard(key);
    }

    public boolean commit() {
        return getLocalLog().commit(mainLog,batchCommit);
    }
}
