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

import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.ObservableValueSet;
import is.codion.common.reactive.value.ValueSet;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.prefs.Preferences;

import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;

/**
 * An abstract {@link EntityModel} implementation.
 * @param <M> the {@link EntityModel} type
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public abstract class AbstractEntityModel<M extends EntityModel<M, E, T, R>, E extends EntityEditModel<R>,
				T extends EntityTableModel<E, R>, R extends EntityEditor<R>> implements EntityModel<M, E, T, R> {

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEntityModel.class);

	private static final String MODEL = "model";
	private static final String DETAILS = "details";

	/**
	 * The node under which edit model preferences are stored, shared with {@link AbstractEntityTableModel},
	 * which persists the edit model on behalf of a table backed model.
	 */
	static final String EDIT = "edit";

	private final E editModel;
	private final @Nullable T tableModel;
	private final DetailModels<M, E, T, R> detailModels = new DefaultDetailModels();

	/**
	 * Instantiates a new {@link AbstractEntityModel}, without a table model
	 * @param editModel the edit model
	 */
	protected AbstractEntityModel(E editModel) {
		this.editModel = requireNonNull(editModel);
		this.tableModel = null;
		bindEventsInternal();
	}

	/**
	 * Instantiates a new {@link AbstractEntityModel}
	 * @param tableModel the table model
	 */
	protected AbstractEntityModel(T tableModel) {
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
	public final E editModel() {
		return editModel;
	}

	@Override
	public final R editor() {
		return editModel.editor();
	}

	@Override
	public final T tableModel() {
		if (tableModel == null) {
			throw new IllegalStateException("Entity model " + this + " does not contain a table model");
		}

		return tableModel;
	}

	@Override
	public final boolean containsTableModel() {
		return tableModel != null;
	}

	@Override
	public final DetailModels<M, E, T, R> detail() {
		return detailModels;
	}

	@Override
	public String preferencesKey() {
		return entityType().name();
	}

	@Override
	public void store(Preferences preferences) {
		Preferences entityNode = requireNonNull(preferences).node(preferencesKey());
		Preferences modelNode = entityNode.node(MODEL);
		if (tableModel != null) {
			tableModel.store(modelNode);
		}
		else {
			editModel.store(modelNode.node(EDIT));
		}
		Map<M, ModelLink> details = detailModels.get();
		if (!details.isEmpty()) {
			Preferences detailNode = entityNode.node(DETAILS);
			Set<String> keys = new HashSet<>();
			for (M detailModel : details.keySet()) {
				if (!keys.add(detailModel.preferencesKey())) {
					// Last writer wins - override preferencesKey() to disambiguate
					LOG.warn("Duplicate detail model preferences key '{}' under '{}'", detailModel.preferencesKey(), preferencesKey());
				}
				detailModel.store(detailNode);
			}
		}
	}

	@Override
	public void restore(Preferences preferences) {
		Preferences entityNode = requireNonNull(preferences).node(preferencesKey());
		Preferences modelNode = entityNode.node(MODEL);
		if (tableModel != null) {
			tableModel.restore(modelNode);
		}
		else {
			editModel.restore(modelNode.node(EDIT));
		}
		Map<M, ModelLink> details = detailModels.get();
		if (!details.isEmpty()) {
			Preferences detailNode = entityNode.node(DETAILS);
			for (M detailModel : details.keySet()) {
				detailModel.restore(detailNode);
			}
		}
	}

	/**
	 * @param link the link
	 * @return the underlying {@link DefaultModelLink}, unwrapping a {@link ForeignKeyModelLink}
	 */
	private DefaultModelLink<M, E, T, R> modelLink(ModelLink link) {
		if (link instanceof ForeignKeyModelLink) {
			return ((DefaultForeignKeyModelLink<M, E, T, R>) link).modelLink();
		}

		return (DefaultModelLink<M, E, T, R>) link;
	}

	private void selectionChanged(Collection<Entity> entities) {
		detailModels.get().values().stream()
						.map(this::modelLink)
						.forEach(link -> link.onSelection(entities));
	}

	private void entityChanged() {
		if (editModel.editor().entity().exists().is()) {
			selectionChanged(singletonList(editModel.editor().entity().get()));
		}
		else {
			selectionChanged(emptyList());
		}
	}

	private void bindEventsInternal() {
		editModel.editor().events().after().insert().addConsumer(this::onInsert);
		editModel.editor().events().after().update().addConsumer(this::onUpdate);
		editModel.editor().events().after().delete().addConsumer(this::onDelete);
		if (containsTableModel()) {
			tableModel.selection().items().addConsumer(this::selectionChanged);
		}
		else {
			editModel.editor().entity().addListener(this::entityChanged);
		}
	}

	private void onInsert(Collection<Entity> insertedEntities) {
		detailModels.get().values().stream()
						.map(this::modelLink)
						.forEach(link -> link.onInsert(insertedEntities));
	}

	private void onUpdate(Map<Entity, Entity> updatedEntities) {
		detailModels.get().values().stream()
						.map(this::modelLink)
						.forEach(link -> link.onUpdate(updatedEntities));
	}

	private void onDelete(Collection<Entity> deletedEntities) {
		detailModels.get().values().stream()
						.map(this::modelLink)
						.forEach(link -> link.onDelete(deletedEntities));
	}

	private final class DefaultDetailModels implements DetailModels<M, E, T, R> {

		private final Map<M, ModelLink> models = new HashMap<>();
		private final ValueSet<M> active = ValueSet.valueSet();

		@Override
		public void add(M... detailModels) {
			for (M detailModel : requireNonNull(detailModels)) {
				add(detailModel);
			}
		}

		@Override
		public void add(M detailModel) {
			add(detailModel, inferForeignKey(detailModel));
		}

		@Override
		public void add(M detailModel, ForeignKey foreignKey) {
			add(ForeignKeyModelLink.builder()
							.model(detailModel)
							.foreignKey(foreignKey)
							.build());
		}

		@Override
		public void add(ModelLink link) {
			DefaultModelLink<M, E, T, R> modelLink = modelLink(requireNonNull(link));
			if (AbstractEntityModel.this == modelLink.model()) {
				throw new IllegalArgumentException("A model can not be its own detail model");
			}
			if (models.containsKey(modelLink.model())) {
				throw new IllegalArgumentException("Detail model " + modelLink.model() + " has already been added");
			}
			DefaultForeignKeyModelLink<M, E, T, R> foreignKeyLink =
							link instanceof ForeignKeyModelLink ? (DefaultForeignKeyModelLink<M, E, T, R>) link : null;
			if (foreignKeyLink != null) {
				validateForeignKey(foreignKeyLink.foreignKey(), modelLink.model());
			}
			modelLink.configure();
			if (foreignKeyLink != null) {
				foreignKeyLink.configure();
			}
			models.put(modelLink.model(), link);
			if (modelLink.active().is()) {
				active.add(modelLink.model());
				modelLink.onSelection(activeEntities());
			}
			modelLink.active().addConsumer(new ActiveChanged(modelLink));
		}

		@Override
		public boolean contains(M detailModel) {
			return models.containsKey(requireNonNull(detailModel));
		}

		@Override
		public Map<M, ModelLink> get() {
			return unmodifiableMap(models);
		}

		@Override
		public State active(M detailModel) {
			if (!models.containsKey(requireNonNull(detailModel))) {
				throw new IllegalStateException("Detail model not found: " + detailModel);
			}

			return modelLink(models.get(detailModel)).active();
		}

		@Override
		public ObservableValueSet<M> active() {
			return active.observable();
		}

		@Override
		public <C extends M> C get(Class<C> modelClass) {
			requireNonNull(modelClass);
			return (C) models.keySet().stream()
							.filter(detailModel -> detailModel.getClass().equals(modelClass))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("Detail model of type " + modelClass.getName() + " not found in model: " + AbstractEntityModel.this));
		}

		@Override
		public M get(EntityType entityType) {
			requireNonNull(entityType);
			return models.keySet().stream()
							.filter(detailModel -> detailModel.entityType().equals(entityType))
							.findFirst()
							.orElseThrow(() -> new IllegalArgumentException("No detail model for entity " + entityType + " found in model: " + AbstractEntityModel.this));
		}

		/**
		 * Infers the foreign key on which to base a detail model link, requiring exactly one.
		 * <p>Uniqueness is a requirement of the inference, not of linking; a detail model referencing
		 * the master via multiple foreign keys is linked by naming one of them explicitly.
		 * @param model the detail model
		 * @return the single foreign key referencing this model's entity type
		 */
		private ForeignKey inferForeignKey(EntityModel<?, ?, ?, ?> model) {
			Collection<ForeignKey> foreignKeys = model.entityDefinition().foreignKeys().get(editModel.entityType());
			if (foreignKeys.isEmpty()) {
				throw new IllegalArgumentException("Entity " + model.entityType() +
								" does not reference " + entityType() + " via a foreign key");
			}
			if (foreignKeys.size() > 1) {
				throw new IllegalArgumentException("Entity " + model.entityType() +
								" references " + entityType() + " via multiple foreign keys");
			}

			return foreignKeys.iterator().next();
		}

		private void validateForeignKey(ForeignKey foreignKey, EntityModel<?, ?, ?, ?> model) {
			if (!foreignKey.entityType().equals(model.entityType())) {
				throw new IllegalArgumentException("Foreign key " + foreignKey + " is not based on entity " + model.entityType());
			}
			if (!foreignKey.referencedType().equals(entityType())) {
				throw new IllegalArgumentException("Foreign key " + foreignKey + " does not reference entity " + entityType());
			}
		}

		private final class ActiveChanged implements Consumer<Boolean> {

			private final DefaultModelLink<M, E, T, R> modelLink;

			private ActiveChanged(DefaultModelLink<M, E, T, R> modelLink) {
				this.modelLink = modelLink;
			}

			@Override
			public void accept(Boolean isActive) {
				if (isActive) {
					active.add(modelLink.model());
					modelLink.onSelection(activeEntities());
				}
				else {
					active.remove(modelLink.model());
				}
			}
		}

		private List<Entity> activeEntities() {
			if (tableModel != null && tableModel.selection().empty().not().is()) {
				return tableModel.selection().items().get();
			}
			else if (editModel.editor().entity().exists().not().is()) {
				return emptyList();
			}

			return singletonList(editModel.editor().entity().get());
		}
	}
}