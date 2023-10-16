/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.io.Serializable;
import java.text.Format;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * Factory class for building functions for String representations of {@link Entity} instances.<br>
 * Given a {@link Entity} instance named entity containing the following mappings:
 * <pre>
 * attribute1 -&#62; value1
 * attribute2 -&#62; value2
 * attribute3 -&#62; value3
 * fkAttribute -&#62; {Entity instance with a single mapping refAttribute -&#62; refValue}
 *
 * StringFactory.Builder builder = StringFactory.builder();
 *
 * builder.text("attribute1=")
 *        .value(attribute1)
 *        .text(", attribute3='")
 *        .value(attribute3)
 *        .text("' foreign key value=")
 *        .value(fkAttribute, refAttribute);
 *
 * System.out.println(builder.build().apply(entity));
 * </pre>
 * outputs the following String:<br><br>
 * {@code attribute1=value1, attribute3='value3' foreign key value=refValue}
 */
public final class StringFactory {

  private static final String ATTRIBUTE_PARAM = "attribute";

  private StringFactory() {}

  /**
   * @return a {@link Builder} instance for configuring a string factory {@link Function} for entities.
   */
  public static Builder builder() {
    return new DefaultStringFactoryBuilder();
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
    Builder value(Attribute<?> attribute, Format format);

    /**
     * Adds the value mapped to the given attribute in the {@link Entity} instance mapped to the given foreign key
     * to this {@link Builder}
     * @param foreignKey the foreign key
     * @param attribute the attribute in the referenced entity to use
     * @return this {@link Builder} instance
     */
    Builder value(ForeignKey foreignKey, Attribute<?> attribute);

    /**
     * Adds the given static text to this {@link Builder}
     * @param text the text to add
     * @return this {@link Builder} instance
     */
    Builder text(String text);

    /**
     * @return a new string factory function based on this builder
     */
    Function<Entity, String> build();
  }

  private static final class DefaultStringFactory implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    /**
     * Holds the ValueProviders used when constructing the String representation
     */
    private final List<Function<Entity, String>> valueProviders;

    /**
     * Instantiates a new {@link StringFactory} instance
     */
    private DefaultStringFactory(DefaultStringFactoryBuilder builder) {
      this.valueProviders = unmodifiableList(builder.valueProviders);
    }

    /**
     * Returns a String representation of the given entity
     * @param entity the entity, may not be null
     * @return a String representation of the entity
     */
    @Override
    public String apply(Entity entity) {
      requireNonNull(entity, "entity");
      if (valueProviders.size() == 1) {
        return valueProviders.get(0).apply(entity);
      }

      return valueProviders.stream()
              .map(valueProvider -> valueProvider.apply(entity))
              .collect(joining());
    }
  }

  private static final class FormattedValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<?> attribute;
    private final Format format;

    private FormattedValueProvider(Attribute<?> attribute, Format format) {
      this.attribute = requireNonNull(attribute, ATTRIBUTE_PARAM);
      this.format = requireNonNull(format, "format");
    }

    @Override
    public String apply(Entity entity) {
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

    private ForeignKeyValueProvider(ForeignKey foreignKey, Attribute<?> attribute) {
      this.foreignKey = requireNonNull(foreignKey, "foreignKey");
      this.attribute = requireNonNull(attribute, ATTRIBUTE_PARAM);
      if (!attribute.entityType().equals(foreignKey.referencedType())) {
        throw new IllegalArgumentException("Attribute " + attribute + " is not part of entity: " + foreignKey.entityType());
      }
    }

    @Override
    public String apply(Entity entity) {
      if (entity.isNull(foreignKey)) {
        return "";
      }

      return entity.referencedEntity(foreignKey).string(attribute);
    }
  }

  private static final class StringValueProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final Attribute<?> attribute;

    private StringValueProvider(Attribute<?> attribute) {
      this.attribute = requireNonNull(attribute, ATTRIBUTE_PARAM);
    }

    @Override
    public String apply(Entity entity) {
      return entity.string(attribute);
    }
  }

  private static final class StaticTextProvider implements Function<Entity, String>, Serializable {

    private static final long serialVersionUID = 1;

    private final String text;

    private StaticTextProvider(String text) {
      this.text = requireNonNull(text, "text");
    }

    @Override
    public String apply(Entity entity) {
      return text;
    }
  }

  private static final class DefaultStringFactoryBuilder implements Builder {

    private final List<Function<Entity, String>> valueProviders = new ArrayList<>();

    private EntityType entityType;

    @Override
    public Builder value(Attribute<?> attribute) {
      valueProviders.add(new StringValueProvider(attribute));
      validateEntityType(attribute);
      return this;
    }

    @Override
    public Builder value(Attribute<?> attribute, Format format) {
      valueProviders.add(new FormattedValueProvider(attribute, format));
      return this;
    }

    @Override
    public Builder value(ForeignKey foreignKey, Attribute<?> attribute) {
      valueProviders.add(new ForeignKeyValueProvider(foreignKey, attribute));
      return this;
    }

    @Override
    public Builder text(String text) {
      valueProviders.add(new StaticTextProvider(text));
      return this;
    }

    @Override
    public Function<Entity, String> build() {
      return new DefaultStringFactory(this);
    }

    private void validateEntityType(Attribute<?> attribute) {
      if (entityType == null) {
        entityType = attribute.entityType();
      }
      else if (!attribute.entityType().equals(entityType)) {
        throw new IllegalArgumentException("entityType " + entityType + " expected, got: " + attribute.entityType());
      }
    }
  }
}
