/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.Util;
import org.jminor.framework.domain.entity.Entities;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.EntityDefinition;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.DerivedProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.TransientProperty;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public final class DomainEntities implements Entities {

  private static final long serialVersionUID = 1;

  private static final Map<String, Entities> REGISTERED_ENTITIES = new HashMap<>();

  private final String domainId;
  private final Map<String, EntityDefinition> entityDefinitions = new LinkedHashMap<>();

  private Map<Class, EntityDefinition> beanEntities;
  private Map<String, Map<String, BeanProperty>> beanProperties;

  private transient boolean strictForeignKeys = EntityDefinition.STRICT_FOREIGN_KEYS.get();

  DomainEntities(final String domainId) {
    this.domainId = domainId;
  }

  @Override
  public String getDomainId() {
    return domainId;
  }

  @Override
  public EntityDefinition getDefinition(final String entityId) {
    final EntityDefinition definition = entityDefinitions.get(requireNonNull(entityId, "entityId"));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityId);
    }

    return definition;
  }

  @Override
  public Collection<EntityDefinition> getDefinitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public Entity entity(final String entityId) {
    return getDefinition(entityId).entity();
  }

  @Override
  public Entity entity(final Entity.Key key) {
    return getDefinition(key.getEntityId()).entity(key);
  }

  @Override
  public Entity.Key key(final String entityId) {
    return getDefinition(entityId).key();
  }

  @Override
  public Entity.Key key(final String entityId, final Integer value) {
    return getDefinition(entityId).key(value);
  }

  @Override
  public Entity.Key key(final String entityId, final Long value) {
    return getDefinition(entityId).key(value);
  }

  @Override
  public List<Entity.Key> keys(final String entityId, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityId, value)).collect(toList());
  }

  @Override
  public List<Entity.Key> keys(final String entityId, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityId, value)).collect(toList());
  }

  @Override
  public Entity defaultEntity(final String entityId, final Function<Property, Object> valueProvider) {
    final EntityDefinition entityDefinition = getDefinition(entityId);
    final Entity entity = entityDefinition.entity();
    final Collection<ColumnProperty> columnProperties = entityDefinition.getColumnProperties();
    for (final ColumnProperty property : columnProperties) {
      if (!property.isForeignKeyProperty() && !property.isDenormalized()//these are set via their respective parent properties
              && (!property.columnHasDefaultValue() || property.hasDefaultValue())) {
        entity.put(property, valueProvider.apply(property));
      }
    }
    final Collection<TransientProperty> transientProperties = entityDefinition.getTransientProperties();
    for (final TransientProperty transientProperty : transientProperties) {
      if (!(transientProperty instanceof DerivedProperty)) {
        entity.put(transientProperty, valueProvider.apply(transientProperty));
      }
    }
    final Collection<ForeignKeyProperty> foreignKeyProperties = entityDefinition.getForeignKeyProperties();
    for (final ForeignKeyProperty foreignKeyProperty : foreignKeyProperties) {
      entity.put(foreignKeyProperty, valueProvider.apply(foreignKeyProperty));
    }
    entity.saveAll();

    return entity;
  }

  @Override
  public List<Entity> deepCopyEntities(final List<Entity> entities) {
    requireNonNull(entities, "entities");

    return entities.stream().map(this::deepCopyEntity).collect(toList());
  }

  @Override
  public Entity copyEntity(final Entity entity) {
    requireNonNull(entity, "entity");
    final Entity copy = entity(entity.getEntityId());
    copy.setAs(entity);

    return copy;
  }

  @Override
  public Entity deepCopyEntity(final Entity entity) {
    requireNonNull(entity, "entity");
    final Entity copy = entity(entity.getEntityId());
    copy.setAs(entity);
    for (final ForeignKeyProperty foreignKeyProperty : getDefinition(entity.getEntityId()).getForeignKeyProperties()) {
      final Entity foreignKeyValue = (Entity) entity.get(foreignKeyProperty);
      if (foreignKeyValue != null) {
        entity.put(foreignKeyProperty, deepCopyEntity(foreignKeyValue));
      }
    }

    return copy;
  }

  @Override
  public Entity.Key copyKey(final Entity.Key key) {
    requireNonNull(key, "key");
    final Entity.Key copy = key(key.getEntityId());
    copy.setAs(key);

    return copy;
  }

  @Override
  public Entity createToStringEntity(final String entityId, final String toStringValue) {
    final Entity entity = entity(entityId);
    return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class[] {Entity.class}, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  @Override
  public <V> List<V> toBeans(final List<Entity> entities) {
    if (Util.nullOrEmpty(entities)) {
      return emptyList();
    }
    final List<V> beans = new ArrayList<>(entities.size());
    for (final Entity entity : entities) {
      beans.add(toBean(entity));
    }

    return beans;
  }

  @Override
  public <V> V toBean(final Entity entity) {
    requireNonNull(entity, "entity");
    final EntityDefinition definition = getDefinition(entity.getEntityId());
    final Class<V> beanClass = definition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class defined for entityId: " + definition.getEntityId());
    }
    final Map<String, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityId());
    try {
      final V bean = beanClass.getConstructor().newInstance();
      for (final Map.Entry<String, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = definition.getProperty(propertyEntry.getKey());
        Object value = entity.get(property);
        if (property instanceof ForeignKeyProperty && value != null) {
          value = toBean((Entity) value);
        }

        propertyEntry.getValue().setter.invoke(bean, value);
      }

      return definition.<V>getBeanHelper().toBean(entity, bean);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public List<Entity> fromBeans(final List beans) {
    if (Util.nullOrEmpty(beans)) {
      return emptyList();
    }
    final List<Entity> result = new ArrayList<>(beans.size());
    for (final Object bean : beans) {
      result.add(fromBean(bean));
    }

    return result;
  }

  @Override
  public <V> Entity fromBean(final V bean) {
    requireNonNull(bean, "bean");
    final Class beanClass = bean.getClass();
    final EntityDefinition definition = getBeanEntity(beanClass);
    final Entity entity = entity(definition.getEntityId());
    try {
      final Map<String, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityId());
      for (final Map.Entry<String, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property property = definition.getProperty(propertyEntry.getKey());
        Object value = propertyEntry.getValue().getter.invoke(bean);
        if (property instanceof ForeignKeyProperty && value != null) {
          value = fromBean(value);
        }

        entity.put(property, value);
      }

      return definition.<V>getBeanHelper().fromBean(bean, entity);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Registers this instance for lookup via {@link DomainEntities#getEntities(String)}, required for serialization
   * of domain objects, entities and related classes.
   * @return this Domain instance
   * @see #getDomainId()
   */
  @Override
  public Entities registerEntities() {
    return registerEntities(this);
  }

  /**
   * Retrieves the Entities for the domain with the given id.
   * @param domainId the id of the domain for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainId
   * @throws IllegalArgumentException in case the domain has not been registered
   * @see #registerEntities()
   */
  public static Entities getEntities(final String domainId) {
    final Entities entities = REGISTERED_ENTITIES.get(domainId);
    if (entities == null) {
      throw new IllegalArgumentException("Entities for domain '" + domainId + "' have not been registered");
    }

    return entities;
  }

  /**
   * @return all domains that have been registered via {@link #registerEntities()}
   */
  public static Collection<Entities> getRegisteredEntities() {
    return Collections.unmodifiableCollection(REGISTERED_ENTITIES.values());
  }

  void addDefinition(final EntityDefinition definition) {
    if (entityDefinitions.containsKey(definition.getEntityId()) && !ENABLE_REDEFINE_ENTITY.get()) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.getEntityId() + ", for table: " + definition.getTableName());
    }
    validateForeignKeyProperties(definition);
    entityDefinitions.put(definition.getEntityId(), definition);
    populateForeignDefinitions();
  }

  void putAll(final DomainEntities entities) {
    this.entityDefinitions.putAll(entities.entityDefinitions);
    this.beanEntities = entities.beanEntities;
    this.beanProperties = entities.beanProperties;
  }

  void setStrictForeignKeys(final boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  private void validateForeignKeyProperties(final EntityDefinition definition) {
    for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
      final String entityId = definition.getEntityId();
      if (!entityId.equals(foreignKeyProperty.getForeignEntityId()) && strictForeignKeys) {
        final EntityDefinition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityId());
        if (foreignEntity == null) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityId()
                  + "' referenced by entity '" + entityId + "' via foreign key property '"
                  + foreignKeyProperty.getPropertyId() + "' has not been defined");
        }
        if (foreignEntity.getPrimaryKeyProperties().isEmpty()) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityId()
                  + "' can not be referenced via foreign key, since it has no primary key");
        }
        if (foreignKeyProperty.getColumnProperties().size() != foreignEntity.getPrimaryKeyProperties().size()) {
          throw new IllegalArgumentException("Number of column properties in '" +
                  entityId + "." + foreignKeyProperty.getPropertyId() +
                  "' does not match the number of foreign properties in the referenced entity '" +
                  foreignKeyProperty.getForeignEntityId() + "'");
        }
      }
    }
  }

  private void populateForeignDefinitions() {
    for (final EntityDefinition definition : entityDefinitions.values()) {
      for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
        final String foreignKeyPropertyId = foreignKeyProperty.getPropertyId();
        final EntityDefinition foreignDefinition = entityDefinitions.get(foreignKeyProperty.getForeignEntityId());
        if (foreignDefinition != null && !definition.hasForeignDefinition(foreignKeyPropertyId)) {
          definition.setForeignDefinition(foreignKeyPropertyId, foreignDefinition);
        }
      }
    }
  }

  private EntityDefinition getBeanEntity(final Class beanClass) {
    if (beanEntities == null) {
      beanEntities = new HashMap<>();
    }
    if (!beanEntities.containsKey(beanClass)) {
      final Optional<EntityDefinition> optionalDefinition = getDefinitions().stream()
              .filter(entityDefinition -> Objects.equals(beanClass, entityDefinition.getBeanClass())).findFirst();
      if (!optionalDefinition.isPresent()) {
        throw new IllegalArgumentException("No entity associated with bean class: " + beanClass);
      }
      beanEntities.put(beanClass, optionalDefinition.get());
    }

    return beanEntities.get(beanClass);
  }

  private Map<String, BeanProperty> getBeanProperties(final String entityId) {
    if (beanProperties == null) {
      beanProperties = new HashMap<>();
    }

    return beanProperties.computeIfAbsent(entityId, this::initializeBeanProperties);
  }

  private Map<String, BeanProperty> initializeBeanProperties(final String entityId) {
    final EntityDefinition entityDefinition = getDefinition(entityId);
    final Class beanClass = entityDefinition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class specified for entity: " + entityId);
    }
    try {
      final Map<String, BeanProperty> map = new HashMap<>();
      for (final Property property : entityDefinition.getProperties()) {
        final String beanProperty = property.getBeanProperty();
        Class typeClass = property.getTypeClass();
        if (property instanceof ForeignKeyProperty) {
          typeClass = getDefinition(((ForeignKeyProperty) property).getForeignEntityId()).getBeanClass();
        }
        if (beanProperty != null && typeClass != null) {
          final Method getter = Util.getGetMethod(typeClass, beanProperty, beanClass);
          final Method setter = Util.getSetMethod(typeClass, beanProperty, beanClass);
          map.put(property.getPropertyId(), new BeanProperty(getter, setter));
        }
      }

      return map;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static Entities registerEntities(final Entities entities) {
    REGISTERED_ENTITIES.put(entities.getDomainId(), entities);

    return entities;
  }

  private static final class BeanProperty implements Serializable {

    private static final long serialVersionUID = 1;

    private final Method getter;
    private final Method setter;

    private BeanProperty(final Method getter, final Method setter) {
      this.getter = requireNonNull(getter, "getter");
      this.setter = requireNonNull(setter, "setter");
    }
  }
}
