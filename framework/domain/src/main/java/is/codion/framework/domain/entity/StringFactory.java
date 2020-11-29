/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Creates String representations of {@link Entity} instances.<br>
 * Given a {@link Entity} instance named entity containing the following mappings:
 * <pre>
 * attribute1 -&#62; value1
 * attribute2 -&#62; value2
 * attribute3 -&#62; value3
 * fkAttribute -&#62; {Entity instance with a single mapping refAttribute -&#62; refValue}
 * </pre>
 * StringFactory.Builder builder = StringFactory.stringFactory();<br>
 * <br>
 * builder.text("attribute1=").value(attribute1).text(", attribute3='").value(attribute3)<br>
 *         .text("' foreign key value=").foreignKeyValue(fkAttribute, refAttribute);<br>
 * <br>
 * System.out.println(builder.get().apply(entity));<br><br>
 * outputs the following String:<br><br>
 * {@code attribute1=value1, attribute3='value3' foreign key value=refValue}
 */
public final class StringFactory implements Function<Entity, String>, Serializable {

  private static final long serialVersionUID = 1;

  private static final String ATTRIBUTE_PARAM = "attribute";

  /**
   * Holds the ValueProviders used when constructing the String representation
   */
  private final List<Function<Entity, String>> valueProviders = new ArrayList<>();

  /**
   * Instantiates a new {@link StringFactory} instance
   */
  StringFactory() {}

  /**
   * Instantiates a new {@link StringFactory} instance
   * @param attribute the attribute which value should be used for a string representation
   */
  StringFactory(final Attribute<?> attribute) {
    addValue(attribute);
  }

  /**
   * Returns a String representation of the given entity
   * @param entity the entity, may not be null
   * @return a String representation of the entity
   */
  @Override
  public String apply(final Entity entity) {
    requireNonNull(entity, "entity");
    if (valueProviders.size() == 1) {
      return valueProviders.get(0).apply(entity);
    }

    return valueProviders.stream().map(valueProvider -> valueProvider.apply(entity)).collect(joining());
  }

  StringFactory addValue(final Attribute<?> attribute) {
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    valueProviders.add(new StringValueProvider(attribute));
    return this;
  }

  StringFactory addFormattedValue(final Attribute<?> attribute, final Format format) {
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    requireNonNull(format, "format");
    valueProviders.add(new FormattedValueProvider(attribute, format));
    return this;
  }

  StringFactory addForeignKeyValue(final ForeignKey foreignKey, final Attribute<?> attribute) {
    requireNonNull(foreignKey, "foreignKey");
    requireNonNull(attribute, ATTRIBUTE_PARAM);
    if (!attribute.getEntityType().equals(foreignKey.getReferencedEntityType())) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + foreignKey.getEntityType());
    }
    valueProviders.add(new ForeignKeyValueProvider(foreignKey, attribute));
    return this;
  }

  StringFactory addText(final String text) {
    valueProviders.add(new StaticTextProvider(text));
    return this;
  }

  /**
   * @return a {@link Builder} instance for configuring a string factory {@link Function} for entities.
   */
  public static Builder stringFactory() {
    return new DefaultStringFactoryBuilder(new StringFactory());
  }

  /**
   * @param attribute the attribute which value to use for the string generation
   * @return a {@link Builder} instance for configuring a string factory {@link Function} for entities.
   */
  public static Builder stringFactory(final Attribute<?> attribute) {
    return new DefaultStringFactoryBuilder(new StringFactory(attribute));
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

    private final ForeignKey foreignKey;
    private final Attribute<?> attribute;

    private ForeignKeyValueProvider(final ForeignKey foreignKey, final Attribute<?> attribute) {
      this.foreignKey = foreignKey;
      this.attribute = attribute;
    }

    @Override
    public String apply(final Entity entity) {
      if (entity.isNull(foreignKey)) {
        return "";
      }

      return entity.getForeignKey(foreignKey).getAsString(attribute);
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

  /**
   * A Builder for a string function, which provides toString() values for entities.
   */
  public interface Builder {

    /**
     * Adds the value mapped to the given key to this {@link Builder}
     * @param attribute the attribute which value should be added to the string representation
     * @return this {@link Builder} instance
     */
    Builder value(Attribute<?> attribute);

    /**
     * Adds the value mapped to the given key to this StringProvider
     * @param attribute the attribute which value should be added to the string representation
     * @param format the Format to use when appending the value
     * @return this {@link Builder} instance
     */
    Builder formattedValue(Attribute<?> attribute, Format format);

    /**
     * Adds the value mapped to the given property in the {@link Entity} instance mapped to the given foreign key
     * to this {@link Builder}
     * @param foreignKey the foreign key
     * @param attribute the attribute in the referenced entity to use
     * @return this {@link Builder} instance
     */
    Builder foreignKeyValue(ForeignKey foreignKey, Attribute<?> attribute);

    /**
     * Adds the given static text to this {@link Builder}
     * @param text the text to add
     * @return this {@link Builder} instance
     */
    Builder text(String text);

    /**
     * @return the string factory function
     */
    Function<Entity, String> get();
  }
}
