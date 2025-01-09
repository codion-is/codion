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
 * Copyright (c) 2008 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.DerivedAttribute;
import is.codion.framework.domain.entity.attribute.DerivedAttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.TransientAttributeDefinition;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import static is.codion.framework.domain.entity.DefaultKey.serializerForDomain;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

class DefaultEntity implements Entity, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	static final DefaultKeyGenerator DEFAULT_KEY_GENERATOR = new DefaultKeyGenerator();
	static final DefaultStringFactory DEFAULT_STRING_FACTORY = new DefaultStringFactory();
	static final EntityValidator DEFAULT_VALIDATOR = new DefaultEntityValidator();
	static final Predicate<Entity> DEFAULT_EXISTS = new DefaultEntityExists();

	protected EntityDefinition definition;
	protected Map<Attribute<?>, Object> values;
	protected Map<Attribute<?>, Object> originalValues;

	private String toStringCache;
	private Map<ForeignKey, Key> foreignKeyCache;
	private Key primaryKey;

	protected DefaultEntity(EntityDefinition definition) {
		this.definition = requireNonNull(definition);
	}

	/**
	 * Instantiates a new DefaultEntity
	 * @param key the key
	 */
	DefaultEntity(Key key) {
		this(requireNonNull(key).entityDefinition(), createValueMap(key), null);
		if (key.primaryKey()) {
			this.primaryKey = key;
		}
	}

	/**
	 * Instantiates a new DefaultEntity based on the given values.
	 * @param definition the entity definition
	 * @param values the initial values, may be null
	 * @param originalValues the original values, may be null
	 * @throws IllegalArgumentException in case any of the attributes are not part of the entity.
	 */
	DefaultEntity(EntityDefinition definition, Map<Attribute<?>, Object> values, Map<Attribute<?>, Object> originalValues) {
		this(definition);
		this.values = validateValues(definition, values == null ? new HashMap<>() : new HashMap<>(values));
		this.originalValues = validateTypes(definition, originalValues == null ? null : new HashMap<>(originalValues));
		this.definition = definition;
	}

	@Override
	public final EntityType entityType() {
		return definition.entityType();
	}

	@Override
	public final EntityDefinition definition() {
		return definition;
	}

	@Override
	public final Key primaryKey() {
		if (primaryKey == null) {
			primaryKey = createPrimaryKey(false);
		}

		return primaryKey;
	}

	@Override
	public final Key originalPrimaryKey() {
		return createPrimaryKey(true);
	}

	@Override
	public final boolean modified() {
		if (originalValues != null) {
			for (Attribute<?> attribute : originalValues.keySet()) {
				AttributeDefinition<?> attributeDefinition = definition.attributes().definition(attribute);
				if (attributeDefinition instanceof ColumnDefinition) {
					ColumnDefinition<?> columnDefinition = (ColumnDefinition<?>) attributeDefinition;
					if (columnDefinition.insertable() && columnDefinition.updatable()) {
						return true;
					}
				}
				if (attributeDefinition instanceof TransientAttributeDefinition && ((TransientAttributeDefinition<?>) attributeDefinition).modifiesEntity()) {
					return true;
				}
			}
		}

		return false;
	}

	@Override
	public final <T> T get(Attribute<T> attribute) {
		return cached(definition.attributes().definition(attribute));
	}

	@Override
	public final <T> Optional<T> optional(Attribute<T> attribute) {
		return Optional.ofNullable(get(attribute));
	}

	@Override
	public final <T> T original(Attribute<T> attribute) {
		return original(definition.attributes().definition(attribute));
	}

	@Override
	public final boolean isNull(Attribute<?> attribute) {
		return isNull(definition.attributes().definition(attribute));
	}

	@Override
	public final boolean isNotNull(Attribute<?> attribute) {
		return !isNull(attribute);
	}

	@Override
	public final boolean modified(Attribute<?> attribute) {
		definition.attributes().definition(attribute);
		return isModified(attribute);
	}

	@Override
	public final boolean exists() {
		return definition.exists().test(this);
	}

	@Override
	public final Entity entity(ForeignKey foreignKey) {
		Entity entity = (Entity) values.get(requireNonNull(foreignKey));
		if (entity == null) {//possibly not loaded
			Key key = key(foreignKey);
			if (key != null) {
				return new DefaultEntity(key);
			}
		}

		return entity;
	}

	@Override
	public final <T> String string(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
		if (attribute instanceof ForeignKey && values.get(attribute) == null) {
			Key key = key((ForeignKey) attribute);
			if (key != null) {
				return key.toString();
			}
		}

		return attributeDefinition.string(cached(attributeDefinition));
	}

	@Override
	public <T> T put(Attribute<T> attribute, T value) {
		return put(definition.attributes().definition(attribute), value);
	}

	@Override
	public Entity clearPrimaryKey() {
		definition.primaryKey().columns().forEach(this::remove);
		primaryKey = null;

		return this;
	}

	@Override
	public void save(Attribute<?> attribute) {
		removeOriginalValue(requireNonNull(attribute));
	}

	@Override
	public void save() {
		originalValues = null;
	}

	@Override
	public void revert(Attribute<?> attribute) {
		AttributeDefinition<?> attributeDefinition = definition.attributes().definition(attribute);
		if (isModified(attribute)) {
			put((AttributeDefinition<Object>) attributeDefinition, original(attributeDefinition));
		}
	}

	@Override
	public void revert() {
		if (originalValues != null) {
			for (Attribute<?> attribute : new ArrayList<>(originalValues.keySet())) {
				revert(attribute);
			}
		}
	}

	@Override
	public <T> T remove(Attribute<T> attribute) {
		AttributeDefinition<T> attributeDefinition = definition.attributes().definition(attribute);
		T value = null;
		if (values.containsKey(attribute)) {
			value = (T) values.remove(attribute);
			removeOriginalValue(attribute);
			if (attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).primaryKey()) {
				primaryKey = null;
			}
			if (attribute instanceof Column) {
				definition.foreignKeys().definitions((Column<?>) attribute).forEach(foreignKey -> remove(foreignKey.attribute()));
			}
		}

		return value;
	}

	@Override
	public Map<Attribute<?>, Object> set(Entity entity) {
		if (entity == this) {
			return emptyMap();
		}
		if (entity != null && !definition.entityType().equals(entity.entityType())) {
			throw new IllegalArgumentException("Entity of type: " + definition.entityType() + " expected, got: " + entity.entityType());
		}

		return populateValues(entity);
	}

	@Override
	public final Copy copy() {
		return new DefaultCopy(this);
	}

	@Override
	public final Entity immutable() {
		if (!mutable()) {
			return this;
		}

		return new ImmutableEntity(this);
	}

	@Override
	public final boolean mutable() {
		return !(this instanceof ImmutableEntity);
	}

	@Override
	public boolean equalValues(Entity entity) {
		return equalValues(entity, definition.attributes().get());
	}

	@Override
	public final boolean equalValues(Entity entity, Collection<? extends Attribute<?>> attributes) {
		if (!definition.entityType().equals(requireNonNull(entity).entityType())) {
			throw new IllegalArgumentException("Entity of type: " + definition.entityType() + " expected, got: " + entity.entityType());
		}

		return requireNonNull(attributes).stream()
						.allMatch(attribute -> valueEqual(entity, attribute));
	}

	/**
	 * @param obj the object to compare with
	 * @return true if the given object is an Entity and its primary key is equal to this ones
	 */
	@Override
	public final boolean equals(Object obj) {
		return this == obj || obj instanceof Entity && primaryKey().equals(((Entity) obj).primaryKey());
	}

	/**
	 * @param entity the entity to compare with
	 * @return the compare result from comparing {@code entity} with this Entity instance
	 * @see EntityDefinition.Builder#comparator(java.util.Comparator)
	 */
	@Override
	public final int compareTo(Entity entity) {
		return definition.comparator().compare(this, entity);
	}

	/**
	 * Returns the hash code of the primary key
	 */
	@Override
	public final int hashCode() {
		return primaryKey().hashCode();
	}

	/**
	 * <p>Returns a String representation of this entity.
	 * <p>Note that if the this entitys {@link StringFactory} returns null for some reason, the default String factory is used instead.
	 * <p>The result of this method call is cached by default.
	 * @return a string representation of this entity
	 * @see EntityDefinition.Builder#stringFactory(java.util.function.Function)
	 * @see EntityDefinition.Builder#cacheToString(boolean)
	 * @see EntityDefinition#stringFactory()
	 * @see EntityDefinition#cacheToString()
	 */
	@Override
	public final String toString() {
		if (definition.cacheToString()) {
			if (toStringCache == null) {
				toStringCache = createToString();
			}

			return toStringCache;
		}

		return createToString();
	}

	@Override
	public final Key key(ForeignKey foreignKey) {
		definition.foreignKeys().definition(foreignKey);
		Key cachedReferencedKey = cachedKey(foreignKey);
		if (cachedReferencedKey != null) {
			return cachedReferencedKey;
		}

		return createAndCacheReferencedKey(foreignKey);
	}

	@Override
	public final boolean contains(Attribute<?> attribute) {
		return values.containsKey(requireNonNull(attribute));
	}

	@Override
	public final Set<Map.Entry<Attribute<?>, Object>> entrySet() {
		return unmodifiableSet(values.entrySet());
	}

	@Override
	public final Set<Map.Entry<Attribute<?>, Object>> originalEntrySet() {
		if (originalValues == null) {
			return emptySet();
		}

		return unmodifiableSet(originalValues.entrySet());
	}

	private String createToString() {
		String string = definition.stringFactory().apply(this);
		if (string == null) {
			return DEFAULT_STRING_FACTORY.apply(this);
		}

		return string;
	}

	private void clear() {
		values.clear();
		originalValues = null;
		primaryKey = null;
		foreignKeyCache = null;
		toStringCache = null;
	}

	private Map<Attribute<?>, Object> populateValues(Entity entity) {
		Map<Attribute<?>, Object> previousValues = currentValues();
		clear();
		if (entity != null) {
			entity.entrySet().forEach(entry -> values.put(entry.getKey(), entry.getValue()));
			Set<Map.Entry<Attribute<?>, Object>> originalEntrySet = entity.originalEntrySet();
			if (!originalEntrySet.isEmpty()) {
				originalValues = new HashMap<>();
				originalEntrySet.forEach(entry -> originalValues.put(entry.getKey(), entry.getValue()));
			}
		}
		previousValues.entrySet().removeIf(new Unmodified());

		return unmodifiableMap(previousValues);
	}

	private Map<Attribute<?>, Object> currentValues() {
		return definition.attributes().definitions().stream()
						.collect(HashMap::new, (map, attributeDefinition) ->
										map.put(attributeDefinition.attribute(), get(attributeDefinition)), HashMap::putAll);
	}

	private <T> T get(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition.derived()) {
			return derived((DerivedAttributeDefinition<T>) attributeDefinition);
		}

		return (T) values.get(attributeDefinition.attribute());
	}

	private <T> T cached(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition instanceof DerivedAttributeDefinition<T>) {
			DerivedAttributeDefinition<T> derivedDefinition = (DerivedAttributeDefinition<T>) attributeDefinition;
			if (derivedDefinition.cached()) {
				return derivedCached(derivedDefinition);
			}

			return derived(derivedDefinition);
		}

		return (T) values.get(attributeDefinition.attribute());
	}

	private <T> T original(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition.derived()) {
			return derivedOriginal((DerivedAttributeDefinition<T>) attributeDefinition);
		}
		if (isModified(attributeDefinition.attribute())) {
			return (T) originalValues.get(attributeDefinition.attribute());
		}

		return get(attributeDefinition);
	}

	private <T> T put(AttributeDefinition<T> attributeDefinition, T value) {
		T newValue = validateAndAdjustValue(attributeDefinition, value);
		Attribute<T> attribute = attributeDefinition.attribute();
		boolean initialization = !values.containsKey(attribute);
		T previousValue = (T) values.put(attribute, newValue);
		if (!initialization && Objects.equals(previousValue, newValue)) {
			return newValue;
		}
		if (!initialization) {
			updateOriginalValue(attribute, newValue, previousValue);
		}
		if (attributeDefinition instanceof ColumnDefinition) {
			updateRelatedKeys((ColumnDefinition<T>) attributeDefinition, newValue);
		}
		if (attributeDefinition instanceof ForeignKeyDefinition) {
			updateReferencedColumns((ForeignKeyDefinition) attributeDefinition, (Entity) newValue);
		}
		clearDerivedCache(attribute);
		toStringCache = null;

		return previousValue;
	}

	private void clearDerivedCache(Attribute<? extends Object> sourceAttribute) {
		Collection<Attribute<?>> derivedFrom = definition.attributes().derivedFrom(sourceAttribute);
		if (!derivedFrom.isEmpty()) {
			derivedFrom.forEach(values::remove);
			derivedFrom.forEach(this::clearDerivedCache);
		}
	}

	private <T> boolean isNull(AttributeDefinition<T> attributeDefinition) {
		if (attributeDefinition instanceof ForeignKeyDefinition) {
			return isReferenceNull(((ForeignKeyDefinition) attributeDefinition).attribute());
		}

		return cached(attributeDefinition) == null;
	}

	private boolean isReferenceNull(ForeignKey foreignKey) {
		List<ForeignKey.Reference<?>> references = foreignKey.references();
		if (references.size() == 1) {
			return isNull(references.get(0).column());
		}
		EntityDefinition referencedEntity = definition.foreignKeys().referencedBy(foreignKey);
		for (int i = 0; i < references.size(); i++) {
			ForeignKey.Reference<?> reference = references.get(i);
			ColumnDefinition<?> referencedColumn = referencedEntity.columns().definition(reference.foreign());
			if (!referencedColumn.nullable() && isNull(reference.column())) {
				return true;
			}
		}

		return false;
	}

	private <T> T validateAndAdjustValue(AttributeDefinition<T> attributeDefinition, T value) {
		if (attributeDefinition.derived()) {
			throw new IllegalArgumentException("Can not set the value of a derived attribute");
		}
		if (value == null) {
			return null;
		}
		if (!attributeDefinition.validItem(value)) {
			throw new IllegalArgumentException("Invalid item value: " + value + " for attribute " + attributeDefinition.attribute());
		}
		attributeDefinition.attribute().type().validateType(value);
		if (attributeDefinition instanceof ForeignKeyDefinition) {
			return (T) validateForeignKeyValue((ForeignKeyDefinition) attributeDefinition, (Entity) value);
		}

		return adjustDecimalFractionDigits(attributeDefinition, value);
	}

	private Entity validateForeignKeyValue(ForeignKeyDefinition foreignKeyDefinition, Entity foreignKeyValue) {
		EntityType referencedType = foreignKeyDefinition.attribute().referencedType();
		if (!Objects.equals(referencedType, foreignKeyValue.entityType())) {
			throw new IllegalArgumentException("Entity of type " + referencedType +
							" expected for foreign key " + foreignKeyDefinition + ", got: " + foreignKeyValue.entityType());
		}
		for (ForeignKey.Reference<?> reference : foreignKeyDefinition.references()) {
			throwIfModifiesReadOnlyReference(foreignKeyDefinition, foreignKeyValue, reference);
		}

		return foreignKeyValue;
	}

	private void throwIfModifiesReadOnlyReference(ForeignKeyDefinition foreignKeyDefinition, Entity foreignKeyValue,
																								ForeignKey.Reference<?> reference) {
		boolean readOnlyReference = foreignKeyDefinition.readOnly(reference.column());
		if (readOnlyReference) {
			boolean containsValue = contains(reference.column());
			if (containsValue) {
				Object currentReferenceValue = get(reference.column());
				Object newReferenceValue = foreignKeyValue.get(reference.foreign());
				if (!Objects.equals(currentReferenceValue, newReferenceValue)) {
					throw new IllegalArgumentException("Foreign key " + foreignKeyDefinition + " is not allowed to modify read-only reference: " +
									reference.column() + " from " + currentReferenceValue + " to " + newReferenceValue);
				}
			}
		}
	}

	private <T> void updateRelatedKeys(ColumnDefinition<T> columnDefinition, T newValue) {
		if (columnDefinition.primaryKey()) {
			primaryKey = null;
		}
		if (definition.foreignKeys().foreignKeyColumn(columnDefinition.attribute())) {
			removeInvalidForeignKeyValues(columnDefinition.attribute(), newValue);
		}
	}

	private <T> void removeInvalidForeignKeyValues(Column<T> column, T value) {
		for (ForeignKeyDefinition foreignKeyDefinition : definition.foreignKeys().definitions(column)) {
			Entity foreignKeyEntity = get(foreignKeyDefinition);
			if (foreignKeyEntity != null) {
				ForeignKey foreignKey = foreignKeyDefinition.attribute();
				//if the value isn't equal to the value in the foreign key,
				//that foreign key reference is invalid and is removed
				if (!Objects.equals(value, foreignKeyEntity.get(foreignKey.reference(column).foreign()))) {
					remove(foreignKey);
					removeCachedKey(foreignKey);
				}
			}
		}
	}

	/**
	 * Sets the values of the columns used in the reference to the corresponding values found in {@code referencedEntity}.
	 * Example: EntityOne references EntityTwo via entityTwoId, after a call to this method the EntityOne.entityTwoId
	 * attribute has the value of EntityTwos primary key attribute. If {@code referencedEntity} is null then
	 * the corresponding reference values are set to null.
	 * @param foreignKeyDefinition the foreign key definition
	 * @param referencedEntity the referenced entity
	 */
	private void updateReferencedColumns(ForeignKeyDefinition foreignKeyDefinition, Entity referencedEntity) {
		removeCachedKey(foreignKeyDefinition.attribute());
		List<ForeignKey.Reference<?>> references = foreignKeyDefinition.references();
		for (int i = 0; i < references.size(); i++) {
			ForeignKey.Reference<?> reference = references.get(i);
			if (!foreignKeyDefinition.readOnly(reference.column())) {
				AttributeDefinition<Object> columnDefinition = definition.columns().definition((Column<Object>) reference.column());
				put(columnDefinition, referencedEntity == null ? null : referencedEntity.get(reference.foreign()));
			}
		}
	}

	/**
	 * Creates and caches the key referenced by the given foreign key
	 * @param foreignKey the foreign key
	 * @return the referenced key or null if a valid key can not be created (null values for non-nullable columns)
	 */
	private Key createAndCacheReferencedKey(ForeignKey foreignKey) {
		EntityDefinition referencedEntity = definition.foreignKeys().referencedBy(foreignKey);
		List<ForeignKey.Reference<?>> references = foreignKey.references();
		if (references.size() > 1) {
			return createAndCacheCompositeReferenceKey(foreignKey, references, referencedEntity);
		}

		return createAndCacheSingleReferenceKey(foreignKey, references.get(0), referencedEntity);
	}

	private Key createAndCacheCompositeReferenceKey(ForeignKey foreignKey,
																									List<ForeignKey.Reference<?>> references,
																									EntityDefinition referencedEntity) {
		Map<Column<?>, Object> keyValues = new HashMap<>(references.size());
		for (int i = 0; i < references.size(); i++) {
			ForeignKey.Reference<?> reference = references.get(i);
			ColumnDefinition<?> referencedColumn = referencedEntity.columns().definition(reference.foreign());
			Object value = values.get(reference.column());
			if (value == null && !referencedColumn.nullable()) {
				return null;
			}
			keyValues.put(reference.foreign(), value);
		}
		Set<Column<?>> referencedColumns = keyValues.keySet();
		List<Column<?>> primaryKeyColumns = referencedEntity.primaryKey().columns();
		boolean isPrimaryKey = referencedColumns.size() == primaryKeyColumns.size() && referencedColumns.containsAll(primaryKeyColumns);

		return cacheKey(foreignKey, new DefaultKey(referencedEntity, keyValues, isPrimaryKey));
	}

	private Key createAndCacheSingleReferenceKey(ForeignKey foreignKey,
																							 ForeignKey.Reference<?> reference,
																							 EntityDefinition referencedEntityDefinition) {
		Object value = values.get(reference.column());
		if (value == null) {
			return null;
		}

		List<Column<?>> primaryKeyColumns = referencedEntityDefinition.primaryKey().columns();
		boolean isPrimaryKey = primaryKeyColumns.size() == 1 && reference.foreign().equals(primaryKeyColumns.get(0));

		return cacheKey(foreignKey,
						new DefaultKey(definition.foreignKeys().referencedBy(foreignKey),
										reference.foreign(), value, isPrimaryKey));
	}

	private Key cacheKey(ForeignKey foreignKey, Key key) {
		if (foreignKeyCache == null) {
			foreignKeyCache = new HashMap<>();
		}
		foreignKeyCache.put(foreignKey, key);

		return key;
	}

	private Key cachedKey(ForeignKey foreignKey) {
		if (foreignKeyCache == null) {
			return null;
		}

		return foreignKeyCache.get(foreignKey);
	}

	private void removeCachedKey(ForeignKey foreignKey) {
		if (foreignKeyCache != null) {
			foreignKeyCache.remove(foreignKey);
			if (foreignKeyCache.isEmpty()) {
				foreignKeyCache = null;
			}
		}
	}

	/**
	 * Creates a Key for this Entity instance
	 * @param originalValues if true then the original values of the columns involved are used
	 * @return a Key based on the values in this Entity instance
	 */
	private Key createPrimaryKey(boolean originalValues) {
		if (definition.primaryKey().columns().isEmpty()) {
			return createPseudoPrimaryKey(originalValues);
		}
		List<Column<?>> primaryKeyColumns = definition.primaryKey().columns();
		if (primaryKeyColumns.size() == 1) {
			return createSingleColumnPrimaryKey(primaryKeyColumns.get(0), originalValues);
		}

		return createMultiColumnPrimaryKey(primaryKeyColumns, originalValues);
	}

	private DefaultKey createPseudoPrimaryKey(boolean originalValues) {
		Map<Column<?>, Object> allColumnValues = new HashMap<>();
		values.keySet().stream()
						.map(attribute -> definition.attributes().definition(attribute))
						.filter(ColumnDefinition.class::isInstance)
						.map(attributeDefinition -> (ColumnDefinition<?>) attributeDefinition)
						.forEach(columnDefinition ->
										allColumnValues.put(columnDefinition.attribute(), originalValues ?
														original(columnDefinition.attribute()) :
														values.get(columnDefinition.attribute())));

		return new DefaultKey(definition, allColumnValues, false);
	}

	private DefaultKey createSingleColumnPrimaryKey(Column<?> column, boolean originalValues) {
		return new DefaultKey(definition, column, originalValues ? original(column) : values.get(column), true);
	}

	private DefaultKey createMultiColumnPrimaryKey(List<Column<?>> primaryKeyColumn, boolean originalValues) {
		Map<Column<?>, Object> keyValues = new HashMap<>(primaryKeyColumn.size());
		for (int i = 0; i < primaryKeyColumn.size(); i++) {
			Column<?> column = primaryKeyColumn.get(i);
			keyValues.put(column, originalValues ? original(column) : values.get(column));
		}

		return new DefaultKey(definition, keyValues, true);
	}

	private <T> T derivedCached(DerivedAttributeDefinition<T> attributeDefinition) {
		if (values.containsKey(attributeDefinition.attribute())) {
			return (T) values.get(attributeDefinition.attribute());
		}
		T derivedValue = derived(attributeDefinition);
		values.put(attributeDefinition.attribute(), derivedValue);

		return derivedValue;
	}

	private <T> T derived(DerivedAttributeDefinition<T> derivedDefinition) {
		return derivedDefinition.valueProvider().get(sourceValues(derivedDefinition, false));
	}

	private <T> T derivedOriginal(DerivedAttributeDefinition<T> derivedDefinition) {
		return derivedDefinition.valueProvider().get(sourceValues(derivedDefinition, true));
	}

	private DerivedAttribute.SourceValues sourceValues(DerivedAttributeDefinition<?> derivedDefinition,
																										 boolean originalValue) {
		List<Attribute<?>> sourceAttributes = derivedDefinition.sourceAttributes();
		if (sourceAttributes.isEmpty()) {
			return new DefaultSourceValues(derivedDefinition.attribute(), emptyMap());
		}
		else if (sourceAttributes.size() == 1) {
			return new DefaultSourceValues(derivedDefinition.attribute(), createSingleAttributeSourceValueMap(sourceAttributes.get(0), originalValue));
		}

		return new DefaultSourceValues(derivedDefinition.attribute(), createMultiAttributeSourceValueMap(sourceAttributes, originalValue));
	}

	private Map<Attribute<?>, Object> createSingleAttributeSourceValueMap(Attribute<?> sourceAttribute, boolean originalValue) {
		return singletonMap(sourceAttribute, originalValue ? original(sourceAttribute) : get(sourceAttribute));
	}

	private Map<Attribute<?>, Object> createMultiAttributeSourceValueMap(List<Attribute<?>> sourceAttributes, boolean originalValue) {
		Map<Attribute<?>, Object> valueMap = new HashMap<>(sourceAttributes.size());
		for (int i = 0; i < sourceAttributes.size(); i++) {
			Attribute<?> sourceAttribute = sourceAttributes.get(i);
			valueMap.put(sourceAttribute, originalValue ? original(sourceAttribute) : get(sourceAttribute));
		}

		return valueMap;
	}

	private <T> void setOriginalValue(Attribute<T> attribute, T originalValue) {
		if (originalValues == null) {
			originalValues = new HashMap<>();
		}
		originalValues.put(attribute, originalValue);
	}

	private <T> void removeOriginalValue(Attribute<T> attribute) {
		if (originalValues != null) {
			originalValues.remove(attribute);
			if (originalValues.isEmpty()) {
				originalValues = null;
			}
		}
	}

	private <T> void updateOriginalValue(Attribute<T> attribute, T value, T previousValue) {
		boolean modified = isModified(attribute);
		if (modified && Objects.equals(originalValues.get(attribute), value)) {
			removeOriginalValue(attribute);//we're back to the original value
		}
		else if (!modified) {//only the first original value is kept
			setOriginalValue(attribute, previousValue);
		}
	}

	private boolean valueEqual(Entity entity, Attribute<?> attribute) {
		if (contains(attribute) != entity.contains(attribute)) {
			return false;
		}
		Object value = get(attribute);
		Object other = entity.get(attribute);
		if (attribute.type().isByteArray()) {
			return Arrays.equals((byte[]) value, (byte[]) other);
		}

		return Objects.equals(value, other);
	}

	private boolean isModified(Attribute<?> attribute) {
		return originalValues != null && originalValues.containsKey(attribute);
	}

	@Serial
	private void writeObject(ObjectOutputStream stream) throws IOException {
		stream.writeObject(definition.entityType().domainType().name());
		EntitySerializer.serialize(this, stream);
	}

	@Serial
	private void readObject(ObjectInputStream stream) throws IOException, ClassNotFoundException {
		serializerForDomain((String) stream.readObject()).deserialize(this, stream);
	}

	private static Map<Attribute<?>, Object> validateValues(EntityDefinition definition, Map<Attribute<?>, Object> values) {
		validateTypes(definition, values);

		return values;
	}

	private static Map<Attribute<?>, Object> validateTypes(EntityDefinition definition, Map<Attribute<?>, Object> values) {
		if (values != null && !values.isEmpty()) {
			for (Map.Entry<Attribute<?>, Object> valueEntry : values.entrySet()) {
				definition.attributes().definition((Attribute<Object>) valueEntry.getKey()).attribute().type().validateType(valueEntry.getValue());
			}
		}

		return values;
	}

	private static Map<Attribute<?>, Object> createValueMap(Key key) {
		Collection<Column<?>> columns = key.columns();
		Map<Attribute<?>, Object> values = new HashMap<>(columns.size());
		for (Column<?> column : columns) {
			values.put(column, key.get(column));
		}

		return values;
	}

	private static <T> T adjustDecimalFractionDigits(AttributeDefinition<T> attributeDefinition, T value) {
		if (value instanceof Double) {
			return (T) round((Double) value, attributeDefinition.maximumFractionDigits(),
							attributeDefinition.decimalRoundingMode());
		}
		if (value instanceof BigDecimal) {
			return (T) ((BigDecimal) value).setScale(attributeDefinition.maximumFractionDigits(),
											attributeDefinition.decimalRoundingMode())
							.stripTrailingZeros();
		}

		return value;
	}

	private static Double round(Double value, int places, RoundingMode roundingMode) {
		return value == null ? null : new BigDecimal(Double.toString(value))
						.setScale(places, requireNonNull(roundingMode)).doubleValue();
	}

	private final class Unmodified implements Predicate<Map.Entry<Attribute<?>, Object>> {

		@Override
		public boolean test(Map.Entry<Attribute<?>, Object> entry) {
			return Objects.equals(entry.getValue(), get(definition.attributes().definition(entry.getKey())));
		}
	}

	private static final class DefaultCopy implements Copy {

		private final DefaultEntity entity;

		private DefaultCopy(DefaultEntity entity) {
			this.entity = entity;
		}

		@Override
		public Entity mutable() {
			DefaultEntity copy = new DefaultEntity(entity.definition(), null, null);
			copy.values.putAll(entity.values);
			if (entity.originalValues != null) {
				copy.originalValues = new HashMap<>(entity.originalValues);
			}

			return copy;
		}

		@Override
		public Builder builder() {
			return new DefaultEntityBuilder(entity.definition, entity.values, entity.originalValues);
		}
	}

	private static final class DefaultSourceValues implements DerivedAttribute.SourceValues {

		private final Attribute<?> derivedAttribute;
		private final Map<Attribute<?>, Object> values;

		private DefaultSourceValues(Attribute<?> derivedAttribute, Map<Attribute<?>, Object> values) {
			this.derivedAttribute = derivedAttribute;
			this.values = values;
		}

		@Override
		public <T> T get(Attribute<T> attribute) {
			if (!values.containsKey(attribute)) {
				throw new IllegalArgumentException("Attribute " + attribute +
								" is not specified as a source attribute for derived attribute: " + derivedAttribute);
			}

			return (T) values.get(attribute);
		}

		@Override
		public <T> Optional<T> optional(Attribute<T> attribute) {
			return Optional.ofNullable((T) values.get(attribute));
		}
	}
}
