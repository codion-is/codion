/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
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
public class StringProvider implements ValueMap.ToString, Serializable {

  /**
   * Holds the ValueProviders used when constructing the String representation
   */
  private final List<ValueProvider> valueProviders = new ArrayList<ValueProvider>();

  /**
   * Instantiates a new StringProvider instance
   */
  public StringProvider() {}

  /**
   * Instantiates a new StringProvider instance, with the value mapped to the given key
   * @param key the key
   */
  public StringProvider(final String key) {
    addValue(key);
  }

  /** {@inheritDoc} */
  public String toString(final ValueMap valueMap) {
    final StringBuilder builder = new StringBuilder();
    for (final ValueProvider valueProvider : valueProviders)
      builder.append(valueProvider.toString(valueMap));

    return builder.toString();
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider addValue(final String key) {
    valueProviders.add(new StringValueProvider(key));
    return this;
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param key the key
   * @param format the Format to use when appending the value
   * @return this StringProvider instance
   */
  public StringProvider addFormattedValue(final String key, final Format format) {
    valueProviders.add(new FormattedValueProvider(key, format));
    return this;
  }

  /**
   * Adds the value mapped to the given key in the Entity instance mapped to the given foreignKeyPropertyID
   * to this StringProvider
   * @param foreignKeyPropertyID the ID of the foreign key property
   * @param key the key
   * @return this StringProvider instance
   */
  public StringProvider addReferencedValue(final String foreignKeyPropertyID, final String key) {
    valueProviders.add(new ReferencedValueProvider(foreignKeyPropertyID, key));
    return this;
  }

  /**
   * Adds the given static text to this StringProvider
   * @param text the text to add
   * @return this StringProvider instance
   */
  public StringProvider addText(final String text) {
    valueProviders.add(new StaticTextProvider(text));
    return this;
  }

  private static interface ValueProvider {
    public String toString(final ValueMap valueMap);
  }

  private static class FormattedValueProvider implements ValueProvider, Serializable {
    private final String key;
    private final Format format;

    public FormattedValueProvider(final String key, final Format format) {
      this.key = key;
      this.format = format;
    }

    public String toString(final ValueMap valueMap) {
      if (valueMap.isValueNull(key))
        return "";

      return format.format(valueMap.getValue(key));
    }
  }

  private static class ReferencedValueProvider implements ValueProvider, Serializable {
    private final String referenceKey;
    private final String key;

    public ReferencedValueProvider(final String referenceKey, final String key) {
      this.referenceKey = referenceKey;
      this.key = key;
    }

    public String toString(final ValueMap valueMap) {
      if (valueMap.isValueNull(referenceKey))
        return "";
      final Object referencedValue = valueMap.getValue(referenceKey);
      if (!(referencedValue instanceof ValueMap))
        throw new RuntimeException(referenceKey + " does not refer to a ValueMap instance");
      final ValueMap foreignKeyEntity = (ValueMap) referencedValue;
      if (foreignKeyEntity.isValueNull(key))
        return "";

      return foreignKeyEntity.getValue(key).toString();
    }
  }

  private static class StringValueProvider implements ValueProvider, Serializable {
    private final String key;

    public StringValueProvider(final String key) {
      this.key = key;
    }

    public String toString(final ValueMap valueMap) {
      if (valueMap.isValueNull(key))
        return "";

      return valueMap.getValue(key).toString();
    }
  }

  private static class StaticTextProvider implements ValueProvider, Serializable {
    private final String text;

    public StaticTextProvider(final String text) {
      this.text = text;
    }

    public String toString(final ValueMap valueMap) {
      return text;
    }
  }
}
