/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Util;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public abstract class DefaultEntities implements Entities {

  private static final long serialVersionUID = 1;

  private static final Map<String, Entities> REGISTERED_ENTITIES = new ConcurrentHashMap<>();

  private final String domainName;
  private final Map<EntityType, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

  private Map<Class<?>, EntityDefinition> beanEntities;
  private Map<EntityType, Map<Attribute<?>, BeanProperty>> beanProperties;

  private transient boolean strictForeignKeys = EntityDefinition.STRICT_FOREIGN_KEYS.get();

  /**
   * Instantiates a new DefaultEntities for the given domainId
   * @param domainName the domainId
   */
  protected DefaultEntities(final String domainName) {
    this.domainName = requireNonNull(domainName, "domainId");
    register(domainName);
  }

  @Override
  public final String getDomainName() {
    return domainName;
  }

  @Override
  public final EntityDefinition getDefinition(final EntityType entityType) {
    final EntityDefinition definition = entityDefinitions.get(requireNonNull(entityType, "entityType"));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityType);
    }

    return definition;
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public final Entity entity(final EntityType entityType) {
    return getDefinition(entityType).entity();
  }

  @Override
  public final Entity entity(final Entity.Key key) {
    return getDefinition(key.getEntityType()).entity(key);
  }

  @Override
  public final Entity.Key key(final EntityType entityType) {
    return getDefinition(entityType).key();
  }

  @Override
  public final Entity.Key key(final EntityType entityType, final Integer value) {
    return getDefinition(entityType).key(value);
  }

  @Override
  public final Entity.Key key(final EntityType entityType, final Long value) {
    return getDefinition(entityType).key(value);
  }

  @Override
  public final List<Entity.Key> keys(final EntityType entityType, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityType, value)).collect(toList());
  }

  @Override
  public final List<Entity.Key> keys(final EntityType entityType, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityType, value)).collect(toList());
  }

  @Override
  public final List<Entity> deepCopyEntities(final List<Entity> entities) {
    requireNonNull(entities, "entities");

    return entities.stream().map(this::deepCopyEntity).collect(toList());
  }

  @Override
  public final Entity copyEntity(final Entity entity) {
    requireNonNull(entity, "entity");
    final Entity copy = entity(entity.getEntityType());
    copy.setAs(entity);

    return copy;
  }

  @Override
  public final Entity deepCopyEntity(final Entity entity) {
    requireNonNull(entity, "entity");
    final Entity copy = entity(entity.getEntityType());
    copy.setAs(entity);
    for (final ForeignKeyProperty foreignKeyProperty : getDefinition(entity.getEntityType()).getForeignKeyProperties()) {
      final Entity foreignKeyValue = entity.get(foreignKeyProperty.getAttribute());
      if (foreignKeyValue != null) {
        entity.put(foreignKeyProperty.getAttribute(), deepCopyEntity(foreignKeyValue));
      }
    }

    return copy;
  }

  @Override
  public final Entity createToStringEntity(final EntityType entityType, final String toStringValue) {
    final Entity entity = entity(entityType);
    return (Entity) Proxy.newProxyInstance(Entity.class.getClassLoader(), new Class[] {Entity.class}, (proxy, method, args) -> {
      if ("toString".equals(method.getName())) {
        return toStringValue;
      }

      return method.invoke(entity, args);
    });
  }

  @Override
  public final <V> List<V> toBeans(final List<Entity> entities) {
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
  public final <V> V toBean(final Entity entity) {
    requireNonNull(entity, "entity");
    final EntityDefinition definition = getDefinition(entity.getEntityType());
    final Class<V> beanClass = definition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class defined for entityType: " + definition.getEntityType());
    }
    final Map<Attribute<?>, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityType());
    try {
      final V bean = beanClass.getConstructor().newInstance();
      for (final Map.Entry<Attribute<?>, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property<?> property = definition.getProperty(propertyEntry.getKey());
        Object value = entity.get(property.getAttribute());
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
  public final List<Entity> fromBeans(final List<Object> beans) {
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
  public final <V> Entity fromBean(final V bean) {
    requireNonNull(bean, "bean");
    final Class<V> beanClass = (Class<V>) bean.getClass();
    final EntityDefinition definition = getBeanEntityDefinition(beanClass);
    final Entity entity = entity(definition.getEntityType());
    try {
      final Map<Attribute<?>, BeanProperty> beanPropertyMap = getBeanProperties(definition.getEntityType());
      for (final Map.Entry<Attribute<?>, BeanProperty> propertyEntry : beanPropertyMap.entrySet()) {
        final Property<?> property = definition.getProperty(propertyEntry.getKey());
        Object value = propertyEntry.getValue().getter.invoke(bean);
        if (property instanceof ForeignKeyProperty && value != null) {
          value = fromBean(value);
        }

        entity.put(property.getAttribute(), value);
      }

      return definition.<V>getBeanHelper().fromBean(bean, entity);
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Retrieves the Entities for the domain with the given id.
   * @param domainName the id of the domain for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainName
   * @throws IllegalArgumentException in case the domain has not been registered
   */
  static Entities getEntities(final String domainName) {
    final Entities entities = REGISTERED_ENTITIES.get(domainName);
    if (entities == null) {
      throw new IllegalArgumentException("Entities for domain '" + domainName + "' have not been registered");
    }

    return entities;
  }

  protected final void setStrictForeignKeys(final boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  protected final EntityDefinition.Builder define(final EntityType entityType, final String tableName,
                                                  final Property.Builder<?>... propertyBuilders) {
    final EntityDefinition.Builder definitionBuilder =
            new DefaultEntityDefinition(entityType, tableName, propertyBuilders).builder();
    addDefinition((DefaultEntityDefinition) definitionBuilder.domainName(domainName).get());

    return definitionBuilder;
  }

  private void addDefinition(final DefaultEntityDefinition definition) {
    if (entityDefinitions.containsKey(definition.getEntityType())) {
      throw new IllegalArgumentException("Entity has already been defined: " +
              definition.getEntityType() + ", for table: " + definition.getTableName());
    }
    validateForeignKeyProperties(definition);
    entityDefinitions.put(definition.getEntityType(), definition);
    populateForeignDefinitions();
  }

  private void validateForeignKeyProperties(final EntityDefinition definition) {
    for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
      final EntityType entityType = definition.getEntityType();
      if (!entityType.equals(foreignKeyProperty.getForeignEntityType()) && strictForeignKeys) {
        final EntityDefinition foreignEntity = entityDefinitions.get(foreignKeyProperty.getForeignEntityType());
        if (foreignEntity == null) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityType()
                  + "' referenced by entity '" + entityType + "' via foreign key property '"
                  + foreignKeyProperty.getAttribute() + "' has not been defined");
        }
        if (foreignEntity.getPrimaryKeyAttributes().isEmpty()) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getForeignEntityType()
                  + "' can not be referenced via foreign key, since it has no primary key");
        }
        if (foreignKeyProperty.getColumnAttributes().size() != foreignEntity.getPrimaryKeyAttributes().size()) {
          throw new IllegalArgumentException("Number of column properties in '" +
                  entityType + "." + foreignKeyProperty.getAttribute() +
                  "' does not match the number of foreign properties in the referenced entity '" +
                  foreignKeyProperty.getForeignEntityType() + "'");
        }
      }
    }
  }

  private void populateForeignDefinitions() {
    for (final DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
        final Attribute<Entity> foreignKeyAttribute = foreignKeyProperty.getAttribute();
        final EntityDefinition foreignDefinition = entityDefinitions.get(foreignKeyProperty.getForeignEntityType());
        if (foreignDefinition != null && !definition.hasForeignDefinition(foreignKeyAttribute)) {
          definition.setForeignDefinition(foreignKeyAttribute, foreignDefinition);
        }
      }
    }
  }

  private EntityDefinition getBeanEntityDefinition(final Class<?> beanClass) {
    if (beanEntities == null) {
      beanEntities = new HashMap<>();
    }
    if (!beanEntities.containsKey(beanClass)) {
      final Optional<DefaultEntityDefinition> optionalDefinition = entityDefinitions.values().stream()
              .filter(entityDefinition -> Objects.equals(beanClass, entityDefinition.getBeanClass())).findFirst();
      if (!optionalDefinition.isPresent()) {
        throw new IllegalArgumentException("No entity associated with bean class: " + beanClass);
      }
      beanEntities.put(beanClass, optionalDefinition.get());
    }

    return beanEntities.get(beanClass);
  }

  private Map<Attribute<?>, BeanProperty> getBeanProperties(final EntityType entityType) {
    if (beanProperties == null) {
      beanProperties = new HashMap<>();
    }

    return beanProperties.computeIfAbsent(entityType, this::initializeBeanProperties);
  }

  private Map<Attribute<?>, BeanProperty> initializeBeanProperties(final EntityType entityType) {
    final EntityDefinition entityDefinition = getDefinition(entityType);
    final Class<?> beanClass = entityDefinition.getBeanClass();
    if (beanClass == null) {
      throw new IllegalArgumentException("No bean class specified for entity: " + entityType);
    }
    try {
      final Map<Attribute<?>, BeanProperty> map = new HashMap<>();
      for (final Property<?> property : entityDefinition.getProperties()) {
        final String beanProperty = property.getBeanProperty();
        Class<?> typeClass = property.getAttribute().getTypeClass();
        if (property instanceof ForeignKeyProperty) {
          typeClass = getDefinition(((ForeignKeyProperty) property).getForeignEntityType()).getBeanClass();
        }
        if (beanProperty != null && typeClass != null) {
          final Method getter = Util.getGetMethod(typeClass, beanProperty, beanClass);
          final Method setter = Util.getSetMethod(typeClass, beanProperty, beanClass);
          map.put(property.getAttribute(), new BeanProperty(getter, setter));
        }
      }

      return map;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void register(final String domainName) {
    REGISTERED_ENTITIES.put(domainName, this);
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    register(domainName);
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
