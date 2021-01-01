/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A class representing a unique key for entities.
 */
class DefaultKey implements Key, Serializable {

  private static final long serialVersionUID = 1;

  private static final String COMPOSITE_KEY_MESSAGE = "Key is a composite key";

  /**
   * The attributes comprising this key
   */
  private List<Attribute<?>> attributes;

  /**
   * True if this key represents a primary key
   */
  private boolean primaryKey;

  /**
   * Holds the values contained in this key.
   */
  private Map<Attribute<?>, Object> values;

  /**
   * true if this key consists of a single integer value
   */
  private boolean singleIntegerKey;

  /**
   * Caching the hash code
   */
  private Integer cachedHashCode = null;

  /**
   * True until cachedHashCode has been computed
   */
  private boolean hashCodeDirty = true;

  /**
   * Caching this extremely frequently referenced object
   */
  private EntityDefinition definition;

  /**
   * Instantiates a new DefaultKey based on the given attributes, with the associated values as null
   * @param definition the entity definition
   * @param attributes the attributes comprising this key
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(final EntityDefinition definition, final List<Attribute<?>> attributes, final boolean primaryKey) {
    this(definition, createNullValueMap(attributes), primaryKey);
    this.hashCodeDirty = false;
  }

  /**
   * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
   * @param definition the entity definition
   * @param value the value
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(final EntityDefinition definition, final Attribute<?> attribute, final Object value, final boolean primaryKey) {
    this(definition, singletonMap(attribute, value), primaryKey);
  }

  /**
   * Instantiates a new DefaultKey with the given values
   * @param definition the entity definition
   * @param values the values associated with their respective attributes
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(final EntityDefinition definition, final Map<Attribute<?>, Object> values, final boolean primaryKey) {
    values.forEach((attribute, value) -> ((Attribute<Object>) attribute).validateType(value));
    this.values = unmodifiableMap(values);
    this.attributes = unmodifiableList(new ArrayList<>(values.keySet()));
    this.definition = definition;
    this.primaryKey = primaryKey;
    if (!this.attributes.isEmpty()) {
      this.singleIntegerKey = attributes.size() == 1 && attributes.get(0).isInteger();
    }
  }

  @Override
  public EntityType<?> getEntityType() {
    return definition.getEntityType();
  }

  @Override
  public Collection<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public boolean isPrimaryKey() {
    return primaryKey;
  }

  @Override
  public <T> Attribute<T> getAttribute() {
    if (attributes.size() > 1) {
      throw new IllegalStateException(COMPOSITE_KEY_MESSAGE);
    }

    return (Attribute<T>) attributes.get(0);
  }

  @Override
  public <T> Key withValue(final T value) {
    if (attributes.size() > 1) {
      throw new IllegalStateException(COMPOSITE_KEY_MESSAGE);
    }

    return withValue((Attribute<T>) attributes.get(0), value);
  }

  @Override
  public <T> T get() {
    if (attributes.size() > 1) {
      throw new IllegalStateException(COMPOSITE_KEY_MESSAGE);
    }

    return (T) values.get(attributes.get(0));
  }

  @Override
  public <T> Optional<T> getOptional() {
    return Optional.ofNullable(get());
  }

  @Override
  public <T> Key withValue(final Attribute<T> attribute, final T value) {
    if (!values.containsKey(requireNonNull(attribute))) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of this key");
    }
    final Map<Attribute<?>, Object> newKeyValues = new HashMap<>(values);
    newKeyValues.put(attribute, value);

    return new DefaultKey(definition, newKeyValues, primaryKey);
  }

  @Override
  public <T> T get(final Attribute<T> attribute) {
    if (!values.containsKey(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of key: " + definition.getEntityType());
    }

    return (T) values.get(definition.getColumnProperty(attribute).getAttribute());
  }

  @Override
  public <T> Optional<T> getOptional(final Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < attributes.size(); i++) {
      final Attribute<Object> attribute = (Attribute<Object>) attributes.get(i);
      stringBuilder.append(attribute.getName()).append(":").append(values.get(attribute));
      if (i < attributes.size() - 1) {
        stringBuilder.append(",");
      }
    }

    return stringBuilder.toString();
  }

  /**
   * Keys are equal if all attributes and their associated values are equal.
   * Empty and null keys are only equal to themselves.
   * @param object the object to check for equality
   * @return true if object is equal to this key
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || values.isEmpty()) {
      return false;
    }
    if (object.getClass() == DefaultKey.class) {
      final DefaultKey otherKey = (DefaultKey) object;
      if (isNull() || otherKey.isNull()) {
        return false;
      }

      if (attributes.size() == 1 && otherKey.attributes.size() == 1) {
        final Attribute<?> attribute = attributes.get(0);
        final Attribute<?> otherAttribute = otherKey.attributes.get(0);

        return Objects.equals(values.get(attribute), otherKey.values.get(otherAttribute)) && attribute.equals(otherAttribute);
      }

      return values.equals(otherKey.values);
    }

    return false;
  }

  /**
   * @return a hash code based on the values of this key, for single integer keys the hash code is simply the key value.
   */
  @Override
  public int hashCode() {
    if (hashCodeDirty) {
      cachedHashCode = computeHashCode();
      hashCodeDirty = false;
    }

    return cachedHashCode == null ? 0 : cachedHashCode;
  }

