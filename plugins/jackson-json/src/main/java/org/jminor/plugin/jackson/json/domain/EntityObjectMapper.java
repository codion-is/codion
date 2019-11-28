/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.plugin.jackson.json.domain;

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

  private static final long serialVersionUID = 1;

  private final EntitySerializer entitySerializer;
  private final EntityDeserializer entityDeserializer;
  private final Domain domain;

  /**
   * Instantiates a new EntityObjectMapper for the given domain
   * @param domain the Domain model
   */
  public EntityObjectMapper(final Domain domain) {
    this.domain = domain;
    final SimpleModule module = new SimpleModule();
    entitySerializer = new EntitySerializer(this);
    entityDeserializer = new EntityDeserializer(domain, this);
    module.addSerializer(Entity.class, entitySerializer);
    module.addDeserializer(Entity.class, entityDeserializer);
    module.addSerializer(Entity.Key.class, new EntityKeySerializer(this));
    module.addDeserializer(Entity.Key.class, new EntityKeyDeserializer(domain, this));
    module.addSerializer(LocalTime.class, new LocalTimeSerializer());
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addSerializer(BigDecimal.class, new BigDecimalSerializer());
    registerModule(module);
  }

  /**
   * @return the underlying domain model
   */
  public Domain getDomain() {
    return domain;
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
