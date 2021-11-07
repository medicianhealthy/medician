package interfaces;

public interface BasicMap {
    void removeKey(final String key);
    boolean containsKey(final String key);
    void setValue(final String key, final Object value);
    int getInt(final String key, final int defaultValue);
    long getLong(final String key, final long defaultValue);
    float getFloat(final String key, final float defaultVale);
    String getString(final String key, final String defaultValue);


}
