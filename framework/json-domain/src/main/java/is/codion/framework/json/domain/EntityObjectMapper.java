/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Entity object mapper for mapping {@link Entity} and {@link Entity.Key} to and from JSON.<br><br>
 * For instances use the {@link #entityObjectMapper(Entities)} factory method.
 */
public final class EntityObjectMapper extends ObjectMapper {

  private static final long serialVersionUID = 1;

  public static final TypeReference<List<Entity.Key>> KEY_LIST_REFERENCE = new TypeReference<List<Entity.Key>>() {};
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
    module.addSerializer(Entity.Key.class, new EntityKeySerializer(this));
    module.addDeserializer(Entity.Key.class, new EntityKeyDeserializer(entities, this));
    module.addKeyDeserializer(EntityType.class, new EntityTypeKeyDeserializer(entities));
    registerModule(module);
    registerModule(new JavaTimeModule());
    disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
  }

  /**
   * @return the underlying domain model entities
   */
  public Entities entities() {
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
  public String serializeEntities(Collection<Entity> entities) throws JsonProcessingException {
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
  public String serializeKeys(Collection<Entity.Key> keys) throws JsonProcessingException {
    return writeValueAsString(keys);
  }

  /**
   * Deserializes the given JSON string into a list of Key instances
   * @param jsonString the JSON string to parse
   * @return a List containing the Key instances represented by the given JSON string
   * @throws JsonProcessingException in case of an exception
   */
  public List<Entity.Key> deserializeKeys(String jsonString) throws JsonProcessingException {
    return readValue(jsonString, KEY_LIST_REFERENCE);
  }

  /**
   * Deserializes the given JSON input stream into a list of Key instances
   * @param inputStream the JSON input stream to parse
   * @return a List containing the Key instances represented by the given JSON input stream
   * @throws IOException in case of an exception
   */
  public List<Entity.Key> deserializeKeys(InputStream inputStream) throws IOException {
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
  public static EntityObjectMapper entityObjectMapper(Entities entities) {
    return new EntityObjectMapper(entities);
  }
}
