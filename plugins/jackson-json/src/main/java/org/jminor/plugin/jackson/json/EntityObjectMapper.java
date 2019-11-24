/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.Entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Entity object mapper for mapping {@link Entity} and {@link Entity.Key} to and from JSON.
 */
public final class EntityObjectMapper extends ObjectMapper {

  private final EntitySerializer entitySerializer;

  public EntityObjectMapper(final Domain domain) {
    final SimpleModule module = new SimpleModule();
    entitySerializer = new EntitySerializer(this);
    module.addSerializer(Entity.class, entitySerializer);
    module.addDeserializer(Entity.class, new EntityDeserializer(domain, this));
    module.addSerializer(Entity.Key.class, new EntityKeySerializer());
    module.addDeserializer(Entity.Key.class, new EntityKeyDeserializer(domain));
    module.addSerializer(LocalTime.class, new LocalTimeSerializer());
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
    registerModule(module);
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

  /**
   * @param includeReadOnlyValues true if read only values should be included in exported entities
   * @return this {@link EntityObjectMapper} instance
   */
  public EntityObjectMapper setIncludeReadOnlyValues(final boolean includeReadOnlyValues) {
    entitySerializer.setIncludeReadOnlyValues(includeReadOnlyValues);
    return this;
  }
}
