/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.valuemap.DefaultValueMap;
import org.jminor.framework.domain.property.ColumnProperty;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * A class representing a primary key for entities.
 */
final class DefaultEntityKey extends DefaultValueMap<ColumnProperty, Object> implements Entity.Key {

  private static final long serialVersionUID = 1;

  /**
   * true if this key consists of a single integer value
   */
  private boolean singleIntegerKey;

  /**
   * true if this key consists of multiple properties
   */
  private boolean compositeKey;

  /**
   * Caching the hash code
   */
  private Integer cachedHashCode = null;

  /**
   * True if the value of a key property has changed, thereby invalidating the cached hash code value
   */
  private boolean hashCodeDirty = true;

  /**
   * Caching this extremely frequently referenced object
   */
  private EntityDefinition definition;

  /**
   * Instantiates a new empty primary key, for entities without primary keys
   * @param definition the entity definition
   * @throws IllegalArgumentException in case the entity has a primary key defined
   */
  DefaultEntityKey(final EntityDefinition definition) {
    if (definition.hasPrimaryKey()) {
      throw new IllegalArgumentException("Can not create an empty key for entity '" + definition.getEntityId() + "'");
    }
    this.definition = definition;
    this.cachedHashCode = 0;
    this.hashCodeDirty = false;
  }

  /**
   * Instantiates a new DefaultKey for the given entity type, assuming it is a single value key
   * @param definition the entity definition
   * @param value the value
   * @throws IllegalArgumentException in case this key is a composite key or if the entity has no primary key
   */
  DefaultEntityKey(final EntityDefinition definition, final Object value) {
    this(definition, createSingleValueMap(definition.getPrimaryKeyProperties().get(0), value));
    if (!definition.hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + definition.getEntityId() + "' has no primary key defined");
    }
    if (compositeKey) {
      throw new IllegalArgumentException(definition.getEntityId() + " has a composite primary key");
    }
  }

  /**
   * Instantiates a new Key for the given entity type
   * @param definition the entity definition
   * @throws IllegalArgumentException in case the entity has no primary key
   */
  DefaultEntityKey(final EntityDefinition definition, final Map<ColumnProperty, Object> values) {
    super(values, null);
    if (!definition.hasPrimaryKey()) {
      throw new IllegalArgumentException("Entity '" + definition.getEntityId() + "' has no primary key defined");
    }
    this.definition = definition;
    final List<ColumnProperty> properties = definition.getPrimaryKeyProperties();
    this.compositeKey = properties.size() > 1;
    this.singleIntegerKey = !compositeKey && properties.get(0).isInteger();
  }

  @Override
  public String getEntityId() {
    return definition.getEntityId();
  }

  @Override
  public List<ColumnProperty> getProperties() {
    return definition.getPrimaryKeyProperties();
  }

  @Override
  public ColumnProperty getFirstProperty() {
    return definition.getPrimaryKeyProperties().get(0);
  }

  @Override
  public Object getFirstValue() {
    return super.get(getFirstProperty());
  }

  @Override
  public Object put(final String propertyId, final Object value) {
    return super.put(definition.getPrimaryKeyProperty(propertyId), value);
  }

  @Override
  public Object get(final String propertyId) {
    return super.get(definition.getPrimaryKeyProperty(propertyId));
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      final ColumnProperty property = primaryKeyProperties.get(i);
      stringBuilder.append(property.getPropertyId()).append(":").append(super.get(property));
      if (i < getPropertyCount() - 1) {
        stringBuilder.append(",");
      }
    }

    return stringBuilder.toString();
  }

  @Override
  public boolean isSingleIntegerKey() {
    return singleIntegerKey;
  }

  @Override
  public boolean isCompositeKey() {
    return compositeKey;
  }

  /**
   * Key objects are equal if the entityIds match as well as all property values.
   * @param obj the object to compare with
   * @return true if object is equal to this key
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj instanceof Entity.Key) {
      final String entityId = definition.getEntityId();
      final Entity.Key otherKey = (Entity.Key) obj;
      if (compositeKey) {
        return otherKey.isCompositeKey() && entityId.equals(otherKey.getEntityId()) && super.equals(otherKey);
      }
      if (singleIntegerKey) {
        return otherKey.isSingleIntegerKey() && isNull() == otherKey.isNull()
                && hashCode() == otherKey.hashCode() && entityId.equals(otherKey.getEntityId());
      }
      //single non-integer key
      return !otherKey.isCompositeKey() && entityId.equals(otherKey.getEntityId()) && Objects.equals(getFirstValue(), otherKey.getFirstValue());
    }

    return false;
  }

  /**
   * @return a hash code based on the values of this key, for single integer keys the hash code is simply the key value.
   */
  @Override
  public int hashCode() {
    updateHashCode();

    return cachedHashCode == null ? 0 : cachedHashCode;
  }

  @Override
  public boolean isNull() {
    updateHashCode();

    return cachedHashCode == null;
  }

  @Override
  public boolean isNull(final String propertyId) {
    return super.isNull(definition.getPrimaryKeyProperty(propertyId));
  }

  @Override
  public boolean isNotNull(final String propertyId) {
    return !isNull(propertyId);
  }

  @Override
  protected void clear() {
    super.clear();
    cachedHashCode = null;
    hashCodeDirty = true;
  }

  @Override
  protected Object validateAndPrepareForPut(final ColumnProperty property, final Object value) {
    return property.prepareValue(property.validateType(value));
  }

  @Override
  protected void onValuePut(final ColumnProperty property, final Object value, final Object previousValue) {
    if (singleIntegerKey) {
      setHashCode((Integer) value);
    }
    else {
      hashCodeDirty = true;
    }
  }

  private void setHashCode(final Integer value) {
    cachedHashCode = value;
    hashCodeDirty = false;
  }

  /**
   * Updates the cached hashCode in case it is dirty
   */
  private void updateHashCode() {
    if (hashCodeDirty) {
      cachedHashCode = computeHashCode();
      hashCodeDirty = false;
    }
  }

  private Integer computeHashCode() {
    if (size() == 0) {
      return null;
    }
    if (isCompositeKey()) {
      return computeMultipleValueHashCode();
    }

    return computeSingleValueHashCode();
  }

  private Integer computeMultipleValueHashCode() {
    int hash = 0;
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      final ColumnProperty property = primaryKeyProperties.get(i);
      final Object value = super.get(property);
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
    final Object value = getFirstValue();
    if (value == null) {
      return null;
    }
    else if (singleIntegerKey) {
      return (Integer) value;
    }

    return value.hashCode();
  }

  private int getPropertyCount() {
    if (compositeKey) {
      return getProperties().size();
    }

    return 1;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainId());
    stream.writeObject(definition.getEntityId());
    final List<ColumnProperty> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      stream.writeObject(super.get(primaryKeyProperties.get(i)));
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String domainId = (String) stream.readObject();
    final String entityId = (String) stream.readObject();
    definition = Domain.getDomain(domainId).getDefinition(entityId);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }
    final List<ColumnProperty> properties = definition.getPrimaryKeyProperties();
    compositeKey = properties.size() > 1;
    singleIntegerKey = !compositeKey && properties.get(0).isInteger();
    for (int i = 0; i < properties.size(); i++) {
      final ColumnProperty property = properties.get(i);
      super.put(property, property.validateType(stream.readObject()));
    }
  }

  private static Map<ColumnProperty, Object> createSingleValueMap(final ColumnProperty keyProperty, final Object value) {
    final Map<ColumnProperty, Object> values = new HashMap<>(1);
    values.put(keyProperty, value);

    return values;
  }
}
