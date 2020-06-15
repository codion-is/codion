/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Property;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public abstract class DefaultEntities implements Entities {

  private static final long serialVersionUID = 1;

  private static final Map<DomainType, Entities> REGISTERED_ENTITIES = new ConcurrentHashMap<>();

  private final DomainType domainType;
  private final Map<EntityType<? extends Entity>, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

  private transient boolean strictForeignKeys = EntityDefinition.STRICT_FOREIGN_KEYS.get();

  /**
   * Instantiates a new DefaultEntities for the given domainType
   * @param domainType the domainType
   */
  protected DefaultEntities(final DomainType domainType) {
    this.domainType = requireNonNull(domainType, "domainType");
    REGISTERED_ENTITIES.put(domainType, this);
  }

  @Override
  public DomainType getDomainType() {
    return domainType;
  }

  @Override
  public final EntityDefinition getDefinition(final EntityType<? extends Entity> entityType) {
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
  public final Entity entity(final EntityType<? extends Entity> entityType) {
    return getDefinition(entityType).entity();
  }

  @Override
  public final Entity entity(final Key key) {
    return getDefinition(key.getEntityType()).entity(key);
  }

  @Override
  public final Key key(final EntityType<? extends Entity> entityType) {
    return getDefinition(entityType).key();
  }

  @Override
  public final Key key(final EntityType<? extends Entity> entityType, final Integer value) {
    return getDefinition(entityType).key(value);
  }

  @Override
  public final Key key(final EntityType<? extends Entity> entityType, final Long value) {
    return getDefinition(entityType).key(value);
  }

  @Override
  public final List<Key> keys(final EntityType<? extends Entity> entityType, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityType, value)).collect(toList());
  }

  @Override
  public final List<Key> keys(final EntityType<? extends Entity> entityType, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> key(entityType, value)).collect(toList());
  }

  @Override
  public final List<Entity> deepCopyEntities(final List<? extends Entity> entities) {
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
  public <T extends Entity> List<T> castTo(final EntityType<T> type, final List<Entity> entities) {
    return entities.stream().map(entity -> castTo(type, entity)).collect(toList());
  }

  @Override
  public <T extends Entity> T castTo(final EntityType<T> type, final Entity entity) {
    requireNonNull(type, "type");
    if (entity == null) {
      return null;
    }
    if (type.getEntityClass().isAssignableFrom(entity.getClass())) {
      // no double wrapping
      return (T) entity;
    }
    if (!entity.getEntityType().equals(type)) {
      throw new IllegalArgumentException("Entities of type " + type + " expected, got: " + entity.getEntityType());
    }

    return (T) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[] {type.getEntityClass()}, new EntityInvoker(entity, this));
  }

  /**
   * Retrieves the Entities for the given domain type.
   * @param domainType the domain type for which to retrieve the entity definitions
   * @return the Entities instance registered for the given domainType
   * @throws IllegalArgumentException in case the domain has not been registered
   */
  static Entities getEntities(final String domainName) {
    final Entities entities = REGISTERED_ENTITIES.get(DomainType.getDomainType(domainName));
    if (entities == null) {
      throw new IllegalArgumentException("Entities for domain '" + domainName + "' have not been registered");
    }

    return entities;
  }

  protected final void setStrictForeignKeys(final boolean strictForeignKeys) {
    this.strictForeignKeys = strictForeignKeys;
  }

  protected final EntityDefinition.Builder define(final EntityType<? extends Entity> entityType, final String tableName,
                                                  final Property.Builder<?>... propertyBuilders) {
    requireNonNull(propertyBuilders, "propertyBuilders");
    final ArrayList<Property<?>> properties = new ArrayList<>();
    for (final Property.Builder<?> builder : propertyBuilders) {
      properties.add(builder.get());
    }
    final EntityDefinition.Builder definitionBuilder =
            new DefaultEntityDefinition(domainType.getName(), entityType, tableName, properties).builder();
    addDefinition((DefaultEntityDefinition) definitionBuilder.get());

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
      final EntityType<? extends Entity> entityType = definition.getEntityType();
      if (!entityType.equals(foreignKeyProperty.getReferencedEntityType()) && strictForeignKeys) {
        final EntityDefinition foreignEntity = entityDefinitions.get(foreignKeyProperty.getReferencedEntityType());
        if (foreignEntity == null) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getReferencedEntityType()
                  + "' referenced by entity '" + entityType + "' via foreign key property '"
                  + foreignKeyProperty.getAttribute() + "' has not been defined");
        }
        if (foreignEntity.getPrimaryKeyAttributes().isEmpty()) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getReferencedEntityType()
                  + "' can not be referenced via foreign key, since it has no primary key");
        }
        if (foreignKeyProperty.getColumnAttributes().size() != foreignEntity.getPrimaryKeyAttributes().size()) {
          throw new IllegalArgumentException("Number of column properties in '" +
                  entityType + "." + foreignKeyProperty.getAttribute() +
                  "' does not match the number of foreign properties in the referenced entity '" +
                  foreignKeyProperty.getReferencedEntityType() + "'");
        }
      }
    }
  }

  private void populateForeignDefinitions() {
    for (final DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
        final Attribute<Entity> foreignKeyAttribute = foreignKeyProperty.getAttribute();
        final EntityDefinition referencedDefinition = entityDefinitions.get(foreignKeyProperty.getReferencedEntityType());
        if (referencedDefinition != null && !definition.hasForeignDefinition(foreignKeyAttribute)) {
          definition.setForeignDefinition(foreignKeyAttribute, referencedDefinition);
        }
      }
    }
  }

  private void readObject(final ObjectInputStream stream) throws IOException, ClassNotFoundException {
    stream.defaultReadObject();
    REGISTERED_ENTITIES.put(domainType, this);
  }

  private static final class EntityInvoker implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1;

    private final Entity entity;
    private final Entities entities;

    private EntityInvoker(final Entity entity, final Entities entities) {
      this.entity = entity;
      this.entities = entities;
    }

    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
      if (DefaultEntityDefinition.ENTITY_METHODS.contains(method)) {
        return method.invoke(entity, args);
      }

      final EntityDefinition definition = entities.getDefinition(entity.getEntityType());
      Attribute<?> attribute = definition.getGetterAttribute(method);
      if (attribute != null) {
        final Object value = entity.get(attribute);
        if (value instanceof Entity) {
          final Entity entityValue = (Entity) value;

          return entities.castTo(entityValue.getEntityType(), entityValue);
        }

        return value;
      }
      attribute = definition.getSetterAttribute(method);
      if (attribute != null) {
        entity.put((Attribute<Object>) attribute, args[0]);

        return null;
      }

      throw new IllegalArgumentException("Unknown method: " + method);
    }
  };
}
