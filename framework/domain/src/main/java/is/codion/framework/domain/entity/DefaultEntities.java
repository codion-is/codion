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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A default {@link Entities} implementation.
 */
public abstract class DefaultEntities implements Entities, Serializable {

  private static final long serialVersionUID = 1;

  private static final Map<DomainType, Entities> REGISTERED_ENTITIES = new ConcurrentHashMap<>();

  private final DomainType domainType;
  private final Map<EntityType<?>, DefaultEntityDefinition> entityDefinitions = new LinkedHashMap<>();

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
  public final EntityDefinition getDefinition(final EntityType<?> entityType) {
    final EntityDefinition definition = entityDefinitions.get(requireNonNull(entityType, "entityType"));
    if (definition == null) {
      throw new IllegalArgumentException("Undefined entity: " + entityType);
    }

    return definition;
  }

  @Override
  public final boolean contains(final EntityType<?> entityType) {
    return entityDefinitions.containsKey(requireNonNull(entityType));
  }

  @Override
  public final Collection<EntityDefinition> getDefinitions() {
    return unmodifiableCollection(entityDefinitions.values());
  }

  @Override
  public final Entity entity(final EntityType<?> entityType) {
    return getDefinition(entityType).entity();
  }

  @Override
  public final Entity entity(final Key primaryKey) {
    return getDefinition(primaryKey.getEntityType()).entity(primaryKey);
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType) {
    return getDefinition(entityType).primaryKey();
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType, final Integer value) {
    return getDefinition(entityType).primaryKey(value);
  }

  @Override
  public final Key primaryKey(final EntityType<?> entityType, final Long value) {
    return getDefinition(entityType).primaryKey(value);
  }

  @Override
  public final List<Key> primaryKeys(final EntityType<?> entityType, final Integer... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> primaryKey(entityType, value)).collect(toList());
  }

  @Override
  public final List<Key> primaryKeys(final EntityType<?> entityType, final Long... values) {
    requireNonNull(values, "values");
    return Arrays.stream(values).map(value -> primaryKey(entityType, value)).collect(toList());
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
  public final <T extends Entity> List<T> castTo(final EntityType<T> type, final List<Entity> entities) {
    return requireNonNull(entities, "entities").stream().map(entity -> castTo(type, entity)).collect(toList());
  }

  @Override
  public final <T extends Entity> T castTo(final EntityType<T> type, final Entity entity) {
    requireNonNull(type, "type");
    if (entity == null) {
      return null;
    }
    if (type.getEntityClass().isAssignableFrom(entity.getClass())) {
      // no double wrapping
      return (T) entity;
    }
    if (!entity.getEntityType().equals(type)) {
      throw new IllegalArgumentException("Entity of type " + type + " expected, got: " + entity.getEntityType());
    }

    return (T) Proxy.newProxyInstance(entity.getClass().getClassLoader(),
            new Class[] {type.getEntityClass()}, new EntityInvoker(entity, this));
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

  protected final EntityDefinition.Builder define(final EntityType<?> entityType, final String tableName,
                                                  final Property.Builder<?>... propertyBuilders) {
    requireNonNull(propertyBuilders, "propertyBuilders");
    final DefaultEntityDefinition.DefaultBuilder definitionBuilder =
            new DefaultEntityDefinition(domainType.getName(), entityType, tableName,
                    Arrays.asList(propertyBuilders)).builder();
    addDefinition(definitionBuilder.get());

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
      final EntityType<?> entityType = definition.getEntityType();
      if (!entityType.equals(foreignKeyProperty.getReferencedEntityType()) && strictForeignKeys) {
        final EntityDefinition foreignEntity = entityDefinitions.get(foreignKeyProperty.getReferencedEntityType());
        if (foreignEntity == null) {
          throw new IllegalArgumentException("Entity '" + foreignKeyProperty.getReferencedEntityType()
                  + "' referenced by entity '" + entityType + "' via foreign key property '"
                  + foreignKeyProperty.getAttribute() + "' has not been defined");
        }
      }
    }
  }

  private void populateForeignDefinitions() {
    for (final DefaultEntityDefinition definition : entityDefinitions.values()) {
      for (final ForeignKeyProperty foreignKeyProperty : definition.getForeignKeyProperties()) {
        final ForeignKeyAttribute foreignKeyAttribute = foreignKeyProperty.getAttribute();
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
      final EntityDefinition definition = entities.getDefinition(entity.getEntityType());
      if (method.getParameterCount() == 0) {
        final Attribute<?> attribute = definition.getGetterAttribute(method);
        if (attribute != null) {
          return getValue(attribute, method.getReturnType().equals(Optional.class));
        }
      }
      else if (method.getParameterCount() == 1) {
        final Attribute<?> attribute = definition.getSetterAttribute(method);
        if (attribute != null) {
          return setValue(args[0], attribute);
        }
      }
      if (method.isDefault()) {
        return definition.getDefaultMethodHandle(method).bindTo(proxy).invokeWithArguments(args);
      }

      return method.invoke(entity, args);
    }

    private Object getValue(final Attribute<?> attribute, final boolean optional) {
      Object value = entity.get(attribute);
      if (value instanceof Entity) {
        final Entity entityValue = (Entity) value;

        value = entities.castTo(entityValue.getEntityType(), entityValue);
      }

      return optional ? Optional.ofNullable(value) : value;
    }

    private Object setValue(final Object value, final Attribute<?> attribute) {
      entity.put((Attribute<Object>) attribute, value);

      return null;
    }
  }
}
