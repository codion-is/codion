/*
 * Copyright (c) 2004 - 2010, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.model;

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
 * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")
 *         .addText("' referenced value=").addReferencedValue("key4", "refKey");<br>
 * System.out.println(provider.toString(valueMap));<br>
 * </code>
 * <br>
 * outputs the following String:<br><br>
 * <code>key1=value1, key3='value3' referenced value=refValue</code>
 */
public class StringProvider<T, V> implements ValueMap.ToString<T, V>, Serializable {

  /**
   * Holds the ValueProviders used when constructing the String representation
   */
  private final List<ValueProvider<T, V>> valueProviders = new ArrayList<ValueProvider<T, V>>();

  /**
   * Instantiates a new StringProvider instance
   */
  public StringProvider() {}

  /**
   * Instantiates a new StringProvider instance, with the value mapped to the given key
   * @param key the key
   */
  public StringProvider(final T key) {
    addValue(key);
  }

  /** {@inheritDoc} */
  public String toString(final ValueMap<T, V> valueMap) {
    final StringBuilder builder = new StringBuilder();
    for (final ValueProvider<T, V> valueProvider : valueProviders)
      builder.append(valueProvider.toString(valueMap));

    return builder.toString();
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider<T, V> addValue(final T key) {
    valueProviders.add(new StringValueProvider<T, V>(key));
    return this;
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @param format the Format to use when appending the value
   * @return this StringProvider instance
   */
  public StringProvider<T, V> addFormattedValue(final T key, final Format format) {
    valueProviders.add(new FormattedValueProvider<T, V>(key, format));
    return this;
  }

  /**
   * Adds the value mapped to the given key in the Entity instance mapped to the given foreignKeyPropertyID
   * to this StringProvider
   * @param referenceKey the reference key
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider<T, V> addReferencedValue(final T referenceKey, final T key) {
    valueProviders.add(new ReferencedValueProvider<T, V>(referenceKey, key));
    return this;
  }

  /**
   * Adds the given static text to this StringProvider
   * @param text the text to add
   * @return this StringProvider instance
   */
  public StringProvider<T, V> addText(final String text) {
    valueProviders.add(new StaticTextProvider<T, V>(text));
    return this;
  }

  private static interface ValueProvider<T, V> {
    public String toString(final ValueMap<T, V> valueMap);
  }

  private static class FormattedValueProvider<T, V> implements ValueProvider<T, V>, Serializable {
    private final T key;
    private final Format format;

    public FormattedValueProvider(final T key, final Format format) {
      this.key = key;
      this.format = format;
    }

    public String toString(final ValueMap<T, V> valueMap) {
      if (valueMap.isValueNull(key))
        return "";

      return format.format(valueMap.getValue(key));
    }
  }

  private static class ReferencedValueProvider<T, V> implements ValueProvider<T, V>, Serializable {
    private final T referenceKey;
    private final T key;

    public ReferencedValueProvider(final T referenceKey, final T key) {
      this.referenceKey = referenceKey;
      this.key = key;
    }

    @SuppressWarnings({"unchecked"})
    public String toString(final ValueMap<T, V> valueMap) {
      if (valueMap.isValueNull(referenceKey))
        return "";
      final Object referencedValue = valueMap.getValue(referenceKey);
      if (!(referencedValue instanceof ValueMap))
        throw new RuntimeException(referenceKey + " does not refer to a ValueMap instance");
      final ValueMap<T, V> foreignKeyEntity = (ValueMap<T, V>) referencedValue;
      if (foreignKeyEntity.isValueNull(key))
        return "";

      return foreignKeyEntity.getValue(key).toString();
    }
  }

  private static class StringValueProvider<T, V> implements ValueProvider<T, V>, Serializable {
    private final T key;

    public StringValueProvider(final T key) {
      this.key = key;
    }

    public String toString(final ValueMap<T, V> valueMap) {
      if (valueMap.isValueNull(key))
        return "";

      return valueMap.getValue(key).toString();
    }
  }

  private static class StaticTextProvider<T, V> implements ValueProvider<T, V>, Serializable {
    private final String text;

    public StaticTextProvider(final String text) {
      this.text = text;
    }

    public String toString(final ValueMap<T, V> valueMap) {
      return text;
    }
  }
}
