import java.util.Objects;
import java.util.concurrent.atomic.AtomicStampedReference;

public class LogEntry<T> {
    private final T key;
    private final AtomicStampedReference<T> valueWithVersion;
    private T previousValue; // 🚀 Ensures latest values are visible to all threads
    private int previousVersion;

    public LogEntry(T key, T value, int version) {
        this.key = key;
        this.valueWithVersion = new AtomicStampedReference<>(value, version);
        this.previousValue = value;
        this.previousVersion = version;
    }

    public boolean updateValue(T newValue, int currentVersion, int newVersion) {
        T oldValue = this.getValue();
        int oldVersion = this.getVersion();

        boolean success = this.valueWithVersion.compareAndSet(oldValue, newValue, currentVersion, newVersion);
        if (success) {
            this.previousValue = oldValue;
            this.previousVersion = oldVersion;
        }
        return success;
    }


    public int getVersion() {
        return this.valueWithVersion.getStamp();
    }

    public boolean rollback() {
        if (Objects.equals(this.getValue(), this.previousValue)) return true;
        return this.valueWithVersion.compareAndSet(this.getValue(), this.previousValue, this.getVersion(), this.previousVersion);
    }


    public T getValue() {
        return this.valueWithVersion.getReference();
    }

    public T getKey() {
        return this.key;
    }
}
