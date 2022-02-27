/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
import java.util.Objects;
import java.util.Optional;

import static java.util.Collections.*;

/**
 * A class representing a unique key for entities.
 */
class DefaultKey implements Key, Serializable {

  private static final long serialVersionUID = 1;

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
  public EntityType getEntityType() {
    return definition.getEntityType();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return attributes;
  }

  @Override
  public boolean isPrimaryKey() {
    return primaryKey;
  }

  @Override
  public <T> Attribute<T> getAttribute() {
    assertSingleValueKey();

    return (Attribute<T>) attributes.get(0);
  }

  @Override
  public <T> T get() {
    assertSingleValueKey();

    return (T) values.get(attributes.get(0));
  }

  @Override
  public <T> Optional<T> getOptional() {
    return Optional.ofNullable(get());
  }

  @Override
  public <T> T get(Attribute<T> attribute) {
    if (!values.containsKey(attribute)) {
      throw new IllegalArgumentException("Attribute " + attribute + " is not part of key: " + definition.getEntityType());
    }

    return (T) values.get(definition.getColumnProperty(attribute).getAttribute());
  }

  @Override
  public <T> Optional<T> getOptional(Attribute<T> attribute) {
    return Optional.ofNullable(get(attribute));
  }

  @Override
  public Builder copyBuilder() {
    return new DefaultKeyBuilder(this, definition);
  }

  @Override
  public String toString() {
    StringBuilder stringBuilder = new StringBuilder();
    for (int i = 0; i < attributes.size(); i++) {
      Attribute<Object> attribute = (Attribute<Object>) attributes.get(i);
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
      ColumnProperty<?> property = definition.getColumnProperty(attributes.get(i));
      Object value = values.get(property.getAttribute());
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
      throw new IllegalStateException("Key contains no values");
    }
    if (attributes.size() > 1) {
      throw new IllegalStateException("Key is a composite key");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainName());
    stream.writeObject(definition.getEntityType().getName());
    stream.writeInt(definition.getSerializationVersion());
    stream.writeBoolean(primaryKey);
    stream.writeInt(attributes.size());
    for (int i = 0; i < attributes.size(); i++) {
      Attribute<?> attribute = attributes.get(i);
      stream.writeObject(attribute.getName());
      stream.writeObject(values.get(attribute));
    }
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    Entities entities = DefaultEntities.getEntities((String) stream.readObject());
    definition = entities.getDefinition((String) stream.readObject());
    if (definition.getSerializationVersion() != stream.readInt()) {
      throw new IllegalArgumentException("Entity type '" + definition.getEntityType() + "' can not be deserialized due to version difference");
    }
    primaryKey = stream.readBoolean();
    int attributeCount = stream.readInt();
    values = new HashMap<>(attributeCount);
    for (int i = 0; i < attributeCount; i++) {
      Attribute<Object> attribute = definition.getAttribute((String) stream.readObject());
      values.put(attribute, attribute.validateType(stream.readObject()));
    }
    attributes = new ArrayList<>(values.keySet());
    singleIntegerKey = attributeCount == 1 && attributes.get(0).isInteger();
    hashCodeDirty = true;
  }

  private static Map<Attribute<?>, Object> createNullValueMap(List<Attribute<?>> attributes) {
    Map<Attribute<?>, Object> values = new HashMap<>(attributes.size());
    for (Attribute<?> attribute : attributes) {
      values.put(attribute, null);
    }

    return values;
  }
}
