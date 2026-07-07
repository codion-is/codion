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
package is.codion.swing.framework.model;

import is.codion.common.model.worker.ProgressWorker;
import is.codion.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.AbstractEntityTableModel;
import is.codion.framework.model.EntityConditionModel;
import is.codion.framework.model.EntityQueryModel;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TableModelListener;
import java.util.Collection;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.keys;
import static is.codion.framework.model.EntityQueryModel.entityQueryModel;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends AbstractEntityTableModel<SwingEntityEditModel, SwingEntityEditor>
				implements SwingFilterTableModel<Entity, Attribute<?>> {

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param entityType the entityType
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new SwingEntityEditModel(entityType, connectionProvider));
	}

	/**
	 * Instantiates a new SwingEntityTableModel containing the given entities.
	 * @param entities the entities to populate the model with
	 * @param connectionProvider the connection provider
	 * @throws IllegalArgumentException in case {@code entities} is empty
	 */
	public SwingEntityTableModel(Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
		this(entityType(entities), entities, connectionProvider);
	}

	/**
	 * Instantiates a new SwingEntityTableModel containing the given entities.
	 * @param entityType the entity type
	 * @param entities the entities to populate the model with
	 * @param connectionProvider the connection provider
	 * @throws IllegalArgumentException in case {@code entities} is empty
	 */
	public SwingEntityTableModel(EntityType entityType, Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
		this(new SwingEntityEditModel(entityType, connectionProvider), requireNonNull(entities));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param conditionModel the {@link EntityConditionModel}
	 */
	public SwingEntityTableModel(EntityConditionModel conditionModel) {
		this(entityQueryModel(conditionModel));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param queryModel the table query model
	 */
	public SwingEntityTableModel(EntityQueryModel queryModel) {
		this(new SwingEntityEditModel(requireNonNull(queryModel).entityType(),
						queryModel.condition().connectionProvider()), queryModel);
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel) {
		this(editModel, entityQueryModel(EntityConditionModel.builder()
						.entityType(editModel.entityType())
						.connectionProvider(editModel.connectionProvider())
						.conditions(new SwingEntityConditions(editModel.entityType(), editModel.connectionProvider()))
						.build()));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 * @param queryModel the table query model
	 * @throws IllegalArgumentException in case the edit model and query model entity types are not the same
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel, EntityQueryModel queryModel) {
		super(requireNonNull(editModel), queryModel, tableModelBuilder(editModel.editor())
						.items(requireNonNull(queryModel)::query)
						.build());
	}

	private SwingEntityTableModel(SwingEntityEditModel editModel, Collection<Entity> items) {
		super(requireNonNull(editModel), tableModelBuilder(editModel.editor()).build());
		items().add(requireNonNull(items));
	}

	/**
	 * Returns true if the cell at {@code rowIndex} and {@code modelColumnIndex} is editable.
	 * @param rowIndex the row to edit
	 * @param modelColumnIndex the model index of the column to edit
	 * @return true if the cell is editable
	 */
	@Override
	public final boolean isCellEditable(int rowIndex, int modelColumnIndex) {
		return filterModel().isCellEditable(rowIndex, modelColumnIndex);
	}

	/**
	 * Sets the value in the given cell and updates the underlying Entity.
	 * @param value the new value
	 * @param rowIndex the row whose value is to be changed
	 * @param modelColumnIndex the model index of the column to be changed
	 */
	@Override
	public final void setValueAt(@Nullable Object value, int rowIndex, int modelColumnIndex) {
		filterModel().setValueAt(value, rowIndex, modelColumnIndex);
	}

	@Override
	public final int getRowCount() {
		return filterModel().getRowCount();
	}

	@Override
	public final @Nullable Object getValueAt(int rowIndex, int columnIndex) {
		return filterModel().getValueAt(rowIndex, columnIndex);
	}

	@Override
	public final void fireTableDataChanged() {
		filterModel().fireTableDataChanged();
	}

	@Override
	public final void fireTableRowsUpdated(int fromIndex, int toIndex) {
		filterModel().fireTableRowsUpdated(fromIndex, toIndex);
	}

	@Override
	public final FilterListSelection<Entity> selection() {
		return filterModel().selection();
	}

	@Override
	public final int getColumnCount() {
		return filterModel().getColumnCount();
	}

	@Override
	public final String getColumnName(int columnIndex) {
		return filterModel().getColumnName(columnIndex);
	}

	@Override
	public final Class<?> getColumnClass(int columnIndex) {
		return filterModel().getColumnClass(columnIndex);
	}

	@Override
	public final void addTableModelListener(TableModelListener listener) {
		filterModel().addTableModelListener(listener);
	}

	@Override
	public final void removeTableModelListener(TableModelListener listener) {
		filterModel().removeTableModelListener(listener);
	}

	@Override
	public final SwingEntityRowEditor rowEditor() {
		return (SwingEntityRowEditor) filterModel().rowEditor();
	}

	@Override
	public final void refresh(Collection<Entity.Key> keys) {
		if (!requireNonNull(keys).isEmpty()) {
			RefreshTask task = new RefreshTask(keys);
			if (isEventDispatchThread()) {
				ProgressWorker.builder()
								.task(task)
								.execute();
			}
			else {
				task.onResult(task.execute());
			}
		}
	}

	@Override
	protected final SwingFilterTableModel<Entity, Attribute<?>> filterModel() {
		return (SwingFilterTableModel<Entity, Attribute<?>>) super.filterModel();
	}

	@Override
	protected final void onRowsUpdated(int fromIndex, int toIndex) {
		fireTableRowsUpdated(fromIndex, toIndex);
	}

	private static SwingFilterTableModel.Builder<Entity, Attribute<?>> tableModelBuilder(SwingEntityEditor editor) {
		return SwingFilterTableModel.builder()
						.columns(tableColumns(editor.entityDefinition()))
						.filters(filterConditions(editor.entityDefinition()))
						.validator(itemValidator(editor.entityDefinition().type()))
						.rowEditor(tableModel -> new SwingEntityRowEditor(editor));
	}

	private static EntityType entityType(Collection<Entity> entities) {
		if (requireNonNull(entities).isEmpty()) {
			throw new IllegalArgumentException("One or more entities is required to base a table model on");
		}

		return entities.iterator().next().type();
	}

	/**
	 * A Swing specific {@link EntityRowEditor} implementation.
	 */
	public static final class SwingEntityRowEditor extends AbstractEntityRowEditor<SwingEntityEditor>
					implements EntityRowEditor, RowEditor<Entity, Attribute<?>> {

		private SwingEntityRowEditor(SwingEntityEditor editor) {
			super(editor);
		}

		@Override
		public void set(@Nullable Object value, int rowIndex, Entity entity, Attribute<?> identifier) {
			super.set(value, entity, (Attribute<Object>) identifier);
		}
	}

	private final class RefreshTask implements ResultTaskHandler<Collection<Entity>> {

		private final Collection<Entity.Key> keys;

		private RefreshTask(Collection<Entity.Key> keys) {
			this.keys = keys;
		}

		@Override
		public Collection<Entity> execute() {
			if (keys.isEmpty()) {
				return emptyList();
			}

			return connection().select(where(keys(keys))
							.attributes(query().attributes().defaults().get())
							.include(query().attributes().include().get())
							.exclude(query().attributes().exclude().get()));
		}

		@Override
		public void onResult(Collection<Entity> entities) {
			replace(entities);
		}
	}

}