  @Override
  public boolean isNull() {
    if (hashCodeDirty) {
      cachedHashCode = computeHashCode();
      hashCodeDirty = false;
    }

    return cachedHashCode == null;
  }

  @Override
  public boolean isNotNull() {
    return !isNull();
  }

  @Override
  public <T> boolean isNull(final Attribute<T> attribute) {
    return values.get(attribute) == null;
  }

  @Override
  public <T> boolean isNotNull(final Attribute<T> attribute) {
    return !isNull(attribute);
  }

  private Integer computeHashCode() {
    if (values.size() == 0) {
      return null;
    }
    if (attributes.size() > 1) {
      return computeMultipleValueHashCode();
    }

    return computeSingleValueHashCode();
  }

  private Integer computeMultipleValueHashCode() {
    int hash = 0;
    for (int i = 0; i < attributes.size(); i++) {
      final ColumnProperty<?> property = definition.getColumnProperty(attributes.get(i));
      final Object value = values.get(property.getAttribute());
      if (!property.isNullable() && value == null) {
        return null;
      }
      if (value != null) {
        hash = hash + value.hashCode();
      }
    }

    return hash;
  }

  private Integer computeSingleValueHashCode() {
    final Object value = get();
    if (value == null) {
      return null;
    }
    else if (singleIntegerKey) {
      return (Integer) value;
    }

    return value.hashCode();
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainName());
    stream.writeObject(definition.getEntityType().getName());
    stream.writeBoolean(primaryKey);
    stream.writeInt(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      final Attribute<?> attribute = attributes.get(i);
      stream.writeObject(attribute.getName());
      stream.writeObject(values.get(attribute));
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final Entities entities = DefaultEntities.getEntities((String) stream.readObject());
    final EntityType<Entity> entityType = entities.getDomainType().entityType((String) stream.readObject());
    definition = entities.getDefinition(entityType);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityType);
    }
    primaryKey = stream.readBoolean();
    final int attributeCount = stream.readInt();
    values = new HashMap<>(attributeCount);
    for (int i = 0; i < attributeCount; i++) {
      final Attribute<Object> attribute = definition.getAttribute((String) stream.readObject());
      values.put(attribute, attribute.validateType(stream.readObject()));
    }
    attributes = new ArrayList<>(values.keySet());
    singleIntegerKey = attributeCount == 1 && attributes.get(0).isInteger();
    hashCodeDirty = true;
  }

  private static Map<Attribute<?>, Object> createNullValueMap(final List<Attribute<?>> attributes) {
    final Map<Attribute<?>, Object> values = new HashMap<>(attributes.size());
    for (final Attribute<?> attribute : attributes) {
      values.put(attribute, null);
    }

    return values;
  }
}
