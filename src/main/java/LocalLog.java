import java.util.concurrent.ConcurrentHashMap;

public class LocalLog<T> {
    private final ConcurrentHashMap<T, LogEntry<T>> localEntries = new ConcurrentHashMap<>();

    public void write(T key, T value,int version) {
        LogEntry<T> newEntry = new LogEntry<>(key, value,version);
        localEntries.put(key, newEntry);
    }

    public void remove(T key, int version) {
        localEntries.put(key, new LogEntry<>(key, null,version));
    }

    public T read(T key, MainLog<T> mainLog) {
        LogEntry<T> localEntry = localEntries.get(key);
        if (localEntry != null) return localEntry.getValue();

        return mainLog.getValue(key);
    }

    public void discard(T key) {
        localEntries.remove(key);
    }

    public boolean commit(MainLog<T> mainLog,boolean batchCommit) {
        if (localEntries.isEmpty()) return false;
        return mainLog.commit(localEntries,batchCommit);
    }

}
