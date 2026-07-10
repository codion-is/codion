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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.db.report.ReportType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Entity object mapper for mapping {@link Entity} and {@link Entity.Key} to and from JSON.
 * <p>
 * For instances use the {@link #entityObjectMapper(Entities)} factory method.
 */
public final class EntityObjectMapper extends ObjectMapper {

	@Serial
	private static final long serialVersionUID = 1;

	public static final TypeReference<List<Entity.Key>> KEY_LIST_REFERENCE = new TypeReference<List<Entity.Key>>() {};
	public static final TypeReference<List<Entity>> ENTITY_LIST_REFERENCE = new TypeReference<List<Entity>>() {};

	private final Entities entities;
	private final SimpleModule module;
	private final EntitySerializer entitySerializer;
	private final EntityDeserializer entityDeserializer;
	private final ConditionSerializer conditionSerializer;
	private final ConditionDeserializer conditionDeserializer;
	private final Map<ProcedureType<?, ?>, JsonType<?>> procedureParameters = new HashMap<>();
	private final Map<FunctionType<?, ?, ?>, JsonType<?>> functionParameters = new HashMap<>();
	private final Map<ReportType<?, ?>, JsonType<?>> reportParameters = new HashMap<>();
	private final Map<FunctionType<?, ?, ?>, JsonType<?>> functionReturnTypes = new HashMap<>();

	EntityObjectMapper(Entities entities) {
		this.entities = requireNonNull(entities);
		this.entitySerializer = new EntitySerializer(this);
		this.entityDeserializer = new EntityDeserializer(entities, this);
		this.conditionSerializer = new ConditionSerializer(this);
		this.conditionDeserializer = new ConditionDeserializer(this);
		module = new SimpleModule();
		module.addSerializer(Entity.class, entitySerializer);
		module.addDeserializer(Entity.class, entityDeserializer);
		module.addSerializer(Entity.Key.class, new EntityKeySerializer(this));
		module.addDeserializer(Entity.Key.class, new EntityKeyDeserializer(this));
		module.addKeyDeserializer(EntityType.class, new EntityTypeKeyDeserializer(entities));
		module.addSerializer(Condition.class, conditionSerializer);
		module.addDeserializer(Condition.class, conditionDeserializer);
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
	 * @return the underlying module
	 */
	public Module module() {
		return module;
	}

	/**
	 * @param includeForeignKeyValues if true then the foreign key graph is included in serialized entities
	 * @return this {@link EntityObjectMapper} instance
	 */
	public EntityObjectMapper includeForeignKeyValues(boolean includeForeignKeyValues) {
		entitySerializer.setIncludeForeignKeyValues(includeForeignKeyValues);
		return this;
	}

	/**
	 * @param includeNullValues true if null values should be included in exported entities
	 * @return this {@link EntityObjectMapper} instance
	 */
	public EntityObjectMapper includeNullValues(boolean includeNullValues) {
		entitySerializer.setIncludeNullValues(includeNullValues);
		return this;
	}

	/**
	 * Serializes the given condition
	 * @param condition the condition to serialize
	 * @param generator the json generator
	 * @throws IOException in case of an exception
	 */
	public void serializeCondition(Condition condition, JsonGenerator generator) throws IOException {
		conditionSerializer.serialize(condition, generator);
	}

	/**
	 * Deserializes the given condition
	 * @param definition the entity definition
	 * @param conditionNode the condition node to deserialize
	 * @return the deserialized Condition instance
	 * @throws IOException in case of an exception
	 */
	public Condition deserializeCondition(EntityDefinition definition, JsonNode conditionNode) throws IOException {
		return conditionDeserializer.deserialize(definition, conditionNode);
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
	 * Returns the {@link JsonType} of the given procedure's parameter, for registering the type
	 * the parameter is deserialized as, when the procedure is called over a JSON connection.
	 * @param procedureType the procedure type
	 * @param <P> the procedure parameter type
	 * @return the parameter {@link JsonType} for the given procedure
	 */
	public <P> JsonType<P> parameter(ProcedureType<?, P> procedureType) {
		return (JsonType<P>) procedureParameters.computeIfAbsent(requireNonNull(procedureType),
						k -> new DefaultJsonType<>("json parameter type for procedure: " + procedureType.name(),
										"EntityObjectMapper.parameter(procedureType).set(..)"));
	}

	/**
	 * Returns the {@link JsonType} of the given function's parameter, for registering the type
	 * the parameter is deserialized as, when the function is called over a JSON connection.
	 * @param functionType the function type
	 * @param <P> the function parameter type
	 * @return the parameter {@link JsonType} for the given function
	 * @see #returnType(FunctionType)
	 */
	public <P> JsonType<P> parameter(FunctionType<?, P, ?> functionType) {
		return (JsonType<P>) functionParameters.computeIfAbsent(requireNonNull(functionType),
						k -> new DefaultJsonType<>("json parameter type for function: " + functionType.name(),
										"EntityObjectMapper.parameter(functionType).set(..)"));
	}

	/**
	 * Returns the {@link JsonType} of the given report's parameter, for registering the type
	 * the parameter is deserialized as, when the report is filled over a JSON connection.
	 * @param reportType the report type
	 * @param <P> the report parameter type
	 * @return the parameter {@link JsonType} for the given report
	 */
	public <P> JsonType<P> parameter(ReportType<P, ?> reportType) {
		return (JsonType<P>) reportParameters.computeIfAbsent(requireNonNull(reportType),
						k -> new DefaultJsonType<>("json parameter type for report: " + reportType.name(),
										"EntityObjectMapper.parameter(reportType).set(..)"));
	}

	/**
	 * Returns the {@link JsonType} of the given function's return value, for registering the type
	 * it is serialized as and deserialized from, when the function is called over a JSON connection.
	 * <p>A function called over a JSON connection requires a registered return type; the type parameter
	 * of {@link FunctionType} is erased at runtime, leaving nothing to deserialize the response as, and
	 * a type name on the wire would be a type name the client resolves, which is what JSON avoids.
	 * @param functionType the function type
	 * @param <R> the function return type
	 * @return the return {@link JsonType} for the given function
	 * @see #parameter(FunctionType)
	 */
	public <R> JsonType<R> returnType(FunctionType<?, ?, R> functionType) {
		return (JsonType<R>) functionReturnTypes.computeIfAbsent(requireNonNull(functionType),
						k -> new DefaultJsonType<>("json return type for function: " + functionType.name(),
										"EntityObjectMapper.returnType(functionType).set(..)"));
	}

	/**
	 * The type a value is serialized as and deserialized from, registered once and for all.
	 * @param <T> the value type
	 * @see EntityObjectMapper#parameter(FunctionType)
	 * @see EntityObjectMapper#returnType(FunctionType)
	 */
	public sealed interface JsonType<T> {

		/**
		 * Registers a generic type, {@code new TypeReference<List<Track>>() {}}, whose type arguments,
		 * unlike those of a {@link Class}, survive to the deserialization site.
		 * @param type the type
		 * @throws IllegalStateException in case a type has already been registered
		 */
		void set(TypeReference<T> type);

		/**
		 * @param type the type
		 * @throws IllegalStateException in case a type has already been registered
		 */
		void set(Class<T> type);

		/**
		 * @return the registered type
		 * @throws IllegalStateException in case no type has been registered
		 */
		JavaType get();
	}

	/**
	 * A factory method for {@link EntityObjectMapper} instances.
	 * @param entities the domain entities
	 * @return a new {@link EntityObjectMapper} instance based on the given entities
	 */
	public static EntityObjectMapper entityObjectMapper(Entities entities) {
		return new EntityObjectMapper(entities);
	}

	private static final class DefaultJsonType<T> implements JsonType<T> {

		private final String identifier;
		private final String registration;

		private @Nullable JavaType type;

		private DefaultJsonType(String identifier, String registration) {
			this.identifier = identifier;
			this.registration = registration;
		}

		@Override
		public void set(TypeReference<T> type) {
			//the type reference, not its raw type, a List<Track> deserialized as a
			//List leaves the caller holding a List<LinkedHashMap>
			set(TypeFactory.defaultInstance().constructType(requireNonNull(type)));
		}

		@Override
		public void set(Class<T> type) {
			set(TypeFactory.defaultInstance().constructType(requireNonNull(type)));
		}

		@Override
		public JavaType get() {
			if (type == null) {
				throw new IllegalStateException("No " + identifier + " registered, register one via " + registration);
			}

			return type;
		}

		private void set(JavaType javaType) {
			if (type != null) {
				throw new IllegalStateException(identifier + " already registered");
			}
			this.type = javaType;
		}
	}
}
