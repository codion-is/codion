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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.json.domain;

import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.condition.Condition;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serial;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
	private final Map<ProcedureType<?, ?>, DefaultProcedureDefinition<?>> procedureDefinitions = new HashMap<>();
	private final Map<FunctionType<?, ?, ?>, DefaultFunctionDefinition<?, ?>> functionDefinitions = new HashMap<>();

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
	 * Define the argument type for a procedure
	 * @param procedureType the procedure type
	 * @param <T> the argument type
	 * @return the {@link ProcedureArgumentTypeDefiner}
	 * @throws IllegalStateException in case this procedure has already been defined
	 */
	public <T> ProcedureArgumentTypeDefiner<T> define(ProcedureType<?, T> procedureType) {
		return new DefaultProcedureArgumentTypeDefiner<>(requireNonNull(procedureType));
	}

	/**
	 * Define the return type for a function
	 * @param functionType the function type
	 * @param <T> the argument type
	 * @param <R> the return type
	 * @return the {@link FunctionArgumentTypeDefiner}
	 * @throws IllegalStateException in case this function has already been defined
	 */
	public <T, R> FunctionReturnTypeDefiner<T, R> define(FunctionType<?, T, R> functionType) {
		return new DefaultFunctionReturnTypeDefiner<>(requireNonNull(functionType));
	}

	/**
	 * @param procedureType the procedure type
	 * @param <T> the argument type
	 * @return the procedure definition
	 */
	public <T> ProcedureDefinition<T> procedure(ProcedureType<?, T> procedureType) {
		ProcedureDefinition<T> definition = (ProcedureDefinition<T>) procedureDefinitions.get(requireNonNull(procedureType));
		if (definition == null) {
			throw new IllegalArgumentException("Procedure not defined " + procedureType);
		}

		return definition;
	}

	/**
	 * @param functionType the function type
	 * @param <T> the argument type
	 * @param <R> the return type
	 * @return the function definition
	 */
	public <T, R> FunctionDefinition<T, R> function(FunctionType<?, T, R> functionType) {
		FunctionDefinition<T, R> definition = (FunctionDefinition<T, R>) functionDefinitions.get(requireNonNull(functionType));
		if (definition == null) {
			throw new IllegalArgumentException("Function not defined " + functionType);
		}

		return definition;
	}

	/**
	 * @param <T> the argument type
	 */
	public interface ProcedureDefinition<T> {

		/**
		 * @return the argument type
		 */
		Class<T> argumentType();
	}

	/**
	 * @param <T> the argument type
	 * @param <R> the return type
	 */
	public interface FunctionDefinition<T, R> {

		/**
		 * @return the argument type or an empty {@link Optional} in case none is defined
		 */
		Optional<Class<T>> argumentType();

		/**
		 * @return the return type
		 */
		Class<R> returnType();
	}

	/**
	 * @param <T> the argument type
	 */
	public interface ProcedureArgumentTypeDefiner<T> {

		/**
		 * @param argumentType the argument type
		 */
		void argumentType(TypeReference<T> argumentType);

		/**
		 * @param argumentType the argument type
		 */
		void argumentType(Class<T> argumentType);
	}

	/**
	 * @param <T> the argument type
	 * @param <R> the return type
	 */
	public interface FunctionReturnTypeDefiner<T, R> {

		/**
		 * @param returnType the return type
		 * @return the function definer
		 */
		FunctionArgumentTypeDefiner<T> returnType(TypeReference<R> returnType);

		/**
		 * @param returnType the return type
		 * @return the function definer
		 */
		FunctionArgumentTypeDefiner<T> returnType(Class<R> returnType);
	}

	/**
	 * @param <T> the argument type
	 */
	public interface FunctionArgumentTypeDefiner<T> {

		/**
		 * @param argumentType the argument type
		 */
		void argumentType(TypeReference<T> argumentType);

		/**
		 * @param argumentType the argument type
		 */
		void argumentType(Class<T> argumentType);
	}

	private final class DefaultProcedureArgumentTypeDefiner<T> implements ProcedureArgumentTypeDefiner<T> {

		private final ProcedureType<?, T> procedureType;

		private DefaultProcedureArgumentTypeDefiner(ProcedureType<?, T> procedureType) {
			if (procedureDefinitions.containsKey(procedureType)) {
				throw new IllegalStateException("Procedure already defined " + procedureType);
			}
			this.procedureType = procedureType;
		}

		@Override
		public void argumentType(TypeReference<T> argumentType) {
			argumentType(rawType(requireNonNull(argumentType)));
		}

		@Override
		public void argumentType(Class<T> argumentType) {
			procedureDefinitions.computeIfAbsent(procedureType, k -> new DefaultProcedureDefinition<>(requireNonNull(argumentType)));
		}
	}

	private final class DefaultFunctionReturnTypeDefiner<T, R> implements FunctionReturnTypeDefiner<T, R> {

		private final FunctionType<?, T, R> functionType;

		private DefaultFunctionReturnTypeDefiner(FunctionType<?, T, R> functionType) {
			if (functionDefinitions.containsKey(functionType)) {
				throw new IllegalStateException("Function already defined " + functionType);
			}
			this.functionType = functionType;
		}

		@Override
		public FunctionArgumentTypeDefiner<T> returnType(TypeReference<R> returnType) {
			return returnType(rawType(requireNonNull(returnType)));
		}

		@Override
		public FunctionArgumentTypeDefiner<T> returnType(Class<R> returnType) {
			return new DefaultFunctionArgumentTypeDefiner<>(functionType, requireNonNull(returnType));
		}
	}

	private final class DefaultFunctionArgumentTypeDefiner<T, R> implements FunctionArgumentTypeDefiner<T> {

		private final FunctionType<?, T, R> functionType;

		private DefaultFunctionArgumentTypeDefiner(FunctionType<?, T, R> functionType, Class<R> returnType) {
			this.functionType = requireNonNull(functionType);
			functionDefinitions.put(functionType, new DefaultFunctionDefinition<>(returnType, null));
		}

		@Override
		public void argumentType(TypeReference<T> argumentType) {
			argumentType(rawType(requireNonNull(argumentType)));
		}

		@Override
		public void argumentType(Class<T> argumentType) {
			functionDefinitions.compute(functionType, (k, definition) ->
							new DefaultFunctionDefinition<>(definition.returnType, requireNonNull(argumentType)));
		}
	}

	private static final class DefaultProcedureDefinition<T> implements ProcedureDefinition<T> {

		private final Class<T> argumentType;

		private DefaultProcedureDefinition(Class<T> argumentType) {
			this.argumentType = argumentType;
		}

		@Override
		public Class<T> argumentType() {
			return argumentType;
		}
	}

	private static final class DefaultFunctionDefinition<T, R> implements FunctionDefinition<T, R> {

		private final Class<R> returnType;
		private final @Nullable Class<T> argumentType;

		private DefaultFunctionDefinition(Class<R> returnType, @Nullable Class<T> argumentType) {
			this.returnType = returnType;
			this.argumentType = argumentType;
		}

		@Override
		public Class<R> returnType() {
			return returnType;
		}

		@Override
		public Optional<Class<T>> argumentType() {
			return Optional.ofNullable(argumentType);
		}
	}

	/**
	 * A factory method for {@link EntityObjectMapper} instances.
	 * @param entities the domain entities
	 * @return a new {@link EntityObjectMapper} instance based on the given entities
	 */
	public static EntityObjectMapper entityObjectMapper(Entities entities) {
		return new EntityObjectMapper(entities);
	}

	private static <T> Class<T> rawType(TypeReference<T> typeReference) {
		Type type = typeReference.getType();
		if (type instanceof ParameterizedType) {
			return (Class<T>) ((ParameterizedType) type).getRawType();
		}
		else if (type instanceof Class<?>) {
			return (Class<T>) type;
		}

		throw new IllegalArgumentException("Cannot extract raw type from: " + type);
	}
}
