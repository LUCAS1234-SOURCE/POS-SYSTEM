import java.util.HashSet;
import java.util.Set;

/**
 * Handles communication between dialogs when data changes
 */
public class EventBus {
    private static final EventBus instance = new EventBus();
    private final Set<DataUpdateListener> listeners = new HashSet<>();

    private EventBus() {}

    public static EventBus getInstance() {
        return instance;
    }

    public void registerListener(DataUpdateListener listener) {
        listeners.add(listener);
    }

    public void unregisterListener(DataUpdateListener listener) {
        listeners.remove(listener);
    }

    public void notifyDataUpdated(UpdateType type) {
        for (DataUpdateListener listener : listeners) {
            listener.onDataUpdated(type);
        }
    }

    public enum UpdateType {
        PRODUCTS_UPDATED,
        SALES_UPDATED
    }

    public interface DataUpdateListener {
        void onDataUpdated(UpdateType type);
    }
}