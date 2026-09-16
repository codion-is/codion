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
import is.codion.common.reactive.value.AbstractValue;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.Value.Notify;
import is.codion.common.reactive.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.OrderBy.OrderByColumn;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.text.MessageFormat.format;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class DefaultEntitySearchModel implements EntitySearchModel {

	private static final Logger LOG = LoggerFactory.getLogger(DefaultEntitySearchModel.class);

	private static final Supplier<@Nullable Condition> NULL_CONDITION = () -> null;
	private static final String WILDCARD_MULTIPLE = "%";
	private static final String WILDCARD_SINGLE = "_";

	private final State selectionPresent = State.state();

	private final EntityDefinition entityDefinition;
	private final Collection<Column<String>> columns;
	private final Collection<Attribute<?>> attributes;
	private final @Nullable OrderBy orderBy;
	private final DefaultSearch search = new DefaultSearch();
	private final DefaultSelection selection = new DefaultSelection();
	private final DefaultFilter filter = new DefaultFilter();
	private final EntityConnection connection;
	private final Map<Column<String>, Settings> settings;
	private final Value<Supplier<Condition>> condition;
	private final Value<Integer> limit;

	//we keep references to these listeners, since they will only be referenced via a WeakReference elsewhere
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private DefaultEntitySearchModel(DefaultBuilder builder) {
		this.entityDefinition = builder.entityDefinition;
		this.connection = builder.connection;
		this.columns = unmodifiableList(new ArrayList<>(builder.columns));
		this.condition = Value.builder()
						.nonNull(NULL_CONDITION)
						.value(builder.condition)
						.build();
		this.attributes = builder.attributes;
		this.orderBy = builder.orderBy;
		this.settings = unmodifiableMap(columns.stream()
						.collect(toMap(Function.identity(), column -> new DefaultSettings())));
		this.limit = Value.nullable(builder.limit);
		if (builder.persistenceAware) {
			PersistenceEvents persistenceEvents = persistenceEvents(entityDefinition.type());
			persistenceEvents.updated().addWeakConsumer(updateListener);
			persistenceEvents.deleted().addWeakConsumer(deleteListener);
		}
		builder.filterLinks.forEach((foreignKey, link) -> link.accept(filter.get(foreignKey)));
	}

	@Override
	public EntityDefinition entityDefinition() {
		return entityDefinition;
	}

	@Override
	public EntityConnection connection() {
		return connection;
	}

	@Override
	public Collection<Column<String>> columns() {
		return columns;
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

	@Override
	public Filter filter() {
		return filter;
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
		public List<Entity> perform() {
			if (filter.excludesAll()) {
				return emptyList();
			}
			List<Entity> result = new ArrayList<>(connection.select(select()));
			result.sort(entityDefinition.comparator());

			return unmodifiableList(result);
		}

		private Select select() {
			//not validated at build time, a ForeignKeyConditionModel provides a search model for any
			//foreign key, including those referencing entities without searchable columns
			if (columns.isEmpty()) {
				throw new IllegalStateException("No search columns provided for search model: " + entityDefinition.type());
			}
			Collection<Condition> conditions = new ArrayList<>();
			for (Column<String> column : columns) {
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
			//trim before replacing spaces, otherwise surrounding whitespace becomes wildcards instead of being removed
			String searchString = rawSearchString.trim();
			if (settings.spaceAsWildcard().is()) {
				searchString = searchString.replace(' ', '%');
			}

			return searchString.equals(WILDCARD_MULTIPLE) ? WILDCARD_MULTIPLE :
							((wildcardPrefix ? WILDCARD_MULTIPLE : "") + searchString + (wildcardPostfix ? WILDCARD_MULTIPLE : ""));
		}

		private Condition createCombinedCondition(Collection<Condition> conditions) {
			List<Condition> combined = new ArrayList<>(filter.conditions());
			Supplier<Condition> conditionSupplier = condition.getOrThrow();
			if (conditionSupplier != NULL_CONDITION) {
				combined.add(validate(conditionSupplier.get()));
			}
			combined.add(or(conditions));

			return combined.size() == 1 ? combined.get(0) : and(combined);
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

	private final class DefaultFilter implements Filter {

		private final Map<ForeignKey, DefaultForeignKeyFilter> foreignKeyFilters = new HashMap<>();

		@Override
		public ForeignKeyFilter get(ForeignKey foreignKey) {
			entityDefinition.foreignKeys().definition(foreignKey);

			return foreignKeyFilters.computeIfAbsent(foreignKey, DefaultForeignKeyFilter::new);
		}

		/**
		 * @return true if a strict filter without keys excludes all entities
		 */
		private boolean excludesAll() {
			return foreignKeyFilters.values().stream()
							.anyMatch(DefaultForeignKeyFilter::excludesAll);
		}

		private List<Condition> conditions() {
			return foreignKeyFilters.values().stream()
							.map(DefaultForeignKeyFilter::condition)
							.flatMap(Optional::stream)
							.collect(toList());
		}
	}

	private final class DefaultForeignKeyFilter implements ForeignKeyFilter {

		private final ForeignKey foreignKey;
		private final State strict = State.state(true);

		private @Nullable Set<Entity.Key> keys;

		private DefaultForeignKeyFilter(ForeignKey foreignKey) {
			this.foreignKey = foreignKey;
		}

		@Override
		public void set(Entity.Key key) {
			set(singleton(requireNonNull(key)));
		}

		@Override
		public void set(Collection<Entity.Key> keys) {
			for (Entity.Key key : requireNonNull(keys)) {
				if (!key.type().equals(foreignKey.referencedType())) {
					throw new IllegalArgumentException("Key " + key + " is not of the correct type (" + foreignKey.referencedType() + ")");
				}
			}
			this.keys = unmodifiableSet(new HashSet<>(keys));
		}

		@Override
		public Collection<Entity.Key> get() {
			return keys == null ? emptySet() : keys;
		}

		@Override
		public void clear() {
			keys = null;
		}

		@Override
		public State strict() {
			return strict;
		}

		@Override
		public void link(EntityComboBoxModel filterModel) {
			DefaultEntityComboBoxModel.validateLink(foreignKey, requireNonNull(filterModel).entityDefinition().type());
			link(filterModel.selection().item());
			selection.entity.addConsumer(selected -> select(filterModel, selected));
		}

		@Override
		public void link(EntitySearchModel filterModel) {
			DefaultEntityComboBoxModel.validateLink(foreignKey, requireNonNull(filterModel).entityDefinition().type());
			link(filterModel.selection().entity());
			selection.entity.addConsumer(selected -> select(filterModel, selected));
		}

		private void link(Value<Entity> masterSelection) {
			Entity selected = masterSelection.get();
			//preserve any pre-set filter keys when the master has no selection to sync from
			if (selected != null || get().isEmpty()) {
				set(selected);
			}
			masterSelection.addConsumer(this::set);
		}

		private boolean excludesAll() {
			return keys != null && keys.isEmpty() && strict.is();
		}

		/**
		 * @return the filter condition, empty if cleared or no keys and not strict, the strict case excluding all without a query
		 */
		private Optional<Condition> condition() {
			if (keys == null || keys.isEmpty()) {
				return Optional.empty();
			}
			Condition in = foreignKey.in(keys.stream()
							.map(this::entity)
							.collect(toList()));

			return Optional.of(strict.is() ? in : or(in, foreignKey.isNull()));
		}

		private Entity entity(Entity.Key key) {
			Entity.Builder builder = connection.entities().entity(key.type());
			key.columns().forEach(column -> builder.with((Column<Object>) column, key.get((Column<Object>) column)));

			return builder.build();
		}

		private void set(@Nullable Entity selected) {
			if (selected != null) {
				set(selected.primaryKey());
			}
			else if (strict.is()) {
				set(emptyList());
			}
			else {
				clear();
			}
		}

		private void select(EntityComboBoxModel filterModel, @Nullable Entity selected) {
			if (selected != null && selected.present(foreignKey)) {
				filterModel.select(selected.key(foreignKey));
			}
		}

		private void select(EntitySearchModel filterModel, @Nullable Entity selected) {
			if (selected != null && selected.present(foreignKey)) {
				filterModel.selection().entity().set(selected.entity(foreignKey));
			}
		}
	}

	private final class DefaultSelection implements Selection {

		private final SelectedEntity entity = new SelectedEntity();

		@Override
		public Value<Entity> entity() {
			return entity;
		}

		@Override
		public ObservableState present() {
			return selectionPresent.observable();
		}

		@Override
		public void clear() {
			entity.clear();
		}
	}

	/**
	 * The selected entity, notifying when the instance changes, so that an updated entity replacing an
	 * equal one notifies, while setting the selected instance again, or clearing an empty selection, does not.
	 */
	private final class SelectedEntity extends AbstractValue<Entity> {

		private @Nullable Entity entity;

		private SelectedEntity() {
			addValidator(new EntityValidator());
		}

		@Override
		protected @Nullable Entity getValue() {
			return entity;
		}

		@Override
		protected void setValue(@Nullable Entity entity) {
			Entity previous = this.entity;
			this.entity = entity;
			selectionPresent.set(entity != null);
			if (entity != previous) {
				notifyObserver();
			}
		}
	}

	private final class EntityValidator implements Value.Validator<Entity> {

		@Override
		public void validate(@Nullable Entity entity) {
			if (entity != null && !entity.type().equals(entityDefinition.type())) {
				throw new IllegalArgumentException("Entities of type " + entityDefinition.type() + " expected, got " + entity.type());
			}
		}
	}

	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Entity selected = selection.entity.get();
			if (selected != null) {
				updated.forEach((beforeUpdate, afterUpdate) -> {
					// matched on the original primary key, in case the update modified it
					if (beforeUpdate.originalPrimaryKey().equals(selected.primaryKey())) {
						selection.entity.set(afterUpdate);
						LOG.debug("{} - replaced the updated selected entity", DefaultEntitySearchModel.this);
					}
				});
			}
		}
	}

	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			Entity selected = selection.entity.get();
			if (selected != null && deleted.contains(selected)) {
				selection.entity.clear();
				LOG.debug("{} - cleared the deleted selected entity", DefaultEntitySearchModel.this);
			}
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
		public Builder.ConnectionStep entityType(EntityType entityType) {
			return new DefaultConnectionStep(requireNonNull(entityType));
		}
	}

	private static class DefaultConnectionStep implements Builder.ConnectionStep {

		private final EntityType entityType;

		private DefaultConnectionStep(EntityType entityType) {
			this.entityType = entityType;
		}

		@Override
		public Builder connection(EntityConnection connection) {
			return new DefaultBuilder(this.entityType, connection);
		}
	}

	static final class DefaultBuilder implements Builder {

		static final Builder.EntityTypeStep ENTITY_TYPE = new DefaultEntityTypeStep();

		private final EntityDefinition entityDefinition;
		private final EntityConnection connection;
		private Collection<Column<String>> columns;
		private @Nullable Supplier<Condition> condition;
		private Collection<Attribute<?>> attributes = emptyList();
		private @Nullable Integer limit = DEFAULT_LIMIT.get();
		private boolean persistenceAware = PERSISTENCE_AWARE.getOrThrow();
		private @Nullable OrderBy orderBy;
		private final Map<ForeignKey, Consumer<ForeignKeyFilter>> filterLinks = new LinkedHashMap<>();

		DefaultBuilder(EntityType entityType, EntityConnection connection) {
			this.connection = requireNonNull(connection);
			this.entityDefinition = connection.entities().definition(entityType);
			this.columns = entityDefinition.columns().searchable();
			this.orderBy = entityDefinition.orderBy().orElse(null);
		}

		@Override
		public Builder search(Column<String>... columns) {
			return search(asList(requireNonNull(columns)));
		}

		@Override
		public Builder search(Collection<Column<String>> columns) {
			if (requireNonNull(columns).isEmpty()) {
				throw new IllegalArgumentException("One or more search column is required");
			}
			validateAttributes(columns);
			this.columns = columns;
			return this;
		}

		@Override
		public Builder condition(Supplier<Condition> condition) {
			this.condition = condition;
			return this;
		}

		@Override
		public Builder filter(ForeignKey foreignKey, EntityComboBoxModel filterModel) {
			entityDefinition.foreignKeys().definition(foreignKey);
			DefaultEntityComboBoxModel.validateLink(foreignKey, requireNonNull(filterModel).entityDefinition().type());
			filterLinks.put(foreignKey, filter -> filter.link(filterModel));
			return this;
		}

		@Override
		public Builder filter(ForeignKey foreignKey, EntitySearchModel filterModel) {
			entityDefinition.foreignKeys().definition(foreignKey);
			DefaultEntityComboBoxModel.validateLink(foreignKey, requireNonNull(filterModel).entityDefinition().type());
			filterLinks.put(foreignKey, filter -> filter.link(filterModel));
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
		public Builder persistenceAware(boolean persistenceAware) {
			this.persistenceAware = persistenceAware;
			return this;
		}

		@Override
		public Builder limit(@Nullable Integer limit) {
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
