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
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.AbstractEntityTableModel;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TableModelListener;
import java.util.Collection;
import java.util.function.Consumer;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.keys;
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
	 * @param connection the connection
	 */
	public SwingEntityTableModel(EntityType entityType, EntityConnection connection) {
		this(entityType, connection, config -> {});
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param entityType the entityType
	 * @param connection the connection
	 * @param config the table model configuration
	 */
	public SwingEntityTableModel(EntityType entityType, EntityConnection connection, Consumer<Config> config) {
		this(new SwingEntityEditModel(entityType, connection), config);
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel) {
		this(editModel, config -> {});
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 * @param config the table model configuration
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel, Consumer<Config> config) {
		super(requireNonNull(editModel), tableModelBuilder(editModel.editor()), config);
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

	/**
	 * Returns a table model of the given entities, without an item source or query conditions: refreshing its items
	 * does nothing. The items still change via {@link #items()}, and via its edit model on insert, update and delete.
	 * @param entities the entities, of the same type
	 * @param connection the connection
	 * @return a new SwingEntityTableModel of the given entities
	 * @throws IllegalArgumentException in case {@code entities} is empty
	 * @see #of(EntityType, Collection, EntityConnection)
	 */
	public static SwingEntityTableModel of(Collection<Entity> entities, EntityConnection connection) {
		return of(entityType(entities), entities, connection);
	}

	/**
	 * Returns a table model of the given entities, without an item source or query conditions: refreshing its items
	 * does nothing. The items still change via {@link #items()}, and via its edit model on insert, update and delete.
	 * @param entityType the entity type
	 * @param entities the entities, possibly none
	 * @param connection the connection
	 * @return a new SwingEntityTableModel of the given entities
	 */
	public static SwingEntityTableModel of(EntityType entityType, Collection<Entity> entities, EntityConnection connection) {
		return new SwingEntityTableModel(new SwingEntityEditModel(entityType, connection), requireNonNull(entities));
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
							.include(query().attributes().included().get())
							.exclude(query().attributes().excluded().get()));
		}

		@Override
		public void onResult(Collection<Entity> entities) {
			replace(entities);
		}
	}
}