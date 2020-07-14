/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Entity object mapper for mapping {@link Entity} and {@link Key} to and from JSON.
 */
public final class EntityObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  public static final TypeReference<List<Key>> KEY_LIST_REFERENCE = new TypeReference<List<Key>>() {};
  public static final TypeReference<List<Entity>> ENTITY_LIST_REFERENCE = new TypeReference<List<Entity>>() {};

  private final EntitySerializer entitySerializer;
  private final EntityDeserializer entityDeserializer;
  private final Entities entities;

  /**
   * Instantiates a new EntityObjectMapper for the given domain
   * @param entities the domain model entities
   */
  public EntityObjectMapper(final Entities entities) {
    this.entities = entities;
    this.entitySerializer = new EntitySerializer(this);
    this.entityDeserializer = new EntityDeserializer(entities, this);
    final SimpleModule module = new SimpleModule();
    module.addSerializer(Entity.class, entitySerializer);
    module.addDeserializer(Entity.class, entityDeserializer);
    module.addSerializer(Key.class, new EntityKeySerializer(this));
    module.addDeserializer(Key.class, new EntityKeyDeserializer(entities, this));
    module.addSerializer(LocalTime.class, new LocalTimeSerializer());
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
    registerModule(module);
  }

  /**
   * @return the underlying domain model entities
   */
  public Entities getEntities() {
    return entities;
  }

  /**
   * @param includeForeignKeyValues if true then the foreign key graph is included in serialized entities
   * @return this {@link EntityObjectMapper} instance
   */
  public EntityObjectMapper setIncludeForeignKeyValues(final boolean includeForeignKeyValues) {
    entitySerializer.setIncludeForeignKeyValues(includeForeignKeyValues);
    return this;
  }

  /**
   * @param includeNullValues true if null values should be included in exported entities
   * @return this {@link EntityObjectMapper} instance
   */
  public EntityObjectMapper setIncludeNullValues(final boolean includeNullValues) {
    entitySerializer.setIncludeNullValues(includeNullValues);
    return this;
  }
}
