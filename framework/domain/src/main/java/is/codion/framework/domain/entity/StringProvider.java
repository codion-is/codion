/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

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
 * provider.addText("key1=").addValue(KEY1).addText(", key3='").addValue(KEY3")
 *         .addText("' foreign key value=").addForeignKeyValue(FK, FK_REF");
 * System.out.println(provider.apply(entity));
 * }
 * <br>
 * outputs the following String:<br><br>
 * {@code key1=value1, key3='value3' foreign key value=refValue}
 */
public final class StringProvider implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  private static final String ATTRIBUTE_PARAM = "attribute";

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
   * @param attribute the attribute which value should be used for a string representation
   */
  public StringProvider(final Attribute<?> attribute) {
    addValue(attribute);
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
   * @param attribute the attribute which value should be added to the string representation
   * @return this {@link StringProvider} instance
   */
  public StringProvider addValue(final Attribute<?> attribute) {
    Objects.requireNonNull(attribute, ATTRIBUTE_PARAM);
    valueProviders.add(new StringValueProvider(attribute));
    return this;
  }

  /**
   * Adds the value mapped to the given key to this StringProvider
   * @param attribute the attribute which value should be added to the string representation
   * @param format the Format to use when appending the value
   * @return this {@link StringProvider} instance
   */
  public StringProvider addFormattedValue(final Attribute<?> attribute, final Format format) {
    Objects.requireNonNull(attribute, ATTRIBUTE_PARAM);
    requireNonNull(format, "format");
    valueProviders.add(new FormattedValueProvider(attribute, format));
    return this;
  }

  /**
   * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreignKeyProperty
   * to this {@link StringProvider}
   * @param foreignKeyAttribute the foreign key attribute
   * @param attribute the attribute in the referenced entity to use
   * @return this {@link StringProvider} instance
   */
  public StringProvider addForeignKeyValue(final Attribute<Entity> foreignKeyAttribute, final Attribute<?> attribute) {
    requireNonNull(foreignKeyAttribute, "foreignKeyAttribute");
    Objects.requireNonNull(attribute, ATTRIBUTE_PARAM);
    valueProviders.add(new ForeignKeyValueProvider(foreignKeyAttribute, attribute));
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

    private final Attribute<?> attribute;
    private final Format format;

    private FormattedValueProvider(final Attribute<?> attribute, final Format format) {
      this.attribute = attribute;
      this.format = format;
    }

    @Override
    public String apply(final Entity entity) {
      if (entity.isNull(attribute)) {
        return "";
      }

      return format.format(entity.get(attribute));
    }
  }

  private static final class ForeignKeyValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<Entity> foreignKeyAttribute;
    private final Attribute<?> attribute;

    private ForeignKeyValueProvider(final Attribute<Entity> foreignKeyAttribute, final Attribute<?> attribute) {
      this.foreignKeyAttribute = foreignKeyAttribute;
      this.attribute = attribute;
    }

    @Override
    public String apply(final Entity entity) {
      if (entity.isNull(foreignKeyAttribute)) {
        return "";
      }

      return entity.getForeignKey(foreignKeyAttribute).getAsString(attribute);
    }
  }

  private static final class StringValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<?> attribute;

    private StringValueProvider(final Attribute<?> attribute) {
      this.attribute = attribute;
    }

    @Override
    public String apply(final Entity entity) {
      return entity.getAsString(attribute);
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
