import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class RLU<T> {
    private final MainLog<T> mainLog;
    private final ConcurrentHashMap<Long, LocalLog<T>> localLogs;
    private final AtomicLong transactionCounter = new AtomicLong(0);
    private final ThreadLocal<Long> transactionId;
    private final boolean batchCommit;

    private RLU(MainLog<T> mainLog, boolean batchCommit) {
        this.mainLog = mainLog;
        this.localLogs = new ConcurrentHashMap<>();
        this.transactionId = ThreadLocal.withInitial(() -> transactionCounter.incrementAndGet());
        this.batchCommit = batchCommit;
    }

    // Public constructors chaining to the private constructor

    // Default: no bucketSize, batchCommit true
    public RLU() {
        this(new MainLog<>(), true);
    }

    // Constructor with bucketSize, default batchCommit true
    public RLU(int bucketSize) {
        this(new MainLog<>(bucketSize), true);
    }

    // Constructor with batchCommit, default mainLog
    public RLU(boolean batchCommit) {
        this(new MainLog<>(), batchCommit);
    }

    // Constructor with bucketSize and batchCommit
    public RLU(int bucketSize, boolean batchCommit) {
        this(new MainLog<>(bucketSize), batchCommit);
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
