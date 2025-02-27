import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MainLog<T> {
    private final ConcurrentHashMap<T, LogEntry<T>> mainLogEntries;
    private final ThreadLocal<Map<T, LogEntry<T>>> threadCommitLog = ThreadLocal.withInitial(HashMap::new);
    public MainLog() {
        mainLogEntries = new ConcurrentHashMap<>();
    }
    public MainLog(int bucketSize) {
        mainLogEntries = new ConcurrentHashMap<>(bucketSize);
    }

    public boolean commit(ConcurrentHashMap<T, LogEntry<T>> localLog,boolean batchCommit) {
        Map<T, LogEntry<T>> commitLog = threadCommitLog.get();
        commitLog.clear();
        boolean allSuccess = true;

        for (Map.Entry<T, LogEntry<T>> entry : localLog.entrySet()) {
            T key = entry.getKey();
            LogEntry<T> newEntry = entry.getValue();

            LogEntry<T> existingEntry = mainLogEntries.computeIfAbsent(
                    key, k -> new LogEntry<>(newEntry.getKey(), newEntry.getValue(), newEntry.getVersion())
            );

            int currentVersion = newEntry.getVersion();
            int newVersion = currentVersion + 1;

            boolean success = existingEntry.updateValue(newEntry.getValue(), currentVersion, newVersion);
            if (!success && batchCommit) {
                allSuccess = false;
                break;
            }
            commitLog.put(key, existingEntry); // Store commited entry for rollback
        }

        if (!allSuccess && batchCommit) {
            rollback(commitLog,localLog);
            return false;
        }

        localLog.clear(); // clear logs if the entire batch succeeds
        commitLog.clear();
        return true;
    }

    private void rollback(Map<T, LogEntry<T>> commitLog,ConcurrentHashMap<T, LogEntry<T>> localLog) {
        for (LogEntry<T> entry : commitLog.values()) {
            entry.rollback(); // rollback
        }
        localLog.clear(); // clear logs if the entire batch succeeds
        commitLog.clear(); // remove old commitlog
    }

    public LogEntry<T> getEntry(T key) {
        return mainLogEntries.get(key);
    }
    public T getValue(T key) {
        LogEntry<T>  entry = mainLogEntries.get(key);
        return entry != null ? entry.getValue() : null;
    }

    public int getVersion(T key) {
        LogEntry<T> entryRef = mainLogEntries.get(key);
        return (entryRef != null) ? entryRef.getVersion() : 0;
    }
}
