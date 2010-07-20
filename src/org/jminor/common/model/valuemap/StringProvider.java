/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.model.valuemap;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;

/**
 * Provides String representations of ValueMap objects.<br>
 * Given a ValueMap named valueMap containing the following mappings:
 * <pre>
 * "key1" -> value1
 * "key2" -> value2
 * "key3" -> value3
 * "key4" -> {ValueMap instance with a single mapping "refKey" -> refValue}
 * </pre>
 * <code>
 * StringProvider provider = new StringProvider();<br>
 * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")<br>
 *         .addText("' referenced value=").addReferencedValue("key4", "refKey");<br>
 * System.out.println(provider.toString(valueMap));<br>
 * </code>
 * <br>
 * outputs the following String:<br><br>
 * <code>key1=value1, key3='value3' referenced value=refValue</code>
 * @param <K> the type of the map keys
 */
public final class StringProvider<K> implements ValueMap.ToString<K>, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * Holds the ValueProviders used when constructing the String representation
   */
  private final List<ValueProvider<K>> valueProviders = new ArrayList<ValueProvider<K>>();

  /**
   * Instantiates a new StringProvider instance
   */
  public StringProvider() {}

  /**
   * Instantiates a new StringProvider instance, with the value mapped to the given key
   * @param key the key
   */
  public StringProvider(final K key) {
    addValue(key);
  }

  public String toString(final ValueMap<K, ?> valueMap) {
    final StringBuilder builder = new StringBuilder();
    for (final ValueProvider<K> valueProvider : valueProviders) {
      builder.append(valueProvider.toString(valueMap));
    }

    return builder.toString();
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider<K> addValue(final K key) {
    valueProviders.add(new StringValueProvider<K>(key));
    return this;
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @param format the Format to use when appending the value
   * @return this StringProvider instance
   */
  public StringProvider<K> addFormattedValue(final K key, final Format format) {
    valueProviders.add(new FormattedValueProvider<K>(key, format));
    return this;
  }

  /**
   * Adds the value mapped to the given key in the ValueMap instance mapped to the given foreignKeyPropertyID
   * to this StringProvider
   * @param referenceKey the reference key
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider<K> addReferencedValue(final K referenceKey, final K key) {
    valueProviders.add(new ReferencedValueProvider<K>(referenceKey, key));
    return this;
  }

  /**
   * Adds the given static text to this StringProvider
   * @param text the text to add
   * @return this StringProvider instance
   */
  public StringProvider<K> addText(final String text) {
    valueProviders.add(new StaticTextProvider<K>(text));
    return this;
  }

  /**
   * Returns true if the given key is referenced
   * @param referenceKey the key
   * @return true if the given key is referenced
   */
  public boolean references(final K referenceKey) {
    for (final ValueProvider provider : valueProviders) {
      if (provider instanceof ReferencedValueProvider && ((ReferencedValueProvider) provider).getReferenceKey().equals(referenceKey)) {
        return true;
      }
    }
    return false;
  }

  private interface ValueProvider<T> extends Serializable {
    String toString(final ValueMap<T, ?> valueMap);
  }

  private static class FormattedValueProvider<T> implements ValueProvider<T> {
    private final T key;
    private final Format format;

    FormattedValueProvider(final T key, final Format format) {
      this.key = key;
      this.format = format;
    }

    public String toString(final ValueMap<T, ?> valueMap) {
      if (valueMap.isValueNull(key)) {
        return "";
      }

      return format.format(valueMap.getValue(key));
    }
  }

  private static class ReferencedValueProvider<T> implements ValueProvider<T> {
    private final T referenceKey;
    private final T key;

    ReferencedValueProvider(final T referenceKey, final T key) {
      this.referenceKey = referenceKey;
      this.key = key;
    }

    @SuppressWarnings({"unchecked"})
    public String toString(final ValueMap<T, ?> valueMap) {
      if (valueMap.isValueNull(referenceKey)) {
        return "";
      }
      final Object referencedValue = valueMap.getValue(referenceKey);
      if (!(referencedValue instanceof ValueMap)) {
        throw new RuntimeException(referenceKey + " does not refer to a ValueMap instance");
      }
      final ValueMap<T, ?> referencedValueMap = (ValueMap<T, ?>) referencedValue;
      if (referencedValueMap.isValueNull(key)) {
        return "";
      }

      return referencedValueMap.getValue(key).toString();
    }

    public T getReferenceKey() {
      return referenceKey;
    }
  }

  private static class StringValueProvider<T> implements ValueProvider<T> {
    private final T key;

    StringValueProvider(final T key) {
      this.key = key;
    }

    public String toString(final ValueMap<T, ?> valueMap) {
      if (valueMap.isValueNull(key)) {
        return "";
      }

      return valueMap.getValueAsString(key);
    }
  }

  private static class StaticTextProvider<T> implements ValueProvider<T> {
    private final String text;

    StaticTextProvider(final String text) {
      this.text = text;
    }

    public String toString(final ValueMap<T, ?> valueMap) {
      return text;
    }
  }
}
