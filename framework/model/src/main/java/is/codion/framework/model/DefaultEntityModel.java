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

import is.codion.common.value.ValueSet;
import is.codion.common.value.ValueSetObserver;
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

	private static final String DETAIL_MODEL_PARAMETER = "detailModel";

	private final E editModel;
	private final T tableModel;
	private final Map<M, DetailModelLink<M, E, T>> detailModels = new HashMap<>();
	private final ValueSet<M> linkedDetailModels = ValueSet.valueSet();

	/**
	 * Instantiates a new DefaultEntityModel, without a table model
	 * @param editModel the edit model
	 */
	public DefaultEntityModel(E editModel) {
		requireNonNull(editModel, "editModel");
		this.editModel = editModel;
		this.tableModel = null;
		bindEventsInternal();
	}

	/**
	 * Instantiates a new DefaultEntityModel
	 * @param tableModel the table model
	 */
	public DefaultEntityModel(T tableModel) {
		requireNonNull(tableModel, "tableModel");
		this.editModel = tableModel.editModel();
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
	@SafeVarargs
	public final void addDetailModels(M... detailModels) {
		requireNonNull(detailModels, "detailModels");
		for (M detailModel : detailModels) {
			addDetailModel(detailModel);
		}
	}

	@Override
	public final ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel) {
		requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
		Collection<ForeignKey> foreignKeys = detailModel.editModel().entityDefinition().foreignKeys().get(editModel.entityType());
		if (foreignKeys.isEmpty()) {
			throw new IllegalArgumentException("Entity " + detailModel.editModel().entityType() +
							" does not reference " + editModel.entityType() + " via a foreign key");
		}

		return addDetailModel(detailModel, foreignKeys.iterator().next());
	}

	@Override
	public final ForeignKeyDetailModelLink<M, E, T> addDetailModel(M detailModel, ForeignKey foreignKey) {
		requireNonNull(detailModel, DETAIL_MODEL_PARAMETER);
		requireNonNull(foreignKey, "foreignKey");

		return addDetailModel(new DefaultForeignKeyDetailModelLink<>(detailModel, foreignKey));
	}

	@Override
	public final <L extends DetailModelLink<M, E, T>> L addDetailModel(L detailModelLink) {
		requireNonNull(detailModelLink, "detailModelLink");
		if (this == detailModelLink.detailModel()) {
			throw new IllegalArgumentException("A model can not be its own detail model");
		}
		if (detailModels.containsKey(detailModelLink.detailModel())) {
			throw new IllegalArgumentException("Detail model " + detailModelLink.detailModel() + " has already been added");
		}
		detailModels.put(detailModelLink.detailModel(), detailModelLink);
		detailModelLink.active().addConsumer(new ActiveDetailModelConsumer(detailModelLink));

		return detailModelLink;
	}

	@Override
	public final boolean containsDetailModel(Class<? extends M> modelClass) {
		requireNonNull(modelClass, "modelClass");
		return detailModels.keySet().stream()
						.anyMatch(detailModel -> detailModel.getClass().equals(modelClass));
	}

	@Override
	public final boolean containsDetailModel(EntityType entityType) {
		requireNonNull(entityType, "entityType");
		return detailModels.keySet().stream()
						.anyMatch(detailModel -> detailModel.entityType().equals(entityType));
	}

	@Override
	public final boolean containsDetailModel(M detailModel) {
		return detailModels.containsKey(requireNonNull(detailModel, DETAIL_MODEL_PARAMETER));
	}

	@Override
	public final Collection<M> detailModels() {
		return unmodifiableCollection(detailModels.keySet());
	}

	@Override
	public final <L extends DetailModelLink<M, E, T>> L detailModelLink(M detailModel) {
		if (!detailModels.containsKey(requireNonNull(detailModel))) {
			throw new IllegalStateException("Detail model not found: " + detailModel);
		}

		return (L) detailModels.get(detailModel);
	}

	@Override
	public final ValueSetObserver<M> linkedDetailModels() {
		return linkedDetailModels.observer();
	}

	@Override
	public final <C extends M> C detailModel(Class<C> modelClass) {
		requireNonNull(modelClass, "modelClass");
		return (C) detailModels.keySet().stream()
						.filter(detailModel -> detailModel.getClass().equals(modelClass))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found in model: " + this));
	}

	@Override
	public final <C extends M> C detailModel(EntityType entityType) {
		requireNonNull(entityType, "entityType");
		return (C) detailModels.keySet().stream()
						.filter(detailModel -> detailModel.entityType().equals(entityType))
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + this));
	}

	private void onMasterSelectionChanged() {
		if (!linkedDetailModels().empty()) {
			List<Entity> activeEntities = activeEntities();
			for (M detailModel : linkedDetailModels()) {
				detailModels.get(detailModel).onSelection(activeEntities);
			}
		}
	}

	private List<Entity> activeEntities() {
		if (tableModel != null && tableModel.selectionModel().selectionNotEmpty().get()) {
			return tableModel.selectionModel().selectedItems();
		}
		else if (editModel.exists().not().get()) {
			return emptyList();
		}

		return singletonList(editModel.entity());
	}

	private void bindEventsInternal() {
		editModel.afterInsertEvent().addConsumer(this::onInsert);
		editModel.afterUpdateEvent().addConsumer(this::onUpdate);
		editModel.afterDeleteEvent().addConsumer(this::onDelete);
		if (containsTableModel()) {
			tableModel.selectionEvent().addListener(this::onMasterSelectionChanged);
		}
		else {
			editModel.entityEvent().addListener(this::onMasterSelectionChanged);
		}
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onInsert(insertedEntities));
	}

	private void onUpdate(Map<Entity.Key, Entity> updatedEntities) {
		detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onUpdate(updatedEntities));
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		detailModels.keySet().forEach(detailModel -> detailModels.get(detailModel).onDelete(deletedEntities));
	}

	private final class ActiveDetailModelConsumer implements Consumer<Boolean> {

		private final DetailModelLink<?, ?, ?> detailModelLink;

		private ActiveDetailModelConsumer(DetailModelLink<?, ?, ?> detailModelLink) {
			this.detailModelLink = detailModelLink;
		}

		@Override
		public void accept(Boolean active) {
			linkedDetailModels.set(detailModels.values().stream()
							.filter(link -> link.active().get())
							.map(DetailModelLink::detailModel)
							.collect(Collectors.toList()));
			if (active) {
				detailModelLink.onSelection(activeEntities());
			}
		}
	}
}