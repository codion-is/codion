/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.entity;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Provides String representations of {@link Entity} instances.<br>
 * Given a {@link Entity} instance named entity containing the following mappings:
 * <pre>
 * "key1" -&#62; value1
 * "key2" -&#62; value2
 * "key3" -&#62; value3
 * "key4" -&#62; {Entity instance with a single mapping "refKey" -&#62; refValue}
 * </pre>
 * {@code
 * StringProvider provider = new StringProvider();
 * provider.addText("key1=").addValue("key1").addText(", key3='").addValue("key3")
 *         .addText("' foreign key value=").addForeignKeyValue("key4", "refKey");
 * System.out.println(provider.apply(entity));
 * }
 * <br>
 * outputs the following String:<br><br>
 * {@code key1=value1, key3='value3' foreign key value=refValue}
 */
public final class StringProvider implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  private static final String PROPERTY_ID_PARAM = "propertyId";

  /**
   * Holds the ValueProviders used when constructing the String representation
   */
  private final List<Function<Entity, String>> valueProviders = new ArrayList<>();

  /**
   * Instantiates a new {@link StringProvider} instance
   */
  public StringProvider() {}

  /**
   * Instantiates a new {@link StringProvider} instance
   * @param propertyId the id of the property which value should be used for a string representation
   */
  public StringProvider(final String propertyId) {
    addValue(propertyId);
  }

  /**
   * Returns a String representation of the given entity
   * @param entity the entity, may not be null
   * @return a String representation of the entity
   */
  @Override
  public String apply(final Entity entity) {
    Objects.requireNonNull(entity, "entity");

    return valueProviders.stream().map(valueProvider -> valueProvider.apply(entity)).collect(joining());
  }

  /**
   * Adds the value mapped to the given key to this {@link StringProvider}
   * @param propertyId the id of the property which value should be added to the string representation
   * @return this {@link StringProvider} instance
   */
  public StringProvider addValue(final String propertyId) {
    Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
    valueProviders.add(new StringValueProvider(propertyId));
    return this;
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param propertyId the id of the property which value should be added to the string representation
   * @param format the Format to use when appending the value
   * @return this {@link StringProvider} instance
   */
  public StringProvider addFormattedValue(final String propertyId, final Format format) {
    Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
    requireNonNull(format, "format");
    valueProviders.add(new FormattedValueProvider(propertyId, format));
    return this;
  }

  /**
   * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyProperty
   * to this {@link StringProvider}
   * @param foreignKeyPropertyId the if of the foreign key property
   * @param propertyId the id of the property in the referenced entity to use
   * @return this {@link StringProvider} instance
   */
  public StringProvider addForeignKeyValue(final String foreignKeyPropertyId, final String propertyId) {
    requireNonNull(foreignKeyPropertyId, "foreignKeyPropertyId");
    Objects.requireNonNull(propertyId, PROPERTY_ID_PARAM);
    valueProviders.add(new ForeignKeyValueProvider(foreignKeyPropertyId, propertyId));
    return this;
  }

  /**
   * Adds the given static text to this {@link StringProvider}
   * @param text the text to add
   * @return this {@link StringProvider} instance
   */
  public StringProvider addText(final String text) {
    valueProviders.add(new StaticTextProvider(text));
    return this;
  }

  private static final class FormattedValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String propertyId;
    private final Format format;

    private FormattedValueProvider(final String propertyId, final Format format) {
      this.propertyId = propertyId;
      this.format = format;
    }

    @Override
    public String apply(final Entity entity) {
      if (entity.isNull(propertyId)) {
        return "";
      }

      return format.format(entity.get(propertyId));
    }
  }

  private static final class ForeignKeyValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String foreignKeyPropertyId;
    private final String propertyId;

    private ForeignKeyValueProvider(final String foreignKeyPropertyId, final String propertyId) {
      this.foreignKeyPropertyId = foreignKeyPropertyId;
      this.propertyId = propertyId;
    }

    @Override
    public String apply(final Entity entity) {
      if (entity.isNull(foreignKeyPropertyId)) {
        return "";
      }

      return entity.getForeignKey(foreignKeyPropertyId).getAsString(propertyId);
    }
  }

  private static final class StringValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String propertyId;

    private StringValueProvider(final String propertyId) {
      this.propertyId = propertyId;
    }

    @Override
    public String apply(final Entity entity) {
      return entity.getAsString(propertyId);
    }
  }

  private static final class StaticTextProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String text;

    private StaticTextProvider(final String text) {
      this.text = text;
    }

    @Override
    public String apply(final Entity entity) {
      return text;
    }
  }
}
