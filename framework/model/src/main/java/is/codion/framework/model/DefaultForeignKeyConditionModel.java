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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.reactive.value.ValueSet;
import is.codion.common.utilities.Operator;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.condition.Condition;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.Operator.*;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toSet;

final class DefaultForeignKeyConditionModel implements ForeignKeyConditionModel {

	private static final List<Operator> OPERATORS = unmodifiableList(asList(EQUAL, NOT_EQUAL, IN, NOT_IN));

	private final ForeignKey foreignKey;
	private final Entities entities;
	private final ConditionModel<Entity> condition;
	private final DefaultModels models;
	// strong references, the persistence events hold their consumers weakly
	private final Consumer<Map<Entity, Entity>> updateListener = new UpdateListener();
	private final Consumer<Collection<Entity>> deleteListener = new DeleteListener();

	private DefaultForeignKeyConditionModel(DefaultBuilder builder) {
		foreignKey = builder.foreignKey;
		entities = builder.entities;
		models = new DefaultModels(builder.comboBoxModel, builder.searchModel);
		condition = ConditionModel.builder()
						.valueClass(Entity.class)
						// the operator before the operators, which must contain it
						.operator(builder.operator == null ? builder.operators.get(0) : builder.operator)
						.operators(builder.operators)
						.caption(builder.caption)
						.build();
		PersistenceEvents persistenceEvents = persistenceEvents(foreignKey.referencedType());
		persistenceEvents.updated().addWeakConsumer(updateListener);
		persistenceEvents.deleted().addWeakConsumer(deleteListener);
	}

	@Override
	public ForeignKey attribute() {
		return foreignKey;
	}

	@Override
	public ConditionModel<Entity> condition() {
		return condition;
	}

	@Override
	public Observer<?> changed() {
		return condition.changed();
	}

	@Override
	public Optional<String> caption() {
		return condition.caption();
	}

	@Override
	public Models models() {
		return models;
	}

	@Override
	public Link link(ForeignKeyConditionModel master, ForeignKey foreignKey) {
		requireNonNull(foreignKey);
		if (requireNonNull(master) == this) {
			throw new IllegalArgumentException("A condition can not be linked to itself");
		}
		referencedDefinition().foreignKeys().definition(foreignKey);
		if (!foreignKey.referencedType().equals(master.attribute().referencedType())) {
			throw new IllegalArgumentException(foreignKey + " does not reference the entity type of the master condition: " +
							master.attribute().referencedType());
		}

		return models.link(master, foreignKey);
	}

	@Override
	public Link link(ForeignKeyConditionModel master) {
		EntityType masterType = requireNonNull(master).attribute().referencedType();
		Collection<ForeignKey> candidates = referencedDefinition().foreignKeys().get(masterType);
		if (candidates.isEmpty()) {
			throw new IllegalArgumentException(foreignKey.referencedType() + " has no foreign key referencing " + masterType);
		}
		if (candidates.size() > 1) {
			throw new IllegalArgumentException(foreignKey.referencedType() + " has more than one foreign key referencing " + masterType + ": " + candidates);
		}

		return link(master, candidates.iterator().next());
	}

	private EntityDefinition referencedDefinition() {
		return entities.definition(foreignKey.referencedType());
	}

	/**
	 * @param foreignKey the foreign key
	 * @param connection the connection
	 * @return the default combo box model for the given foreign key, of the referenced entities, including null
	 */
	static EntityComboBoxModel comboBoxModel(ForeignKey foreignKey, EntityConnection connection) {
		return EntityComboBoxModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(requireNonNull(connection))
						.includeNull(true)
						.build();
	}

	/**
	 * @param foreignKey the foreign key
	 * @param connection the connection
	 * @return the default search model for the given foreign key, of the referenced entities
	 */
	static EntitySearchModel searchModel(ForeignKey foreignKey, EntityConnection connection) {
		return EntitySearchModel.builder()
						.entityType(requireNonNull(foreignKey).referencedType())
						.connection(requireNonNull(connection))
						.build();
	}

	static final class DefaultBuilder implements Builder {

		static final ForeignKeyStep FOREIGN_KEY_STEP = new DefaultForeignKeyStep();

		private final ForeignKey foreignKey;
		private final Entities entities;

		private Supplier<EntityComboBoxModel> comboBoxModel;
		private Supplier<EntitySearchModel> searchModel;
		private List<Operator> operators = OPERATORS;
		private @Nullable Operator operator;
		private @Nullable String caption;

		private DefaultBuilder(ForeignKey foreignKey, EntityConnection connection) {
			this.foreignKey = foreignKey;
			this.entities = connection.entities();
			this.comboBoxModel = () -> DefaultForeignKeyConditionModel.comboBoxModel(foreignKey, connection);
			this.searchModel = () -> DefaultForeignKeyConditionModel.searchModel(foreignKey, connection);
		}

