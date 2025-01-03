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
package is.codion.framework.model;

import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.condition.Condition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableMap;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

final class DefaultEntitySearchModel implements EntitySearchModel {

	private static final Supplier<Condition> NULL_CONDITION = () -> null;
	private static final Function<Entity, String> DEFAULT_TO_STRING = Object::toString;
	private static final String DEFAULT_SEPARATOR = ",";
	private static final String WILDCARD_MULTIPLE = "%";
	private static final String WILDCARD_SINGLE = "_";

	private final State searchStringModified = State.state();
	private final State selectionEmpty = State.state(true);

	private final EntityType entityType;
	private final Collection<Column<String>> columns;
	private final DefaultSelection selection = new DefaultSelection();
	private final EntityConnectionProvider connectionProvider;
	private final Map<Column<String>, Settings> settings;
	private final Value<String> searchString = Value.builder()
					.nonNull("")
					.notify(Notify.WHEN_SET)
					.listener(() -> searchStringModified.set(!selection.searchStringRepresentsSelection()))
					.build();
	private final Value<String> separator = Value.builder()
					.nonNull(DEFAULT_SEPARATOR)
					.listener(this::reset)
					.build();
	private final boolean singleSelection;
	private final Value<Supplier<Condition>> condition = Value.nonNull(NULL_CONDITION);
	private final Value<Function<Entity, String>> stringFunction = Value.nonNull(DEFAULT_TO_STRING);
	private final Value<Integer> limit;
	private final String description;

	private DefaultEntitySearchModel(DefaultBuilder builder) {
		this.entityType = builder.entityType;
		this.connectionProvider = builder.connectionProvider;
		this.separator.set(builder.separator);
		this.columns = unmodifiableCollection(builder.columns);
		this.settings = unmodifiableMap(columns.stream()
						.collect(toMap(Function.identity(), column -> new DefaultSettings())));
		this.stringFunction.set(builder.stringFunction);
		this.description = builder.description == null ? createDescription() : builder.description;
		this.singleSelection = builder.singleSelection;
		this.limit = Value.nullable(builder.limit);
	}

	@Override
	public EntityType entityType() {
		return entityType;
	}

	@Override
	public EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Override
	public Collection<Column<String>> columns() {
		return columns;
	}

