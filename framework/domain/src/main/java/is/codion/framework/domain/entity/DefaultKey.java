/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;

/**
 * A class representing a unique key for entities.
 */
class DefaultKey implements Entity.Key, Serializable {

  private static final long serialVersionUID = 1;

  private static final Map<String, EntitySerializer> SERIALIZERS = new ConcurrentHashMap<>();

  /**
   * The columns comprising this key
   */
  List<Column<?>> columns;

  /**
   * True if this key represents a primary key
   */
  boolean primaryKey;

  /**
   * Holds the values contained in this key.
   */
  Map<Column<?>, Object> values;

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
   * @param columns the attributes comprising this key
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, List<Column<?>> columns, boolean primaryKey) {
    this(definition, createNullValueMap(columns), primaryKey);
    this.hashCodeDirty = false;
  }

  /**
   * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
   * @param definition the entity definition
   * @param column the column
   * @param value the value
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, Column<?> column, Object value, boolean primaryKey) {
    this(definition, singletonMap(column, value), primaryKey);
  }

  /**
   * Instantiates a new DefaultKey with the given values
   * @param definition the entity definition
   * @param values the values associated with their respective attributes
   * @param primaryKey true if this key represents a primary key
   */
  DefaultKey(EntityDefinition definition, Map<Column<?>, Object> values, boolean primaryKey) {
    values.forEach((column, value) -> ((Column<Object>) column).type().validateType(value));
    this.values = unmodifiableMap(values);
    this.columns = unmodifiableList(new ArrayList<>(values.keySet()));
    this.definition = definition;
    this.primaryKey = primaryKey;
    if (!this.columns.isEmpty()) {
      this.singleIntegerKey = columns.size() == 1 && columns.get(0).type().isInteger();
    }
  }

  @Override
  public EntityType entityType() {
    return definition.entityType();
  }

  @Override
  public EntityDefinition entityDefinition() {
    return definition;
  }

  @Override
  public Collection<Column<?>> columns() {
    return columns;
  }

  @Override
  public boolean isPrimaryKey() {
    return primaryKey;
  }

  @Override
  public <T> Column<T> column() {
    assertSingleValueKey();

    return (Column<T>) columns.get(0);
  }

  @Override
  public <T> T get() {
    assertSingleValueKey();

    return (T) values.get(columns.get(0));
  }

  @Override
  public <T> Optional<T> optional() {
    return Optional.ofNullable(get());
  }

  @Override
  public <T> T get(Column<T> column) {
    if (!values.containsKey(requireNonNull(column))) {
      throw new IllegalArgumentException("Column " + column + " is not part of key: " + definition.entityType());
    }

    return (T) values.get(definition.columns().definition(column).attribute());
  }

  @Override
  public <T> Optional<T> optional(Column<T> column) {
    return Optional.ofNullable(get(column));
  }

  @Override
  public Builder copyBuilder() {
    return new DefaultKeyBuilder(this);
  }

  @Override
  public String toString() {
    return columns.stream()
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

      if (columns.size() == 1 && otherKey.columns.size() == 1) {
        Column<?> column = columns.get(0);
        Column<?> otherColumn = otherKey.columns.get(0);

        return Objects.equals(values.get(column), otherKey.values.get(otherColumn)) && column.equals(otherColumn);
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
  public boolean isNull(Column<?> column) {
    return values.get(column) == null;
  }

  @Override
  public boolean isNotNull(Column<?> column) {
    return !isNull(column);
  }

  private Integer computeHashCode() {
    if (values.isEmpty()) {
      return null;
    }
    if (columns.size() > 1) {
      return computeMultipleValueHashCode();
    }

    return computeSingleValueHashCode();
  }

  private Integer computeMultipleValueHashCode() {
    int hash = 0;
    for (int i = 0; i < columns.size(); i++) {
      ColumnDefinition<?> columnDefinition = definition.columns().definition(columns.get(i));
      Object value = values.get(columnDefinition.attribute());
      if (!columnDefinition.isNullable() && value == null) {
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
    if (columns.isEmpty()) {
      throw new NoSuchElementException("Key contains no values");
    }
    if (columns.size() > 1) {
      throw new IllegalStateException("Key is a composite key");
    }
  }

  private void writeObject(ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.entityType().domainType().name());
    EntitySerializer.serialize(this, stream);
  }

  private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
    serializerForDomain((String) stream.readObject()).deserialize(this, stream);
  }

  private static Map<Column<?>, Object> createNullValueMap(List<Column<?>> columns) {
    Map<Column<?>, Object> values = new HashMap<>(columns.size());
    for (Column<?> column : columns) {
      values.put(column, null);
    }

    return values;
  }

  static void setSerializer(String domainName, EntitySerializer serializer) {
    SERIALIZERS.put(requireNonNull(domainName), requireNonNull(serializer));
  }

  /**
   * Returns the serializer associated with the given domain name
   * @param domainName the domain name
   * @return the serializer to use for the given domain
   * @throws IllegalArgumentException in case no serializer has been associated with the domain
   */
  static EntitySerializer serializerForDomain(String domainName) {
    EntitySerializer serializer = SERIALIZERS.get(requireNonNull(domainName));
    if (serializer == null) {
      throw new IllegalArgumentException("No EntitySerializer found for domain: " + domainName);
    }

    return serializer;
  }
}
