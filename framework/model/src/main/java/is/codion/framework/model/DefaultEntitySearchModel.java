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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;
import is.codion.common.reactive.value.ValueSet;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.OrderBy.OrderByColumn;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;
import static is.codion.framework.model.EntityEditModel.events;
import static java.text.MessageFormat.format;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultEntitySearchModel implements EntitySearchModel {

	private static final Supplier<@Nullable Condition> NULL_CONDITION = () -> null;
	private static final String WILDCARD_MULTIPLE = "%";
	private static final String WILDCARD_SINGLE = "_";

	private final State selectionEmpty = State.state(true);

	private final EntityDefinition entityDefinition;
	private final Collection<Column<String>> searchColumns;
	private final Collection<Attribute<?>> attributes;
	private final @Nullable OrderBy orderBy;
	private final DefaultSearch search = new DefaultSearch();
	private final DefaultSelection selection = new DefaultSelection();
	private final EntityConnectionProvider connectionProvider;
	private final Map<Column<String>, Settings> settings;
	private final Value<Supplier<Condition>> condition;
	private final Value<Integer> limit;

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private DefaultEntitySearchModel(DefaultBuilder builder) {
		this.entityDefinition = builder.entityDefinition;
		this.connectionProvider = builder.connectionProvider;
		this.searchColumns = unmodifiableCollection(builder.searchColumns);
		this.condition = Value.builder()
						.nonNull(NULL_CONDITION)
						.value(builder.condition)
						.build();
		this.attributes = builder.attributes;
		this.orderBy = builder.orderBy;
		this.settings = unmodifiableMap(searchColumns.stream()
						.collect(toMap(Function.identity(), column -> new DefaultSettings())));
		this.limit = Value.nullable(builder.limit);
		if (builder.editEvents) {
			events(entityDefinition.type()).updated().addWeakConsumer(updateListener);
			events(entityDefinition.type()).deleted().addWeakConsumer(deleteListener);
		}
	}

	@Override
	public EntityDefinition entityDefinition() {
		return entityDefinition;
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public Collection<Column<String>> columns() {
		return searchColumns;
	}

	@Override
	public Search search() {
		return search;
	}

	@Override
	public Selection selection() {
		return selection;
	}

	@Override
	public Map<Column<String>, Settings> settings() {
		return settings;
	}

	@Override
	public Value<Integer> limit() {
		return limit;
	}

	@Override
	public Value<Supplier<Condition>> condition() {
		return condition;
	}

	private void validateType(Entity entity) {
		if (!entity.type().equals(entityDefinition.type())) {
			throw new IllegalArgumentException("Entities of type " + entityDefinition.type() + " exptected, got " + entity.type());
		}
	}

	private final class DefaultSearch implements Search {

		private final ValueSet<String> strings = ValueSet.<String>builder()
						.notify(Notify.SET)
						.build();

		@Override
		public ValueSet<String> strings() {
			return strings;
		}

		@Override
		public List<Entity> result() {
			List<Entity> result = connectionProvider.connection().select(select());
			result.sort(entityDefinition.comparator());

			return result;
		}

		private Select select() {
			if (searchColumns.isEmpty()) {
				throw new IllegalStateException("No search columns provided for search model: " + entityDefinition.type());
			}
			Collection<Condition> conditions = new ArrayList<>();
			for (Column<String> column : searchColumns) {
				Settings columnSettings = settings.get(column);
				for (String rawSearchString : strings.get()) {
					String preparedSearchString = prepareSearchString(rawSearchString, columnSettings);
					boolean containsWildcards = containsWildcards(preparedSearchString);
					if (columnSettings.caseSensitive().is()) {
						conditions.add(containsWildcards ? column.like(preparedSearchString) : column.equalTo(preparedSearchString));
					}
					else {
						conditions.add(containsWildcards ? column.likeIgnoreCase(preparedSearchString) : column.equalToIgnoreCase(preparedSearchString));
					}
				}
			}

			return Select.where(createCombinedCondition(conditions))
							.attributes(attributes)
							.limit(limit.get())
							.orderBy(orderBy)
							.build();
		}

		private String prepareSearchString(String rawSearchString, Settings settings) {
			boolean wildcardPrefix = settings.wildcardPrefix().is();
			boolean wildcardPostfix = settings.wildcardPostfix().is();
			rawSearchString = settings.spaceAsWildcard().is() ? rawSearchString.replace(' ', '%') : rawSearchString;

			return rawSearchString.equals(WILDCARD_MULTIPLE) ? WILDCARD_MULTIPLE :
							((wildcardPrefix ? WILDCARD_MULTIPLE : "") + rawSearchString.trim() + (wildcardPostfix ? WILDCARD_MULTIPLE : ""));
		}

		private Condition createCombinedCondition(Collection<Condition> conditions) {
			Condition conditionCombination = or(conditions);
			Supplier<Condition> conditionSupplier = condition.getOrThrow();
			if (conditionSupplier == NULL_CONDITION) {
				return conditionCombination;
			}

			return and(validate(conditionSupplier.get()), conditionCombination);
		}

		private Condition validate(Condition queryCondition) {
			if (queryCondition == null) {
				throw new IllegalArgumentException(format("EntitySearchModel condition supplier returned null: {0}", entityDefinition.type()));
			}
			if (!queryCondition.entityType().equals(entityDefinition.type())) {
				throw new IllegalArgumentException(format("EntitySearchModel condition supplier returned a condition for the incorrect type {0}, expecting: {1}",
								queryCondition.entityType(), entityDefinition.type()));
			}

			return queryCondition;
		}
	}

	private final class DefaultSelection implements Selection {

		private final ValueSet<Entity> entities = ValueSet.<Entity>builder()
						.notify(Notify.SET)
						.validator(new EntityValidator())
						.consumer(selectedEntities -> selectionEmpty.set(selectedEntities.isEmpty()))
						.build();

		@Override
		public Value<Entity> entity() {
			return entities.value();
		}

		@Override
		public ValueSet<Entity> entities() {
			return entities;
		}

		@Override
		public ObservableState empty() {
			return selectionEmpty.observable();
		}

		@Override
		public void clear() {
			entities.clear();
		}
	}

	private final class EntityValidator implements Value.Validator<Set<Entity>> {

		@Override
		public void validate(Set<Entity> entitySet) {
			entitySet.forEach(DefaultEntitySearchModel.this::validateType);
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Set<Entity> toRemove = new HashSet<>();
			Set<Entity> toAdd = new HashSet<>();
			updated.keySet().forEach(beforeUpdate -> {
				Entity entity = beforeUpdate.copy().builder().originalPrimaryKey().build();
				if (selection.entities.contains(entity)) {
					toRemove.add(entity);
					toAdd.add(updated.get(beforeUpdate));
				}
			});
			if (!toRemove.isEmpty()) {
				selection.entities.removeAll(toRemove);
				selection.entities.addAll(toAdd);
			}
		}
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			selection.entities.removeAll(deleted);
		}
	}

	private static boolean containsWildcards(String value) {
		return value.contains(WILDCARD_MULTIPLE) || value.contains(WILDCARD_SINGLE);
	}

	private static final class DefaultSettings implements Settings {

		private final State wildcardPrefixState = State.state(true);
		private final State wildcardPostfixState = State.state(true);
		private final State caseSensitiveState = State.state(false);
		private final State spaceAsWildcard = State.state(true);

		@Override
		public State wildcardPrefix() {
			return wildcardPrefixState;
		}

		@Override
		public State wildcardPostfix() {
			return wildcardPostfixState;
		}

		@Override
		public State spaceAsWildcard() {
			return spaceAsWildcard;
		}

		@Override
		public State caseSensitive() {
			return caseSensitiveState;
		}
	}

	static class DefaultEntityTypeStep implements Builder.EntityTypeStep {

		@Override
		public Builder.ConnectionProviderStep entityType(EntityType entityType) {
			return new DefaultConnectionProviderStep(requireNonNull(entityType));
		}
	}

	private static class DefaultConnectionProviderStep implements Builder.ConnectionProviderStep {

		private final EntityType entityType;

		private DefaultConnectionProviderStep(EntityType entityType) {
			this.entityType = entityType;
		}

		@Override
		public Builder connectionProvider(EntityConnectionProvider connectionProvider) {
			return new DefaultBuilder(this.entityType, connectionProvider);
		}
	}

	static final class DefaultBuilder implements Builder {

		static final Builder.EntityTypeStep ENTITY_TYPE = new DefaultEntityTypeStep();

		private final EntityDefinition entityDefinition;
		private final EntityConnectionProvider connectionProvider;
		private Collection<Column<String>> searchColumns;
		private @Nullable Supplier<Condition> condition;
		private Collection<Attribute<?>> attributes = emptyList();
		private @Nullable Integer limit = DEFAULT_LIMIT.get();
		private boolean editEvents = EDIT_EVENTS.getOrThrow();
		private @Nullable OrderBy orderBy;

		DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this.connectionProvider = requireNonNull(connectionProvider);
			this.entityDefinition = connectionProvider.entities().definition(entityType);
			this.searchColumns = entityDefinition.columns().searchable();
			this.orderBy = entityDefinition.orderBy().orElse(null);
		}

		@Override
		public Builder searchColumns(Collection<Column<String>> searchColumns) {
			if (requireNonNull(searchColumns).isEmpty()) {
				throw new IllegalArgumentException("One or more search column is required");
			}
			validateAttributes(searchColumns);
			this.searchColumns = searchColumns;
			return this;
		}

		@Override
		public Builder condition(Supplier<Condition> condition) {
			this.condition = condition;
			return this;
		}

		@Override
		public Builder attributes(Collection<Attribute<?>> attributes) {
			validateAttributes(requireNonNull(attributes));
			this.attributes = attributes;
			return this;
		}

		@Override
		public Builder orderBy(OrderBy orderBy) {
			validateAttributes(requireNonNull(orderBy).orderByColumns().stream()
							.map(OrderByColumn::column)
							.collect(toList()));
			this.orderBy = orderBy;
			return this;
		}

		@Override
		public Builder editEvents(boolean editEvents) {
			this.editEvents = editEvents;
			return this;
		}

		@Override
		public Builder limit(int limit) {
			this.limit = limit;
			return this;
		}

		@Override
		public EntitySearchModel build() {
			return new DefaultEntitySearchModel(this);
		}

		private void validateAttributes(Collection<? extends Attribute<?>> attributes) {
			for (Attribute<?> attribute : attributes) {
				if (!entityDefinition.type().equals(attribute.entityType())) {
					throw new IllegalArgumentException("Attribute '" + attribute + "' is not part of entity " + entityDefinition.type());
				}
			}
		}
	}
}
