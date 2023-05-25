/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;

import static is.codion.framework.domain.entity.EntitySerializer.serializerForDomain;
import static java.util.Collections.*;
import static java.util.stream.Collectors.joining;

/**
 * A class representing a unique key for entities.
 */
class DefaultKey implements Key, Serializable {

  private static final long serialVersionUID = 1;

  /**
   * The attributes comprising this key
   */
  List<Attribute<?>> attributes;

  /**
   * True if this key represents a primary key
   */
  boolean primaryKey;

  /**
   * Holds the values contained in this key.
   */
  Map<Attribute<?>, Object> values;

  /**
   * true if this key consists of a single integer value
   */
  boolean singleIntegerKey;

  /**
   * Caching the hash code
   */
  private Integer cachedHashCode = null;

  /**
   * True until cachedHashCode has been computed
   */
  boolean hashCodeDirty = true;

  /**
   * Caching this extremely frequently referenced object
   */
  EntityDefinition definition;

  /**
   * Instantiates a new DefaultKey based on the given attributes, with the associated values as null
   * @param definition the entity definition
   * @param attributes the attributes comprising this key
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, List<Attribute<?>> attributes, boolean primaryKey) {
    this(definition, createNullValueMap(attributes), primaryKey);
    this.hashCodeDirty = false;
  }

  /**
   * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
   * @param definition the entity definition
   * @param value the value
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, Attribute<?> attribute, Object value, boolean primaryKey) {
    this(definition, singletonMap(attribute, value), primaryKey);
  }

  /**
   * Instantiates a new DefaultKey with the given values
   * @param definition the entity definition
   * @param values the values associated with their respective attributes
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, Map<Attribute<?>, Object> values, boolean primaryKey) {
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
  public EntityType type() {
    return definition.type();
  }

  @Override
  public EntityDefinition definition() {
    return definition;
  }

  @Override
  public List<Attribute<?>> attributes() {
    return attributes;
  }

  @Override
  public boolean isPrimaryKey() {
    return primaryKey;
  }

  @Override
  public <T> Attribute<T> attribute() {
    assertSingleValueKey();

    return (Attribute<T>) attributes.get(0);
  }

  @Override
  public <T> T get() {
    assertSingleValueKey();

    return (T) values.get(attributes.get(0));
  }

  @Override
  public <T> Optional<T> optional() {
    return Optional.ofNullable(get());
  }

  @Override
  public <T> T get(Attribute<T> attribute) {
    if (!values.containsKey(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of key: " + definition.type());
    }

    return (T) values.get(definition.columnProperty(attribute).attribute());
  }

  @Override
  public <T> Optional<T> optional(Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public Builder copyBuilder() {
    return new DefaultKeyBuilder(this);
  }

  @Override
  public String toString() {
    return attributes.stream()
            .map(attribute -> attribute.name() + ":" + values.get(attribute))
            .collect(joining(","));
  }

  /**
   * Keys are equal if all attributes and their associated values are equal.
   * Empty and null keys are only equal to themselves.
   * @param object the object to check for equality
   * @return true if object is equal to this key
   */
  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || values.isEmpty()) {
      return false;
    }
    if (object.getClass() == DefaultKey.class) {
      DefaultKey otherKey = (DefaultKey) object;
      if (isNull() || otherKey.isNull()) {
        return false;
      }

      if (attributes.size() == 1 && otherKey.attributes.size() == 1) {
        Attribute<?> attribute = attributes.get(0);
        Attribute<?> otherAttribute = otherKey.attributes.get(0);

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
  public boolean isNull(Attribute<?> attribute) {
    return values.get(attribute) == null;
  }

  @Override
  public boolean isNotNull(Attribute<?> attribute) {
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
      ColumnProperty<?> property = definition.columnProperty(attributes.get(i));
      Object value = values.get(property.attribute());
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
    Object value = get();
    if (value == null) {
      return null;
    }
    else if (singleIntegerKey) {
      return (Integer) value;
    }

    return value.hashCode();
  }

  private void assertSingleValueKey() {
    if (attributes.isEmpty()) {
      throw new NoSuchElementException("Key contains no values");
    }
    if (attributes.size() > 1) {
      throw new IllegalStateException("Key is a composite key");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.domainName());
    serializerForDomain(definition.domainName()).serialize(this, stream);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    serializerForDomain((String) stream.readObject()).deserialize(this, stream);
  }

  private static Map<Attribute<?>, Object> createNullValueMap(List<Attribute<?>> attributes) {
    Map<Attribute<?>, Object> values = new HashMap<>(attributes.size());
    for (Attribute<?> attribute : attributes) {
      values.put(attribute, null);
    }

    return values;
  }
}
