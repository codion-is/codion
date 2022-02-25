/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.jackson.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Entity object mapper for mapping {@link Entity} and {@link Key} to and from JSON.<br><br>
 * For instances use the {@link #createEntityObjectMapper(Entities)} factory method.
 */
public final class EntityObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  public static final TypeReference<List<Key>> KEY_LIST_REFERENCE = new TypeReference<List<Key>>() {};
  public static final TypeReference<List<Entity>> ENTITY_LIST_REFERENCE = new TypeReference<List<Entity>>() {};

  private final SimpleModule module = new SimpleModule();
  private final EntitySerializer entitySerializer;
  private final EntityDeserializer entityDeserializer;
  private final Entities entities;

  EntityObjectMapper(Entities entities) {
    this.entities = requireNonNull(entities, "entities");
    this.entitySerializer = new EntitySerializer(this);
    this.entityDeserializer = new EntityDeserializer(entities, this);
    module.addSerializer(Entity.class, entitySerializer);
    module.addDeserializer(Entity.class, entityDeserializer);
    module.addSerializer(Key.class, new EntityKeySerializer(this));
    module.addDeserializer(Key.class, new EntityKeyDeserializer(entities, this));
    module.addSerializer(LocalTime.class, new LocalTimeSerializer());
    module.addSerializer(LocalDate.class, new LocalDateSerializer());
    module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer());
    module.addSerializer(OffsetDateTime.class, new OffsetDateTimeSerializer());
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
  public EntityObjectMapper setIncludeForeignKeyValues(boolean includeForeignKeyValues) {
    entitySerializer.setIncludeForeignKeyValues(includeForeignKeyValues);
    return this;
  }

  /**
   * @param includeNullValues true if null values should be included in exported entities
   * @return this {@link EntityObjectMapper} instance
   */
  public EntityObjectMapper setIncludeNullValues(boolean includeNullValues) {
    entitySerializer.setIncludeNullValues(includeNullValues);
    return this;
  }

  /**
   * Serializes the given Entity instances into a JSON string array
   * @param entities the entities
   * @return a JSON string representation of the given entities
   * @throws JsonProcessingException in case of an exception
   */
  public String serializeEntities(List<Entity> entities) throws JsonProcessingException {
    return writeValueAsString(entities);
  }

  /**
   * Deserializes the given JSON string into a list of Entity instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Entity instances represented by the given JSON string
   * @throws JsonProcessingException in case of an exception
   */
  public List<Entity> deserializeEntities(String jsonString) throws JsonProcessingException {
    return readValue(jsonString, ENTITY_LIST_REFERENCE);
  }

  /**
   * Deserializes the given JSON input stream into a list of Entity instances
   * @param inputStream the JSON input stream to parse
   * @return a List containing the Entity instances represented by the given JSON input stream
   * @throws IOException in case of an exception
   */
  public List<Entity> deserializeEntities(InputStream inputStream) throws IOException {
    return readValue(inputStream, ENTITY_LIST_REFERENCE);
  }

  /**
   * Serializes the given Key instances into a JSON string array
   * @param keys the keys
   * @return a JSON string representation of the given entity keys
   * @throws JsonProcessingException in case of an exception
   */
  public String serializeKeys(List<Key> keys) throws JsonProcessingException {
    return writeValueAsString(keys);
  }

  /**
   * Deserializes the given JSON string into a list of Key instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Key instances represented by the given JSON string
   * @throws JsonProcessingException in case of an exception
   */
  public List<Key> deserializeKeys(String jsonString) throws JsonProcessingException {
    return readValue(jsonString, KEY_LIST_REFERENCE);
  }

  /**
   * Deserializes the given JSON input stream into a list of Key instances
   * @param inputStream the JSON input stream to parse
   * @return a List containing the Key instances represented by the given JSON input stream
   * @throws IOException in case of an exception
   */
  public List<Key> deserializeKeys(InputStream inputStream) throws IOException {
    return readValue(inputStream, KEY_LIST_REFERENCE);
  }

  /**
   * Adds a serializer to this EntityObjectMapper instance.
   * @param clazz the class
   * @param serializer the serializer
   * @param <T> the type
   */
  public <T> void addSerializer(Class<? extends T> clazz, StdSerializer<T> serializer) {
    module.addSerializer(requireNonNull(clazz), requireNonNull(serializer));
  }

  /**
   * Adds a deserializer to this EntityObjectMapper instance.
   * @param clazz the class
   * @param deserializer the deserializer
   * @param <T> the type
   */
  public <T> void addDeserializer(Class<T> clazz, StdDeserializer<? extends T> deserializer) {
    module.addDeserializer(requireNonNull(clazz), requireNonNull(deserializer));
  }

  /**
   * A factory method for {@link EntityObjectMapper} instances.
   * @param entities the domain entities
   * @return a new {@link EntityObjectMapper} instance based on the given entities
   */
  public static EntityObjectMapper createEntityObjectMapper(Entities entities) {
    return new EntityObjectMapper(entities);
  }
}
