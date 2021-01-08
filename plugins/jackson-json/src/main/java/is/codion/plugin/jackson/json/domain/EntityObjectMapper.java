/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

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
    this.entities = requireNonNull(entities, "entities");
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

  /**
   * Serializes the given Entity instances into a JSON string array
   * @param entities the entities
   * @return a JSON string representation of the given entities
   * @throws JsonProcessingException in case of an exception
   */
  public String serializeEntities(final List<Entity> entities) throws JsonProcessingException {
    return writeValueAsString(entities);
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   * @throws JsonProcessingException in case of an exception
   */
  public List<Entity> deserializeEntities(final String jsonString) throws JsonProcessingException {
    return readValue(jsonString, ENTITY_LIST_REFERENCE);
  }

  /**
   * Deserializes the given JSON input stream into a list of Entity instances
   * @param inputStream the JSON input stream to parse
   * @return a List containing the Entity instances represented by the given JSON input stream
   * @throws IOException in case of an exception
   */
  public List<Entity> deserializeEntities(final InputStream inputStream) throws IOException {
    return readValue(inputStream, ENTITY_LIST_REFERENCE);
  }

  /**
   * Serializes the given Key instances into a JSON string array
   * @param keys the keys
   * @return a JSON string representation of the given entity keys
   * @throws JsonProcessingException in case of an exception
   */
  public String serializeKeys(final List<Key> keys) throws JsonProcessingException {
    return writeValueAsString(keys);
  }

  /**
   * Deserializes the given JSON string into a list of Key instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Key instances represented by the given JSON string
   * @throws JsonProcessingException in case of an exception
   */
  public List<Key> deserializeKeys(final String jsonString) throws JsonProcessingException {
    return readValue(jsonString, KEY_LIST_REFERENCE);
  }

  /**
   * Deserializes the given JSON input stream into a list of Key instances
   * @param inputStream the JSON input stream to parse
   * @return a List containing the Key instances represented by the given JSON input stream
   * @throws IOException in case of an exception
   */
  public List<Key> deserializeKeys(final InputStream inputStream) throws IOException {
    return readValue(inputStream, KEY_LIST_REFERENCE);
  }
}
