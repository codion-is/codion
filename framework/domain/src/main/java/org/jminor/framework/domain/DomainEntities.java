/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Configuration;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.property.Property;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;

/**
 * Contains the entity definitions associated with a domain model.
 * Provides factory methods and misc. utility methods for {@link Entity}, {@link Entity.Key}.
 */
public interface DomainEntities extends EntityDefinition.Provider, Serializable {

  /**
   * Specifies whether to enable entities to be re-defined, that is,
   * allow a new definition to replace an old one.
   * Value type: Boolean<br>
   * Default value: false
   */
  PropertyValue<Boolean> ENABLE_REDEFINE_ENTITY = Configuration.booleanValue("jminor.domain.redefineEntityEnabled", false);

  /**
   * Creates a new {@link Entity} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity} instance
   */
  Entity entity(String entityId);

  /**
   * Creates a new {@link Entity} instance with the given primary key
   * @param key the primary key
   * @return a new {@link Entity} instance
   */
  Entity entity(Entity.Key key);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId
   * @param entityId the entity id
   * @return a new {@link Entity.Key} instance
   */
  Entity.Key key(String entityId);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the entity id
   * @param value the key value, assumes a single integer key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(String entityId, Integer value);

  /**
   * Creates a new {@link Entity.Key} instance with the given entityId, initialised with the given value
   * @param entityId the entity id
   * @param value the key value, assumes a single long key
   * @return a new {@link Entity.Key} instance
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or value is null
   */
  Entity.Key key(String entityId, Long value);

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the entity id
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  List<Entity.Key> keys(String entityId, Integer... values);

  /**
   * Creates new {@link Entity.Key} instances with the given entityId, initialised with the given values
   * @param entityId the entity id
   * @param values the key values, assumes a single integer key
   * @return new {@link Entity.Key} instances
   * @throws IllegalArgumentException in case the given primary key is a composite key
   * @throws NullPointerException in case entityId or values is null
   */
  List<Entity.Key> keys(String entityId, Long... values);

  /**
   * Instantiates a new {@link Entity} of the given type using the values provided by {@code valueProvider}.
   * Values are fetched for {@link org.jminor.framework.domain.property.ColumnProperty} and its descendants, {@link org.jminor.framework.domain.property.ForeignKeyProperty}
   * and {@link org.jminor.framework.domain.property.TransientProperty} (excluding its descendants).
   * If a {@link org.jminor.framework.domain.property.ColumnProperty}s underlying column has a default value the property is
   * skipped unless the property itself has a default value, which then overrides the columns default value.
   * @param entityId the entity id
   * @param valueProvider provides the default value for a given property
   * @return the populated entity
   * @see org.jminor.framework.domain.property.ColumnProperty.Builder#columnHasDefaultValue(boolean)
   * @see org.jminor.framework.domain.property.ColumnProperty.Builder#defaultValue(Object)
   */
  Entity defaultEntity(String entityId, Function<Property, Object> valueProvider);

  /**
   * Copies the given entities, with new copied instances of all foreign key value entities.
   * @param entities the entities to copy
   * @return deep copies of the entities, in the same order as they are received
   */
  List<Entity> deepCopyEntities(List<Entity> entities);

  /**
   * Copies the given entity.
   * @param entity the entity to copy
   * @return copy of the given entity
   */
  Entity copyEntity(Entity entity);

  /**
   * Copies the given entity, with new copied instances of all foreign key value entities.
   * @param entity the entity to copy
   * @return a deep copy of the given entity
   */
  Entity deepCopyEntity(Entity entity);

  /**
   * Copies the given key.
   * @param key the key to copy
   * @return a copy of the given key
   */
  Entity.Key copyKey(Entity.Key key);

  /**
   * Creates an empty Entity instance returning the given string on a call to toString(), all other
   * method calls are routed to an empty Entity instance.
   * @param entityId the entityId
   * @param toStringValue the string to return by a call to toString() on the resulting entity
   * @return an empty entity wrapping a string
   */
  Entity createToStringEntity(String entityId, String toStringValue);

  /**
   * Transforms the given entities into beans according to the information found in this Domain model
   * @param <V> the bean type
   * @param entities the entities to transform
   * @return a List containing the beans derived from the given entities, an empty List if {@code entities} is null or empty
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> List<V> toBeans(List<Entity> entities);

  /**
   * Transforms the given entity into a bean according to the information found in this Domain model
   * @param <V> the bean type
   * @param entity the entity to transform
   * @return a bean derived from the given entity
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> V toBean(Entity entity);

  /**
   * Transforms the given beans into a entities according to the information found in this Domain model
   * @param beans the beans to transform
   * @return a List containing the entities derived from the given beans, an empty List if {@code beans} is null or empty
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  List<Entity> fromBeans(List beans);

  /**
   * Creates an Entity from the given bean object.
   * @param bean the bean to convert to an Entity
   * @param <V> the bean type
   * @return a Entity based on the given bean
   * @see EntityDefinition.Builder#beanClass(Class)
   * @see Property.Builder#beanProperty(String)
   */
  <V> Entity fromBean(V bean);
}
