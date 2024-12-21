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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.value.ObservableValueSet;
import is.codion.common.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * A default EntityModel implementation.
 * @param <M> the type of {@link DefaultEntityModel} used for detail models
 * @param <E> the type of {@link AbstractEntityEditModel} used by this {@link EntityModel}
 * @param <T> the type of {@link EntityTableModel} used by this {@link EntityModel}
 */
public class DefaultEntityModel<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
				T extends EntityTableModel<E>> implements EntityModel<M, E, T> {

	private final E editModel;
	private final T tableModel;
	private final DefaultDetailModels<M, E, T> detailModels = new DefaultDetailModels<>();

	/**
	 * Instantiates a new DefaultEntityModel, without a table model
	 * @param editModel the edit model
	 */
	public DefaultEntityModel(E editModel) {
		this.editModel = requireNonNull(editModel);
		this.tableModel = null;
		bindEventsInternal();
	}

	/**
	 * Instantiates a new DefaultEntityModel
	 * @param tableModel the table model
	 */
	public DefaultEntityModel(T tableModel) {
		this.editModel = requireNonNull(tableModel).editModel();
		this.tableModel = tableModel;
		bindEventsInternal();
	}

	/**
	 * @return a String representation of this EntityModel,
	 * returns the model class name by default
	 */
	@Override
	public final String toString() {
		return getClass().getSimpleName() + ": " + entityType();
	}

	@Override
	public final EntityType entityType() {
		return editModel.entityType();
	}

	@Override
	public final EntityConnectionProvider connectionProvider() {
		return editModel.connectionProvider();
	}

	@Override
	public final EntityConnection connection() {
		return editModel.connection();
	}

	@Override
	public final Entities entities() {
		return editModel.connectionProvider().entities();
	}

	@Override
	public final EntityDefinition entityDefinition() {
		return editModel.entityDefinition();
	}

	@Override
	public final <C extends E> C editModel() {
		return (C) editModel;
	}

	@Override
	public final <C extends T> C tableModel() {
		if (tableModel == null) {
			throw new IllegalStateException("Entity model " + this + " does not contain a table model");
		}

		return (C) tableModel;
	}

	@Override
	public final boolean containsTableModel() {
		return tableModel != null;
	}

	@Override
	public final DetailModels<M, E, T> detailModels() {
		return detailModels;
	}

	private void onMasterSelectionChanged() {
		if (!detailModels.linked.empty()) {
			List<Entity> activeEntities = activeEntities();
			for (M detailModel : detailModels.linked) {
				detailModels.models.get(detailModel).onSelection(activeEntities);
			}
		}
	}

	private List<Entity> activeEntities() {
		if (tableModel != null && tableModel.selection().empty().not().get()) {
			return tableModel.selection().items().get();
		}
		else if (editModel.editor().exists().not().get()) {
			return emptyList();
		}

		return singletonList(editModel.editor().get());
	}

	private void bindEventsInternal() {
		editModel.afterInsert().addConsumer(this::onInsert);
		editModel.afterUpdate().addConsumer(this::onUpdate);
		editModel.afterDelete().addConsumer(this::onDelete);
		if (containsTableModel()) {
			tableModel.selection().indexes().addListener(this::onMasterSelectionChanged);
		}
		else {
			editModel.editor().addListener(this::onMasterSelectionChanged);
		}
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		detailModels.models.keySet().forEach(detailModel -> detailModels.models.get(detailModel).onInsert(insertedEntities));
	}

	private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
		detailModels.models.keySet().forEach(detailModel -> detailModels.models.get(detailModel).onUpdate(updatedEntities));
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		detailModels.models.keySet().forEach(detailModel -> detailModels.models.get(detailModel).onDelete(deletedEntities));
	}

	private final class ActiveDetailModelConsumer implements Consumer<Boolean> {

		private final DetailModelLink<?, ?, ?> detailModelLink;

		private ActiveDetailModelConsumer(DetailModelLink<?, ?, ?> detailModelLink) {
			this.detailModelLink = detailModelLink;
		}

		@Override
		public void accept(Boolean active) {
			detailModels.linked.set(detailModels.models.values().stream()
							.filter(link -> link.active().get())
							.map(DetailModelLink::detailModel)
							.collect(Collectors.toList()));
			if (active) {
				detailModelLink.onSelection(activeEntities());
			}
		}
	}

	private final class DefaultDetailModels<M extends DefaultEntityModel<M, E, T>, E extends AbstractEntityEditModel,
					T extends EntityTableModel<E>> implements DetailModels<M, E, T> {

		private final Map<M, DetailModelLink<M, E, T>> models = new HashMap<>();
		private final ValueSet<M> linked = ValueSet.valueSet();

		@Override
		public void add(M... detailModels) {
			for (M detailModel : requireNonNull(detailModels)) {
				add(detailModel);
			}
		}

		@Override
		public ForeignKeyDetailModelLink<M, E, T> add(M detailModel) {
			Collection<ForeignKey> foreignKeys = requireNonNull(detailModel).editModel()
							.entityDefinition().foreignKeys().get(editModel.entityType());
			if (foreignKeys.isEmpty()) {
				throw new IllegalArgumentException("Entity " + detailModel.editModel().entityType() +
								" does not reference " + editModel.entityType() + " via a foreign key");
			}

			return add(detailModel, foreignKeys.iterator().next());
		}

		@Override
		public ForeignKeyDetailModelLink<M, E, T> add(M detailModel, ForeignKey foreignKey) {
			return add(new DefaultForeignKeyDetailModelLink<>(requireNonNull(detailModel), requireNonNull(foreignKey)));
		}

		@Override
		public <L extends DetailModelLink<M, E, T>> L add(L detailModelLink) {
			if (DefaultEntityModel.this == requireNonNull(detailModelLink).detailModel()) {
				throw new IllegalArgumentException("A model can not be its own detail model");
			}
			if (models.containsKey(detailModelLink.detailModel())) {
				throw new IllegalArgumentException("Detail model " + detailModelLink.detailModel() + " has already been added");
			}
			models.put(detailModelLink.detailModel(), detailModelLink);
			detailModelLink.active().addConsumer(new ActiveDetailModelConsumer(detailModelLink));

			return detailModelLink;
		}

		@Override
		public boolean contains(Class<? extends M> modelClass) {
			requireNonNull(modelClass);
			return models.keySet().stream()
							.anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
		}

		@Override
		public boolean contains(EntityType entityType) {
			requireNonNull(entityType);
			return models.keySet().stream()
							.anyMatch(detailModel -> detailModel.entityType().equals(entityType));
		}

		@Override
		public boolean contains(M detailModel) {
			return models.containsKey(requireNonNull(detailModel));
		}

		@Override
		public Collection<M> get() {
			return unmodifiableCollection(models.keySet());
		}

		@Override
		public <L extends DetailModelLink<M, E, T>> L link(M detailModel) {
			if (!models.containsKey(requireNonNull(detailModel))) {
				throw new IllegalStateException("Detail model not found: " + detailModel);
			}

			return (L) models.get(detailModel);
		}

		@Override
		public ObservableValueSet<M> linked() {
			return linked.observable();
		}

		@Override
		public <C extends M> C get(Class<C> modelClass) {
			requireNonNull(modelClass);
			return (C) models.keySet().stream()
							.filter(detailModel -> detailModel.getClass().equals(modelClass))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found in model: " + this));
		}

		@Override
		public <C extends M> C get(EntityType entityType) {
			requireNonNull(entityType);
			return (C) models.keySet().stream()
							.filter(detailModel -> detailModel.entityType().equals(entityType))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this));
		}
	}
}