		@Override
		public Builder comboBoxModel(Supplier<EntityComboBoxModel> comboBoxModel) {
			this.comboBoxModel = requireNonNull(comboBoxModel);
			return this;
		}

		@Override
		public Builder searchModel(Supplier<EntitySearchModel> searchModel) {
			this.searchModel = requireNonNull(searchModel);
			return this;
		}

		@Override
		public Builder operators(List<Operator> operators) {
			if (requireNonNull(operators).isEmpty()) {
				throw new IllegalArgumentException("No operators specified");
			}
			if (!OPERATORS.containsAll(operators)) {
				throw new IllegalArgumentException("Operators not supported by a foreign key condition: " + operators);
			}
			this.operators = unmodifiableList(new ArrayList<>(operators));
			return this;
		}

		@Override
		public Builder operator(Operator operator) {
			this.operator = requireNonNull(operator);
			return this;
		}

		@Override
		public Builder caption(@Nullable String caption) {
			this.caption = caption;
			return this;
		}

		@Override
		public ForeignKeyConditionModel build() {
			if (operator != null && !operators.contains(operator)) {
				throw new IllegalArgumentException("Operator " + operator + " is not one of the available operators: " + operators);
			}

			return new DefaultForeignKeyConditionModel(this);
		}

		private static final class DefaultForeignKeyStep implements ForeignKeyStep {

			@Override
			public ConnectionStep foreignKey(ForeignKey foreignKey) {
				return new DefaultConnectionStep(requireNonNull(foreignKey));
			}
		}

		private static final class DefaultConnectionStep implements ConnectionStep {

			private final ForeignKey foreignKey;

			private DefaultConnectionStep(ForeignKey foreignKey) {
				this.foreignKey = foreignKey;
			}

			@Override
			public Builder connection(EntityConnection connection) {
				return new DefaultBuilder(foreignKey, requireNonNull(connection));
			}
		}
	}

	private final class DefaultModels implements Models {

		private final Supplier<EntityComboBoxModel> comboBoxModel;
		private final Supplier<EntitySearchModel> searchModel;
		private final Value<Supplier<Condition>> condition = Value.nullable();
		private final DefaultOperand equal = new DefaultOperand();
		private final DefaultOperand in = new DefaultOperand();
		private final List<DefaultLink> links = new ArrayList<>();

		private DefaultModels(Supplier<EntityComboBoxModel> comboBoxModel, Supplier<EntitySearchModel> searchModel) {
			this.comboBoxModel = comboBoxModel;
			this.searchModel = searchModel;
		}

		@Override
		public Operand equal() {
			return equal;
		}

		@Override
		public Operand in() {
			return in;
		}

		@Override
		public Value<Supplier<Condition>> condition() {
			return condition;
		}

		private Link link(ForeignKeyConditionModel master, ForeignKey foreignKey) {
			synchronized (this) {
				if (links.stream().anyMatch(link -> link.foreignKey.equals(foreignKey))) {
					throw new IllegalStateException("Already linked on foreign key: " + foreignKey);
				}
				DefaultLink link = new DefaultLink(master, foreignKey);
				links.add(link);
				for (DefaultOperand operand : asList(equal, in)) {
					if (operand.comboBoxModel != null) {
						link.attach(operand.comboBoxModel.filter().get(foreignKey));
					}
					if (operand.searchModel != null) {
						link.attach(operand.searchModel.filter().get(foreignKey));
					}
				}
				link.apply();

				return link;
			}
		}

		private final class DefaultOperand implements Operand {

			private @Nullable EntityComboBoxModel comboBoxModel;
			private @Nullable EntitySearchModel searchModel;

			@Override
			public EntityComboBoxModel comboBoxModel() {
				synchronized (DefaultModels.this) {
					if (comboBoxModel == null) {
						EntityComboBoxModel created = requireNonNull(DefaultModels.this.comboBoxModel.get());
						if (created == other().comboBoxModel) {
							throw new IllegalStateException("The EQUAL and IN operands can not share a combo box model: " + created);
						}
						restrict(created.condition());
						links.forEach(link -> link.attach(created.filter().get(link.foreignKey)));
						comboBoxModel = created;
					}

					return comboBoxModel;
				}
			}

			@Override
			public EntitySearchModel searchModel() {
				synchronized (DefaultModels.this) {
					if (searchModel == null) {
						EntitySearchModel created = requireNonNull(DefaultModels.this.searchModel.get());
						if (created == other().searchModel) {
							throw new IllegalStateException("The EQUAL and IN operands can not share a search model: " + created);
						}
						restrict(created.condition());
						links.forEach(link -> link.attach(created.filter().get(link.foreignKey)));
						searchModel = created;
					}

					return searchModel;
				}
			}

			private DefaultOperand other() {
				return this == equal ? in : equal;
			}

			/**
			 * Applies the shared condition to the given model condition, now if set, and whenever set from now on
			 */
			private void restrict(Value<Supplier<Condition>> modelCondition) {
				condition.optional().ifPresent(modelCondition::set);
				condition.addConsumer(modelCondition::set);
			}
		}