	@Override
	public String description() {
		return description;
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
	public Value<Function<Entity, String>> stringFunction() {
		return stringFunction;
	}

	@Override
	public void reset() {
		searchString.set(selection.entitiesToString());
	}

	@Override
	public List<Entity> search() {
		List<Entity> result = connectionProvider.connection().select(select());
		result.sort(connectionProvider.entities().definition(entityType).comparator());

		return result;
	}

	@Override
	public Value<String> searchString() {
		return searchString;
	}

	@Override
	public Value<String> separator() {
		return separator;
	}

	@Override
	public boolean singleSelection() {
		return singleSelection;
	}

	@Override
	public ObservableState searchStringModified() {
		return searchStringModified.observable();
	}

	/**
	 * @return a select instance based on this search model including any additional search condition
	 * @throws IllegalStateException in case no search columns are specified
	 */
	private Select select() {
		if (columns.isEmpty()) {
			throw new IllegalStateException("No search columns provided for search model: " + entityType);
		}
		Collection<Condition> conditions = new ArrayList<>();
		String[] searchStrings = singleSelection ? new String[] {searchString.get()} : searchString.getOrThrow().split(separator.getOrThrow());
		for (Column<String> column : columns) {
			Settings columnSettings = settings.get(column);
			for (String rawSearchString : searchStrings) {
				String preparedSearchString = prepareSearchString(rawSearchString, columnSettings);
				boolean containsWildcards = containsWildcards(preparedSearchString);
				if (columnSettings.caseSensitive().get()) {
					conditions.add(containsWildcards ? column.like(preparedSearchString) : column.equalTo(preparedSearchString));
				}
				else {
					conditions.add(containsWildcards ? column.likeIgnoreCase(preparedSearchString) : column.equalToIgnoreCase(preparedSearchString));
				}
			}
		}

		return Select.where(createCombinedCondition(conditions))
						.limit(limit.get())
						.build();
	}

	private Condition createCombinedCondition(Collection<Condition> conditions) {
		Condition conditionCombination = or(conditions);
		Condition additionalCondition = condition.getOrThrow().get();

		return additionalCondition == null ? conditionCombination : and(additionalCondition, conditionCombination);
	}

	private static String prepareSearchString(String rawSearchString, Settings settings) {
		boolean wildcardPrefix = settings.wildcardPrefix().get();
		boolean wildcardPostfix = settings.wildcardPostfix().get();
		rawSearchString = settings.spaceAsWildcard().get() ? rawSearchString.replace(' ', '%') : rawSearchString;

		return rawSearchString.equals(WILDCARD_MULTIPLE) ? WILDCARD_MULTIPLE :
						((wildcardPrefix ? WILDCARD_MULTIPLE : "") + rawSearchString.trim() + (wildcardPostfix ? WILDCARD_MULTIPLE : ""));
	}

	private String createDescription() {
		EntityDefinition definition = connectionProvider.entities().definition(entityType);

		return columns.stream()
						.map(column -> definition.columns().definition(column).caption())
						.collect(joining(", "));
	}

	private void validateType(Entity entity) {
		if (!entity.entityType().equals(entityType)) {
			throw new IllegalArgumentException("Entities of type " + entityType + " exptected, got " + entity.entityType());
		}
	}

	private final class DefaultSelection implements Selection {

		private final ValueSet<Entity> entities = ValueSet.<Entity>builder()
						.notify(Notify.WHEN_SET)
						.validator(new EntityValidator())
						.listener(DefaultEntitySearchModel.this::reset)
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

		private boolean searchStringRepresentsSelection() {
			return (entities.get().isEmpty() && searchString.getOrThrow().isEmpty()) ||
							(!entities.get().isEmpty() && entitiesToString().equals(searchString.get()));
		}

		private String entitiesToString() {
			return entities.get().stream()
							.map(stringFunction.getOrThrow())
							.collect(joining(separator.getOrThrow()));
		}
	}

	private final class EntityValidator implements Value.Validator<Set<Entity>> {

		@Override
		public void validate(Set<Entity> entitySet) {
			if (entitySet != null) {
				if (entitySet.size() > 1 && singleSelection) {
					throw new IllegalArgumentException("This EntitySearchModel does not allow the selection of multiple entities");
				}
				entitySet.forEach(DefaultEntitySearchModel.this::validateType);
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

	static final class DefaultBuilder implements Builder {

		private final EntityType entityType;
		private final EntityConnectionProvider connectionProvider;
		private Collection<Column<String>> columns;
		private Function<Entity, String> stringFunction = DEFAULT_TO_STRING;
		private String description;
		private boolean singleSelection = false;
		private String separator = DEFAULT_SEPARATOR;
		private Integer limit = DEFAULT_LIMIT.get();

		DefaultBuilder(EntityType entityType, EntityConnectionProvider connectionProvider) {
			this.entityType = requireNonNull(entityType);
			this.connectionProvider = requireNonNull(connectionProvider);
			this.columns = connectionProvider.entities().definition(entityType).columns().searchable();
		}

		@Override
		public Builder columns(Collection<Column<String>> columns) {
			if (requireNonNull(columns).isEmpty()) {
				throw new IllegalArgumentException("One or more search column is required");
			}
			validateColumns(columns);
			this.columns = columns;
			return this;
		}

		@Override
		public Builder stringFunction(Function<Entity, String> stringFunction) {
			this.stringFunction = requireNonNull(stringFunction);
			return this;
		}

		@Override
		public Builder description(String description) {
			this.description = requireNonNull(description);
			return this;
		}

		@Override
		public Builder singleSelection(boolean singleSelection) {
			this.singleSelection = singleSelection;
			return this;
		}

		@Override
		public Builder separator(String separator) {
			this.separator = requireNonNull(separator);
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

		private void validateColumns(Collection<Column<String>> columns) {
			for (Column<String> column : columns) {
				if (!entityType.equals(column.entityType())) {
					throw new IllegalArgumentException("Column '" + column + "' is not part of entity " + entityType);
				}
			}
		}
	}
}
