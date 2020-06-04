/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.property.ColumnProperty;

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
final class DefaultEntityKey implements Entity.Key {

  private static final long serialVersionUID = 1;

  /**
   * Holds the values contained in this key.
   */
  private Map<Attribute<?>, Object> values;

  /**
   * true if this key consists of a single integer value
   */
  private boolean singleIntegerKey;

  /**
   * true if this key consists of multiple properties
   */
  private boolean compositeKey = false;

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
      throw new IllegalArgumentException("Can not create an empty key for entity '" + definition.getEntityType() + "'");
    }
    this.values = new HashMap<>();
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
    this.values = createSingleValueMap(definition, value);
    this.definition = definition;
    this.singleIntegerKey = definition.getPrimaryKeyAttributes().get(0).isInteger();
  }

  /**
   * Instantiates a new Key for the given entity type
   * @param definition the entity definition
   * @param values the values associated with their respective attributes
   * @throws IllegalArgumentException in case the entity has no primary key
   */
  DefaultEntityKey(final EntityDefinition definition, final Map<Attribute<?>, Object> values) {
    this.values = values == null ? new HashMap<>() : new HashMap<>(values);
    final List<Attribute<?>> attributes = definition.getPrimaryKeyAttributes();
    if (attributes.isEmpty()) {
      throw new IllegalArgumentException("Entity '" + definition.getEntityType() + "' has no primary key defined");
    }
    this.definition = definition;
    this.compositeKey = attributes.size() > 1;
    this.singleIntegerKey = !compositeKey && attributes.get(0).isInteger();
  }

  @Override
  public EntityType getEntityType() {
    return definition.getEntityType();
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return definition.getPrimaryKeyAttributes();
  }

  @Override
  public Attribute<?> getFirstAttribute() {
    return definition.getPrimaryKeyAttributes().get(0);
  }

  @Override
  public Object getFirstValue() {
    return values.get(getFirstAttribute());
  }

  @Override
  public <T> T put(final Attribute<T> attribute, final T value) {
    return (T) putInternal(definition.getPrimaryKeyProperty((Attribute<Object>) attribute), value);
  }

  @Override
  public <T> T get(final Attribute<T> attribute) {
    return (T) values.get(definition.getPrimaryKeyProperty(attribute).getAttribute());
  }

  @Override
  public String toString() {
    final StringBuilder stringBuilder = new StringBuilder();
    final List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    for (int i = 0; i < primaryKeyAttributes.size(); i++) {
      final Attribute<Object> attribute = (Attribute<Object>) primaryKeyAttributes.get(i);
      stringBuilder.append(attribute.getName()).append(":").append(values.get(attribute));
      if (i < getAttributeCount() - 1) {
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
   * Key objects are equal if the entity types match as well as all property values.
   * Empty keys are only equal to themselves.
   * @param object the object to compare with
   * @return true if object is equal to this key
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || !definition.hasPrimaryKey()) {
      return false;
    }
    if (object.getClass() ==  DefaultEntityKey.class) {
      final EntityType entityType = definition.getEntityType();
      final DefaultEntityKey otherKey = (DefaultEntityKey) object;
      if (compositeKey) {
        return otherKey.isCompositeKey() && entityType.equals(otherKey.getEntityType()) && this.values.equals(otherKey.values);
      }
      if (singleIntegerKey) {
        return otherKey.isSingleIntegerKey() && isNull() == otherKey.isNull()
                && hashCode() == otherKey.hashCode() && entityType.equals(otherKey.getEntityType());
      }
      //single non-integer key
      return !otherKey.isCompositeKey() && entityType.equals(otherKey.getEntityType()) && Objects.equals(getFirstValue(), otherKey.getFirstValue());
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

  @Override
  public int size() {
    return values.size();
  }

  private Object putInternal(final ColumnProperty<Object> property, final Object value) {
    final Object newValue = property.prepareValue(property.getAttribute().validateType(value));
    values.put(property.getAttribute(), newValue);
    if (singleIntegerKey) {
      setHashCode((Integer) value);
    }
    else {
      hashCodeDirty = true;
    }

    return newValue;
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
    final List<ColumnProperty<?>> primaryKeyProperties = definition.getPrimaryKeyProperties();
    for (int i = 0; i < primaryKeyProperties.size(); i++) {
      final ColumnProperty<?> property = primaryKeyProperties.get(i);
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
    final Object value = getFirstValue();
    if (value == null) {
      return null;
    }
    else if (singleIntegerKey) {
      return (Integer) value;
    }

    return value.hashCode();
  }

  private int getAttributeCount() {
    if (compositeKey) {
      return getAttributes().size();
    }

    return 1;
  }

  private void writeObject(final ObjectOutputStream stream) throws IOException {
    stream.writeObject(definition.getDomainName());
    stream.writeObject(definition.getEntityType().getName());
    final List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    for (int i = 0; i < primaryKeyAttributes.size(); i++) {
      stream.writeObject(values.get(primaryKeyAttributes.get(i)));
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    final String domainName = (String) stream.readObject();
    final EntityType entityType = EntityType.entityType((String) stream.readObject());
    definition = DefaultEntities.getEntities(domainName).getDefinition(entityType);
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityType);
    }
    values = new HashMap<>();
    final List<ColumnProperty<?>> properties = definition.getPrimaryKeyProperties();
    compositeKey = properties.size() > 1;
    singleIntegerKey = !compositeKey && properties.get(0).getAttribute().isInteger();
    hashCodeDirty = true;
    for (int i = 0; i < properties.size(); i++) {
      final Attribute<Object> attribute = ((ColumnProperty<Object>) properties.get(i)).getAttribute();
      values.put(attribute, attribute.validateType(stream.readObject()));
    }
  }

  private static Map<Attribute<?>, Object> createSingleValueMap(final EntityDefinition definition, final Object value) {
    final List<Attribute<?>> primaryKeyAttributes = definition.getPrimaryKeyAttributes();
    if (primaryKeyAttributes.isEmpty()) {
      throw new IllegalArgumentException("Entity '" + definition.getEntityType() + "' has no primary key defined");
    }
    if (primaryKeyAttributes.size() > 1) {
      throw new IllegalArgumentException(definition.getEntityType() + " has a composite primary key");
    }
    final Attribute<Object> attribute = (Attribute<Object>) primaryKeyAttributes.get(0);
    final Map<Attribute<?>, Object> valueMap = new HashMap<>(1);
    valueMap.put(attribute, attribute.validateType(value));

    return valueMap;
  }
}