		/**
		 * Derives the keys the master condition refers to on each change to it, applies them to the filters
		 * of the models, created or not, and drops the operands no longer referred to.
		 */
		private final class DefaultLink implements Link {

			private final ForeignKeyConditionModel master;
			private final ForeignKey foreignKey;
			private final State strict = State.state(true);
			private final List<ForeignKeyFilter> filters = new ArrayList<>(4);

			private DefaultLink(ForeignKeyConditionModel master, ForeignKey foreignKey) {
				this.master = master;
				this.foreignKey = foreignKey;
				master.changed().addListener(this::apply);
				// the filters follow, being linked, the operands depend on it
				strict.addListener(this::reconcile);
			}

			@Override
			public State strict() {
				return strict;
			}

			private void attach(ForeignKeyFilter filter) {
				filter.strict().link(strict);
				filters.add(filter);
				set(filter, keys());
			}

			private void apply() {
				Set<Entity.Key> keys = keys();
				filters.forEach(filter -> set(filter, keys));
				reconcile(keys);
			}

			private void reconcile() {
				reconcile(keys());
			}

			private void reconcile(@Nullable Set<Entity.Key> keys) {
				if (keys == null) {
					return;
				}
				Value<Entity> equal = DefaultForeignKeyConditionModel.this.condition.operands().equal();
				Entity equalOperand = equal.get();
				if (equalOperand != null && !accepted(equalOperand, keys)) {
					equal.clear();
				}
				ValueSet<Entity> in = DefaultForeignKeyConditionModel.this.condition.operands().in();
				Set<Entity> inOperands = in.get();
				if (!inOperands.stream().allMatch(entity -> accepted(entity, keys))) {
					in.set(inOperands.stream()
									.filter(entity -> accepted(entity, keys))
									.collect(toCollection(LinkedHashSet::new)));
				}
			}

			private boolean accepted(Entity entity, Set<Entity.Key> keys) {
				Entity.Key key = entity.key(foreignKey);

				return key == null ? !strict.is() : keys.contains(key);
			}

			/**
			 * @return the keys of the entities the master condition refers to, null for none, the models unfiltered
			 */
			private @Nullable Set<Entity.Key> keys() {
				if (!master.enabled().is()) {
					return null;
				}
				switch (master.operator().getOrThrow()) {
					case EQUAL:
						Entity equalOperand = master.operands().equal().get();
						return equalOperand == null ? emptySet() : singleton(equalOperand.primaryKey());
					case IN:
						return master.operands().in().get().stream()
										.map(Entity::primaryKey)
										.collect(toSet());
					default:
						return null;
				}
			}

			private void set(ForeignKeyFilter filter, @Nullable Set<Entity.Key> keys) {
				if (keys == null) {
					filter.clear();
				}
				else {
					filter.set(keys);
				}
			}
		}
	}

	/**
	 * Replaces updated entities in the operands with their updated state, the primary key matched on
	 * its original value, in case the update modified it.
	 */
	private final class UpdateListener implements Consumer<Map<Entity, Entity>> {

		@Override
		public void accept(Map<Entity, Entity> updated) {
			Map<Entity.Key, Entity> updatedByKey = new HashMap<>(updated.size());
			updated.forEach((beforeUpdate, afterUpdate) -> updatedByKey.put(beforeUpdate.originalPrimaryKey(), afterUpdate));
			Value<Entity> equal = condition.operands().equal();
			Entity equalOperand = equal.get();
			if (equalOperand != null && updatedByKey.containsKey(equalOperand.primaryKey())) {
				equal.set(updatedByKey.get(equalOperand.primaryKey()));
			}
			ValueSet<Entity> in = condition.operands().in();
			Set<Entity> inOperands = in.get();
			if (inOperands.stream().anyMatch(entity -> updatedByKey.containsKey(entity.primaryKey()))) {
				// a single set(), a remove followed by an add would leave the operand transiently without the updated entities
				in.set(inOperands.stream()
								.map(entity -> updatedByKey.getOrDefault(entity.primaryKey(), entity))
								.collect(toCollection(LinkedHashSet::new)));
			}
		}
	}

	/**
	 * Removes deleted entities from the operands, only touching an operand containing one,
	 * since setting an operand notifies its listeners regardless of whether it changed.
	 */
	private final class DeleteListener implements Consumer<Collection<Entity>> {

		@Override
		public void accept(Collection<Entity> deleted) {
			Value<Entity> equal = condition.operands().equal();
			Entity equalOperand = equal.get();
			if (equalOperand != null && deleted.contains(equalOperand)) {
				equal.clear();
			}
			ValueSet<Entity> in = condition.operands().in();
			Set<Entity> inOperands = in.get();
			if (deleted.stream().anyMatch(inOperands::contains)) {
				in.removeAll(deleted);
			}
		}
	}
}
