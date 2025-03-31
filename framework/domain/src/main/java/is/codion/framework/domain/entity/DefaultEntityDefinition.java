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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Text;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKey.Reference;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.condition.ConditionProvider;
import is.codion.framework.domain.entity.condition.ConditionType;
import is.codion.framework.domain.entity.query.SelectQuery;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static is.codion.common.Text.nullOrEmpty;
import static java.util.Collections.*;
import static java.util.Comparator.comparingInt;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.*;

/**
 * A class encapsulating an entity definition, such as table name, order by clause and attributes.
 */
final class DefaultEntityDefinition implements EntityDefinition, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	private static final String ATTRIBUTE = "attribute";
	private static final String COLUMN = "column";

	private final EntityType entityType;
	private final String caption;
	private final String captionResourceKey;
	private transient String resourceCaption;
	private final String description;
	private final OrderBy orderBy;
	private final boolean readOnly;
	private final boolean smallDataset;
	private final boolean keyGenerated;
	private final Function<Entity, String> stringFactory;
	private final boolean cacheToString;
	private final Comparator<Entity> comparator;
	private final EntityValidator validator;
	private final Predicate<Entity> exists;
	private final transient String tableName;
	private final transient String selectTableName;
	private final transient KeyGenerator keyGenerator;
	private final transient boolean optimisticLocking;
	private final transient SelectQuery selectQuery;
	private final transient Map<ConditionType, ConditionProvider> conditionProviders;
	private final Map<ForeignKey, EntityDefinition> referencedEntities = new HashMap<>();
	private final EntityAttributes entityAttributes;
	private final PrimaryKey primaryKey = new DefaultPrimaryKey();
	private final Attributes attributes = new DefaultAttributes();
	private final Columns columns = new DefaultColumns();
	private final ForeignKeys foreignKeys = new DefaultForeignKeys();

	private DefaultEntityDefinition(DefaultBuilder builder) {
		this.entityType = builder.attributes.entityType;
		this.caption = builder.caption;
		this.captionResourceKey = builder.captionResourceKey;
		this.description = builder.description;
		this.orderBy = builder.orderBy;
		this.readOnly = builder.readOnly;
		this.smallDataset = builder.smallDataset;
		this.keyGenerator = builder.keyGenerator;
		this.keyGenerated = builder.keyGenerated;
		this.optimisticLocking = builder.optimisticLocking;
		this.stringFactory = builder.stringFactory;
		this.cacheToString = builder.cacheToString;
		this.comparator = builder.comparator;
		this.validator = builder.validator;
		this.exists = builder.exists;
		this.tableName = builder.tableName;
		this.selectTableName = builder.selectTableName;
		this.selectQuery = builder.selectQuery;
		this.conditionProviders = builder.conditionProviders == null ? null : new HashMap<>(builder.conditionProviders);
		this.entityAttributes = builder.attributes;
	}

	@Override
	public EntityType type() {
		return entityType;
	}

	@Override
	public String tableName() {
		return tableName;
	}

	@Override
	public ConditionProvider condition(ConditionType conditionType) {
		requireNonNull(conditionType);
		if (conditionProviders != null) {
			ConditionProvider conditionProvider = conditionProviders.get(conditionType);
			if (conditionProvider != null) {
				return conditionProvider;
			}
		}

		throw new IllegalArgumentException("ConditionProvider for type " + conditionType + " not found");
	}

	@Override
	public String caption() {
		String resourceBundleName = entityType.resourceBundleName().orElse(null);
		if (resourceBundleName != null) {
			if (resourceCaption == null) {
				ResourceBundle bundle = getBundle(resourceBundleName);
				resourceCaption = bundle.containsKey(captionResourceKey) ? bundle.getString(captionResourceKey) : "";
			}

			if (!resourceCaption.isEmpty()) {
				return resourceCaption;
			}
		}

		return caption == null ? entityType.name() : caption;
	}

	@Override
	public Optional<String> description() {
		return Optional.ofNullable(description);
	}

	@Override
	public boolean smallDataset() {
		return smallDataset;
	}

	@Override
	public boolean readOnly() {
		return readOnly;
	}

	@Override
	public boolean optimisticLocking() {
		return optimisticLocking;
	}

	@Override
	public Optional<OrderBy> orderBy() {
		return Optional.ofNullable(orderBy);
	}

	@Override
	public String selectTableName() {
		return selectTableName == null ? tableName : selectTableName;
	}

	@Override
	public Optional<SelectQuery> selectQuery() {
		return Optional.ofNullable(selectQuery);
	}

	@Override
	public Function<Entity, String> stringFactory() {
		return stringFactory;
	}

	@Override
	public boolean cacheToString() {
		return cacheToString;
	}

	@Override
	public Comparator<Entity> comparator() {
		return comparator;
	}

	@Override
	public PrimaryKey primaryKey() {
		return primaryKey;
	}

	@Override
	public Attributes attributes() {
		return attributes;
	}

	@Override
	public Columns columns() {
		return columns;
	}

	@Override
	public ForeignKeys foreignKeys() {
		return foreignKeys;
	}

	@Override
	public String toString() {
		return entityType.name();
	}

	@Override
	public EntityValidator validator() {
		return validator;
	}

	@Override
	public Predicate<Entity> exists() {
		return exists;
	}

	@Override
	public Entity entity() {
		return entity(emptyMap());
	}

	@Override
	public Entity entity(Map<Attribute<?>, Object> values) {
		return entity(values, emptyMap());
	}

	@Override
	public Entity entity(Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
		return new DefaultEntity(this, values, originalValues);
	}

	@Override
	public <T> Entity.Key primaryKey(T value) {
		if (primaryKey.columns().isEmpty()) {
			throw new IllegalArgumentException("Entity '" + entityType + "' has no primary key");
		}
		if (primaryKey.columns().size() > 1) {
			throw new IllegalStateException(entityType + " has a composite primary key");
		}
		Column<T> column = (Column<T>) primaryKey.columns().get(0);
		column.type().validateType(value);

		return new DefaultKey(this, column, value, true);
	}

	/**
	 * Returns true if an entity definition has been associated with the given foreign key.
	 * @param foreignKey the foreign key
	 * @return true if the referenced entity definition has been set for the given foreign key
	 */
	boolean hasReferencedEntityDefinition(ForeignKey foreignKey) {
		return referencedEntities.containsKey(foreignKey);
	}

	/**
	 * Associates the given definition with the given foreign key.
	 * @param foreignKey the foreign key attribute
	 * @param definition the entity definition referenced by the given foreign key
	 * @throws IllegalStateException in case the foreign definition has already been set
	 * @throws IllegalArgumentException in case the definition does not match the foreign key
	 */
	void setReferencedEntityDefinition(ForeignKey foreignKey, EntityDefinition definition) {
		if (referencedEntities.containsKey(foreignKey)) {
			throw new IllegalStateException("Foreign definition has already been set for " + foreignKey);
		}
		if (!foreignKey.referencedType().equals(definition.type())) {
			throw new IllegalArgumentException("Definition for entity " + foreignKey.referencedType() +
							" expected for " + foreignKey);
		}
		referencedEntities.put(foreignKey, definition);
	}

	private final class DefaultAttributes implements Attributes, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public Collection<Attribute<?>> get() {
			return entityAttributes.attributeDefinitions.stream()
							.map(AttributeDefinition::attribute)
							.collect(toList());
		}

		@Override
		public Collection<AttributeDefinition<?>> definitions() {
			return entityAttributes.attributeDefinitions;
		}

		@Override
		public <T> Collection<Attribute<?>> derivedFrom(Attribute<T> attribute) {
			return entityAttributes.derivedAttributes.getOrDefault(requireNonNull(attribute, ATTRIBUTE), emptySet());
		}

		@Override
		public boolean contains(Attribute<?> attribute) {
			return entityAttributes.attributeMap.containsKey(requireNonNull(attribute));
		}

		@Override
		public <T> Attribute<T> get(String attributeName) {
			return (Attribute<T>) entityAttributes.attributeNameMap.get(requireNonNull(attributeName));
		}

		@Override
		public Collection<Attribute<?>> selected() {
			return entityAttributes.defaultSelectAttributes;
		}

		@Override
		public <T> AttributeDefinition<T> definition(Attribute<T> attribute) {
			AttributeDefinition<T> definition = (AttributeDefinition<T>) entityAttributes.attributeMap.get(requireNonNull(attribute, ATTRIBUTE));
			if (definition == null) {
				throw new IllegalArgumentException("Attribute " + attribute + " not found in entity: " + entityType);
			}

			return definition;
		}

		@Override
		public Collection<AttributeDefinition<?>> updatable() {
			List<ColumnDefinition<?>> updatableColumns = entityAttributes.columnDefinitions.stream()
							.filter(ColumnDefinition::updatable)
							.filter(column -> (!column.primaryKey() || !primaryKey.generated()))
							.collect(toList());
			updatableColumns.removeIf(column -> foreignKeys.foreignKeyColumn(column.attribute()));
			List<AttributeDefinition<?>> updatable = new ArrayList<>(updatableColumns);
			for (ForeignKeyDefinition definition : entityAttributes.foreignKeyDefinitions) {
				if (foreignKeys.updatable(definition.attribute())) {
					updatable.add(definition);
				}
			}

			return updatable;
		}
	}

	private final class DefaultColumns implements Columns, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public Collection<Column<?>> get() {
			return entityAttributes.columns;
		}

		@Override
		public Collection<ColumnDefinition<?>> definitions() {
			return entityAttributes.columnDefinitions;
		}

		@Override
		public Collection<Column<String>> searchable() {
			return entityAttributes.columnDefinitions.stream()
							.filter(ColumnDefinition::searchable)
							.map(column -> ((ColumnDefinition<String>) column).attribute())
							.collect(toList());
		}

		@Override
		public <T> ColumnDefinition<T> definition(Column<T> column) {
			AttributeDefinition<T> definition = (AttributeDefinition<T>) entityAttributes.attributeMap.get(requireNonNull(column, COLUMN));
			if (definition == null) {
				throw new IllegalArgumentException("Column " + column + " not found in entity: " + entityType);
			}
			if (!(definition instanceof ColumnDefinition)) {
				throw new IllegalArgumentException("Column " + column + " has not been defined as a column");
			}

			return (ColumnDefinition<T>) definition;
		}
	}

	private final class DefaultForeignKeys implements ForeignKeys, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public Collection<ForeignKeyDefinition> definitions() {
			return entityAttributes.foreignKeyDefinitions;
		}

		@Override
		public Collection<ForeignKey> get() {
			return entityAttributes.foreignKeyDefinitionMap.keySet();
		}

		@Override
		public EntityDefinition referencedBy(ForeignKey foreignKey) {
			definition(foreignKey);
			EntityDefinition definition = referencedEntities.get(foreignKey);
			if (definition == null) {
				throw new IllegalArgumentException("Referenced entity definition not found for foreign key: " + foreignKey);
			}

			return definition;
		}

		@Override
		public boolean updatable(ForeignKey foreignKey) {
			definition(foreignKey);
			return foreignKey.references().stream()
							.map(reference -> columns.definition(reference.column()))
							.allMatch(ColumnDefinition::updatable);
		}

		@Override
		public boolean foreignKeyColumn(Column<?> column) {
			attributes.definition(column);
			return entityAttributes.foreignKeyColumns.contains(column);
		}

		@Override
		public Collection<ForeignKey> get(EntityType referencedEntityType) {
			requireNonNull(referencedEntityType);
			return get().stream()
							.filter(foreignKey -> foreignKey.referencedType().equals(referencedEntityType))
							.collect(toList());
		}

		@Override
		public ForeignKeyDefinition definition(ForeignKey foreignKey) {
			ForeignKeyDefinition definition = entityAttributes.foreignKeyDefinitionMap.get(requireNonNull(foreignKey));
			if (definition == null) {
				throw new IllegalArgumentException("Foreign key: " + foreignKey + " not found in entity of type: " + entityType);
			}

			return definition;
		}

		@Override
		public <T> Collection<ForeignKeyDefinition> definitions(Column<T> column) {
			return entityAttributes.columnForeignKeyDefinitions
							.getOrDefault(requireNonNull(column, COLUMN), emptyList());
		}
	}

	private final class DefaultPrimaryKey implements PrimaryKey, Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		@Override
		public List<Column<?>> columns() {
			return entityAttributes.primaryKeyColumns;
		}

		@Override
		public List<ColumnDefinition<?>> definitions() {
			return entityAttributes.primaryKeyColumnDefinitions;
		}

		@Override
		public KeyGenerator generator() {
			return keyGenerator;
		}

		@Override
		public boolean generated() {
			return keyGenerated;
		}
	}

	private static final class EntityAttributes implements Serializable {

		@Serial
		private static final long serialVersionUID = 1;

		private final EntityType entityType;
		private final Map<String, Attribute<?>> attributeNameMap;
		private final Map<Attribute<?>, AttributeDefinition<?>> attributeMap;
		private final List<AttributeDefinition<?>> attributeDefinitions;
		private final List<ColumnDefinition<?>> columnDefinitions;
		private final Collection<Column<?>> columns;
		private final List<Column<?>> primaryKeyColumns;
		private final List<ColumnDefinition<?>> primaryKeyColumnDefinitions;
		private final List<ForeignKeyDefinition> foreignKeyDefinitions;
		private final Map<ForeignKey, ForeignKeyDefinition> foreignKeyDefinitionMap;
		private final Map<Column<?>, Collection<ForeignKeyDefinition>> columnForeignKeyDefinitions;
		private final Set<Column<?>> foreignKeyColumns = new HashSet<>();
		private final Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes;
		private final List<Attribute<?>> defaultSelectAttributes;

		private EntityAttributes(EntityType entityType, List<AttributeDefinition.Builder<?, ?>> attributeDefinitionBuilders) {
			this.entityType = requireNonNull(entityType);
			if (requireNonNull(attributeDefinitionBuilders).isEmpty()) {
				throw new IllegalArgumentException("One or more attribute definition builder must be specified when defining an entity");
			}
			List<EntityType> attributeEntityTypes = attributeDefinitionBuilders.stream()
							.map(builder -> builder.attribute().entityType())
							.distinct()
							.collect(toList());
			if (attributeEntityTypes.size() > 1) {
				throw new IllegalArgumentException("Multiple entityTypes found among attribute definitions: " + attributeEntityTypes);
			}
			if (!entityType.equals(attributeEntityTypes.get(0))) {
				throw new IllegalArgumentException("Entity definition: " + entityType + ", " + attributeEntityTypes.get(0) + " found in attribute definitions");
			}
			this.attributeMap = unmodifiableMap(attributeMap(attributeDefinitionBuilders));
			this.attributeNameMap = unmodifiableMap(attributeNameMap(attributeMap));
			this.attributeDefinitions = unmodifiableList(new ArrayList<>(attributeMap.values()));
			this.columnDefinitions = unmodifiableList(columnDefinitions());
			this.columns = unmodifiableList(columnDefinitions.stream()
							.map(ColumnDefinition::attribute)
							.collect(toList()));
			this.primaryKeyColumnDefinitions = unmodifiableList(primaryKeyColumnDefinitions());
			this.primaryKeyColumns = unmodifiableList(primaryKeyColumns());
			this.foreignKeyDefinitions = unmodifiableList(foreignKeyDefinitions());
			this.foreignKeyDefinitionMap = unmodifiableMap(foreignKeyDefinitionMap());
			this.columnForeignKeyDefinitions = unmodifiableMap(columnForeignKeyDefinitions());
			this.derivedAttributes = unmodifiableMap(derivedAttributes());
			this.defaultSelectAttributes = unmodifiableList(defaultSelectAttributes());
		}

		private Map<Attribute<?>, AttributeDefinition<?>> attributeMap(List<AttributeDefinition.Builder<?, ?>> builders) {
			preventDuplicateAttributeNames(builders);
			Map<Attribute<?>, AttributeDefinition<?>> attributes = new HashMap<>(builders.size());
			for (AttributeDefinition.Builder<?, ?> builder : builders) {
				if (!(builder instanceof ForeignKeyDefinition.Builder)) {
					validateAndAddAttribute(builder.build(), attributes, entityType);
				}
			}
			validatePrimaryKeyAttributes(attributes, entityType);

			configureForeignKeyColumns(builders.stream()
							.filter(ForeignKeyDefinition.Builder.class::isInstance)
							.map(ForeignKeyDefinition.Builder.class::cast)
							.collect(toList()), attributes);
			for (AttributeDefinition.Builder<?, ?> builder : builders) {
				if (builder instanceof ForeignKeyDefinition.Builder) {
					validateAndAddAttribute(builder.build(), attributes, entityType);
				}
			}
			Map<Attribute<?>, AttributeDefinition<?>> ordereredMap = new LinkedHashMap<>(builders.size());
			//retain the original attribute order
			for (AttributeDefinition.Builder<?, ?> builder : builders) {
				ordereredMap.put(builder.attribute(), attributes.get(builder.attribute()));
			}

			return ordereredMap;
		}

		private Map<Column<?>, Collection<ForeignKeyDefinition>> columnForeignKeyDefinitions() {
			Map<Column<?>, Collection<ForeignKeyDefinition>> foreignKeyMap = new HashMap<>();
			foreignKeyDefinitions.forEach(foreignKeyDefinition ->
							foreignKeyDefinition.references().forEach(reference ->
											foreignKeyMap.computeIfAbsent(reference.column(),
															columnAttribute -> new ArrayList<>()).add(foreignKeyDefinition)));

			return foreignKeyMap;
		}

		private void configureForeignKeyColumns(List<ForeignKeyDefinition.Builder> foreignKeyBuilders,
																						Map<Attribute<?>, AttributeDefinition<?>> attributeMap) {
			Map<ForeignKey, List<ColumnDefinition<?>>> foreignKeyColumnDefinitions = foreignKeyBuilders.stream()
							.map(ForeignKeyDefinition.Builder::attribute)
							.map(ForeignKey.class::cast)
							.collect(toMap(Function.identity(), foreignKey -> foreignKeyColumnDefinitions(foreignKey, attributeMap)));
			foreignKeyColumns.addAll(foreignKeyColumnDefinitions.values().stream()
							.flatMap(definitions -> definitions.stream().map(ColumnDefinition::attribute))
							.collect(toSet()));
			foreignKeyBuilders.forEach(foreignKeyBuilder -> setForeignKeyNullable(foreignKeyBuilder, foreignKeyColumnDefinitions));
		}

		private List<ForeignKeyDefinition> foreignKeyDefinitions() {
			return attributeDefinitions.stream()
							.filter(ForeignKeyDefinition.class::isInstance)
							.map(ForeignKeyDefinition.class::cast)
							.collect(toList());
		}

		private List<ColumnDefinition<?>> columnDefinitions() {
			return attributeDefinitions.stream()
							.filter(ColumnDefinition.class::isInstance)
							.map(column -> (ColumnDefinition<?>) column)
							.collect(toList());
		}

		private List<Attribute<?>> defaultSelectAttributes() {
			List<Attribute<?>> selectableAttributes = columnDefinitions.stream()
							.filter(ColumnDefinition::selected)
							.map(AttributeDefinition::attribute)
							.collect(toList());
			selectableAttributes.addAll(foreignKeyDefinitions.stream()
							.map(ForeignKeyDefinition::attribute)
							.filter(this::basedOnSelectedColumns)
							.collect(toList()));

			return selectableAttributes;
		}

		private boolean basedOnSelectedColumns(ForeignKey foreignKey) {
			return Collections.disjoint(foreignKey.references().stream()
							.map(Reference::column)
							.collect(toSet()), columnDefinitions.stream()
							.filter(column -> !column.selected())
							.map(ColumnDefinition::attribute)
							.collect(toSet()));
		}

		private Map<Attribute<?>, Set<Attribute<?>>> derivedAttributes() {
			Map<Attribute<?>, Set<Attribute<?>>> derivedAttributeMap = new HashMap<>();
			attributeDefinitions.stream()
							.filter(DerivedAttributeDefinition.class::isInstance)
							.map(DerivedAttributeDefinition.class::cast)
							.forEach(derivedAttribute -> {
								List<Attribute<?>> sourceAttributes = derivedAttribute.sourceAttributes();
								validateSourceAttributes(sourceAttributes);
								for (Attribute<?> sourceAttribute : sourceAttributes) {
									derivedAttributeMap.computeIfAbsent(sourceAttribute, attribute -> new HashSet<>()).add(derivedAttribute.attribute());
								}
							});

			return derivedAttributeMap;
		}

		private List<ColumnDefinition<?>> primaryKeyColumnDefinitions() {
			return attributeDefinitions.stream()
							.filter(ColumnDefinition.class::isInstance)
							.map(column -> (ColumnDefinition<?>) column)
							.filter(ColumnDefinition::primaryKey)
							.sorted(comparingInt(ColumnDefinition::primaryKeyIndex))
							.collect(toList());
		}

		private List<Column<?>> primaryKeyColumns() {
			return primaryKeyColumnDefinitions.stream()
							.map(ColumnDefinition::attribute)
							.collect(toList());
		}

		private void validateSourceAttributes(List<Attribute<?>> sourceAttributes) {
			sourceAttributes.forEach(sourceAttribute -> {
				if (!attributeMap.containsKey(sourceAttribute)) {
					throw new IllegalArgumentException("Source attribute " + sourceAttribute + " is not part of entity");
				}
			});
		}

		private static void preventDuplicateAttributeNames(List<AttributeDefinition.Builder<?, ?>> builders) {
			Set<String> attributeNames = new HashSet<>();
			for (AttributeDefinition.Builder<?, ?> builder : builders) {
				if (attributeNames.contains(builder.attribute().name())) {
					throw new IllegalArgumentException("Duplicate attribute name: " + builder.attribute().name());
				}
				attributeNames.add(builder.attribute().name());
			}
		}

		private static Map<String, Attribute<?>> attributeNameMap(Map<Attribute<?>, AttributeDefinition<?>> attributeDefinitions) {
			return attributeDefinitions.keySet().stream()
							.collect(Collectors.toMap(Attribute::name, Function.identity()));
		}

		private static void validateAndAddAttribute(AttributeDefinition<?> definition, Map<Attribute<?>,
						AttributeDefinition<?>> attributeDefinitions, EntityType entityType) {
			validate(definition, attributeDefinitions, entityType);
			attributeDefinitions.put(definition.attribute(), definition);
		}

		private static void validatePrimaryKeyAttributes(Map<Attribute<?>, AttributeDefinition<?>> attributeDefinitions, EntityType entityType) {
			Set<Integer> usedPrimaryKeyIndexes = new LinkedHashSet<>();
			for (AttributeDefinition<?> definition : attributeDefinitions.values()) {
				if (definition instanceof ColumnDefinition && ((ColumnDefinition<?>) definition).primaryKey()) {
					Integer index = ((ColumnDefinition<?>) definition).primaryKeyIndex();
					if (usedPrimaryKeyIndexes.contains(index)) {
						throw new IllegalArgumentException("Primary key index " + index + " in column " + definition + " has already been used");
					}
					usedPrimaryKeyIndexes.add(index);
				}
			}
			usedPrimaryKeyIndexes.stream()
							.min(Integer::compareTo)
							.ifPresent(minPrimaryKeyIndex -> {
								if (minPrimaryKeyIndex != 0) {
									throw new IllegalArgumentException("Minimum primary key index is "
													+ minPrimaryKeyIndex + " for entity " + entityType + ", when it should be 0");
								}
							});
			usedPrimaryKeyIndexes.stream()
							.max(Integer::compareTo)
							.ifPresent(maxPrimaryKeyIndex -> {
								if (usedPrimaryKeyIndexes.size() != maxPrimaryKeyIndex + 1) {
									throw new IllegalArgumentException("Expecting " + (maxPrimaryKeyIndex + 1)
													+ " primary key columns for entity " + entityType + ", but found only "
													+ usedPrimaryKeyIndexes.size() + " distinct primary key indexes "
													+ usedPrimaryKeyIndexes.stream().sorted().collect(toList()));
								}
							});
		}

		private static void validate(AttributeDefinition<?> definition, Map<Attribute<?>, AttributeDefinition<?>> attributeDefinitions, EntityType entityType) {
			if (!entityType.equals(definition.entityType())) {
				throw new IllegalArgumentException("Attribute entityType (" +
								definition.entityType() + ") in attribute " + definition.attribute() +
								" does not match the definition entityType: " + entityType);
			}
			if (attributeDefinitions.containsKey(definition.attribute())) {
				throw new IllegalArgumentException("Attribute " + definition.attribute()
								+ (definition.caption() != null ? " (" + definition.caption() + ")" : "")
								+ " has already been defined as: " + attributeDefinitions.get(definition.attribute()) + " in entity: " + entityType);
			}
		}

		private Map<ForeignKey, ForeignKeyDefinition> foreignKeyDefinitionMap() {
			return foreignKeyDefinitions.stream()
							.collect(Collectors.toMap(ForeignKeyDefinition::attribute, Function.identity()));
		}

		private static List<ColumnDefinition<?>> foreignKeyColumnDefinitions(ForeignKey foreignKey, Map<Attribute<?>,
						AttributeDefinition<?>> attributeDefinitions) {
			return foreignKey.references().stream()
							.map(reference -> foreignKeyColumnDefinition(reference, attributeDefinitions, foreignKey))
							.collect(toList());
		}

		private static ColumnDefinition<?> foreignKeyColumnDefinition(Reference<?> reference, Map<Attribute<?>,
						AttributeDefinition<?>> attributeMap, ForeignKey foreignKey) {
			ColumnDefinition<?> definition = (ColumnDefinition<?>) attributeMap.get(reference.column());
			if (definition == null) {
				throw new IllegalArgumentException("Column: " + reference.column()
								+ " not found when initializing foreign key: " + foreignKey);
			}

			return definition;
		}

		private static void setForeignKeyNullable(ForeignKeyDefinition.Builder foreignKeyBuilder,
																							Map<ForeignKey, List<ColumnDefinition<?>>> foreignKeyColumnDefinitions) {
			//make foreign keys nullable if and only if any of their constituent columns are nullable
			foreignKeyBuilder.nullable(foreignKeyColumnDefinitions.get(foreignKeyBuilder.attribute()).stream()
							.anyMatch(AttributeDefinition::nullable));
		}
	}

	static final class DefaultBuilder implements Builder {

		private final EntityAttributes attributes;

		private String tableName;
		private Map<ConditionType, ConditionProvider> conditionProviders;
		private String caption;
		private String captionResourceKey;
		private String description;
		private boolean smallDataset;
		private boolean readOnly;
		private KeyGenerator keyGenerator = DefaultEntity.DEFAULT_KEY_GENERATOR;
		private boolean keyGenerated;
		private boolean optimisticLocking = OPTIMISTIC_LOCKING.getOrThrow();
		private OrderBy orderBy;
		private String selectTableName;
		private SelectQuery selectQuery;
		private Function<Entity, String> stringFactory = DefaultEntity.DEFAULT_STRING_FACTORY;
		private boolean cacheToString = true;
		private Comparator<Entity> comparator = Text.collator();
		private EntityValidator validator = DefaultEntity.DEFAULT_VALIDATOR;
		private Predicate<Entity> exists = DefaultEntity.DEFAULT_EXISTS;

		DefaultBuilder(EntityType entityType, List<AttributeDefinition.Builder<?, ?>> attributeDefinitionBuilders) {
			this.attributes = new EntityAttributes(entityType, attributeDefinitionBuilders);
			this.tableName = attributes.entityType.name();
			this.captionResourceKey = attributes.entityType.name();
		}

		@Override
		public Builder tableName(String tableName) {
			if (nullOrEmpty(tableName)) {
				throw new IllegalArgumentException("Table name must be non-empty");
			}
			this.tableName = tableName;
			return this;
		}

		@Override
		public Builder condition(ConditionType conditionType, ConditionProvider conditionProvider) {
			requireNonNull(conditionType);
			requireNonNull(conditionProvider);
			if (this.conditionProviders == null) {
				this.conditionProviders = new HashMap<>();
			}
			if (this.conditionProviders.containsKey(conditionType)) {
				throw new IllegalStateException("ConditionProvider for condition type  " + conditionType + " has already been added");
			}
			this.conditionProviders.put(conditionType, conditionProvider);
			return this;
		}

		@Override
		public Builder caption(String caption) {
			this.caption = requireNonNull(caption);
			return this;
		}

		@Override
		public Builder captionResourceKey(String captionResourceKey) {
			if (this.caption != null) {
				throw new IllegalStateException("Caption has already been set for entity: " + attributes.entityType);
			}
			this.captionResourceKey = requireNonNull(captionResourceKey);
			return this;
		}

		@Override
		public Builder description(String description) {
			this.description = description;
			return this;
		}

		@Override
		public Builder smallDataset(boolean smallDataset) {
			this.smallDataset = smallDataset;
			return this;
		}

		@Override
		public Builder readOnly(boolean readOnly) {
			this.readOnly = readOnly;
			return this;
		}

		@Override
		public Builder optimisticLocking(boolean optimisticLocking) {
			this.optimisticLocking = optimisticLocking;
			return this;
		}

		@Override
		public Builder keyGenerator(KeyGenerator keyGenerator) {
			if (attributes.primaryKeyColumnDefinitions.isEmpty()) {
				throw new IllegalStateException("KeyGenerator can not be set for an entity without a primary key: " + attributes.entityType);
			}
			this.keyGenerator = requireNonNull(keyGenerator);
			this.keyGenerated = true;
			return this;
		}

		@Override
		public Builder orderBy(OrderBy orderBy) {
			this.orderBy = requireNonNull(orderBy);
			return this;
		}

		@Override
		public Builder selectTableName(String selectTableName) {
			this.selectTableName = requireNonNull(selectTableName);
			return this;
		}

		@Override
		public Builder selectQuery(SelectQuery selectQuery) {
			this.selectQuery = requireNonNull(selectQuery);
			return this;
		}

		@Override
		public Builder comparator(Comparator<Entity> comparator) {
			this.comparator = requireNonNull(comparator);
			return this;
		}

		@Override
		public Builder stringFactory(Attribute<?> attribute) {
			return stringFactory(StringFactory.builder()
							.value(attribute)
							.build());
		}

		@Override
		public Builder stringFactory(Function<Entity, String> stringFactory) {
			this.stringFactory = requireNonNull(stringFactory);
			return this;
		}

		@Override
		public Builder cacheToString(boolean cacheToString) {
			this.cacheToString = cacheToString;
			return this;
		}

		@Override
		public Builder validator(EntityValidator validator) {
			this.validator = requireNonNull(validator);
			return this;
		}

		@Override
		public Builder exists(Predicate<Entity> exists) {
			this.exists = requireNonNull(exists);
			return this;
		}

		@Override
		public EntityDefinition build() {
			return new DefaultEntityDefinition(this);
		}
	}
}
