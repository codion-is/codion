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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractEntityEditor;
import is.codion.framework.model.EntityEditor.EditorTask.Result;
import is.codion.swing.common.model.component.combobox.SwingComboBoxModel;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTaskHandler;
import is.codion.swing.framework.model.component.SwingEntityComboBoxModel;

import org.jspecify.annotations.Nullable;

import java.util.Map;

import static java.util.Objects.requireNonNull;
import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * A Swing {@link AbstractEntityEditor} implementation.
 */
public final class SwingEntityEditor extends AbstractEntityEditor<SwingEntityEditor> {

	private @Nullable ProgressWorker<Result<Entity>, ?> worker;
	private @Nullable EditorTask<?> currentTask;

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(entityType, connectionProvider, new SwingComponentModels() {});
	}

	/**
	 * Instantiates a new {@link SwingEntityEditor}
	 * @param entityType the entity type
	 * @param connectionProvider the connection provider
	 * @param componentModels the component models
	 */
	public SwingEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider,
													 SwingComponentModels componentModels) {
		super(entityType, connectionProvider, componentModels, DefaultSwingComboBoxModels::new);
	}

	@Override
	public SwingComboBoxModels comboBoxModels() {
		return (SwingComboBoxModels) super.comboBoxModels();
	}

	@Override
	protected SwingComponentModels componentModels() {
		return (SwingComponentModels) super.componentModels();
	}

	@Override
	protected void execute(EditorTask<Entity> task) {
		requireNonNull(task);
		cancelCurrentWorker();
		currentTask = task;
		if (async().is() && isEventDispatchThread()) {
			worker = ProgressWorker.builder()
							.task(new ExecutionTask(task))
							.execute();
		}
		else {
			super.execute(task);
		}
	}

	@Override
	protected void supersede() {
		// a synchronous reset (set/replace/defaults/clear) is about to be applied; drop any in-flight
		// worker so its result is discarded (currentTask == null) instead of clobbering the reset
		cancelCurrentWorker();
		currentTask = null;
	}

	private void cancelCurrentWorker() {
		ProgressWorker<Result<Entity>, ?> currentWorker = worker;
		if (currentWorker != null && !currentWorker.isDone()) {
			// cancellation is a best-effort optimization, freeing the connection of a superseded load early
			currentWorker.cancel(true);
		}
	}

	private final class ExecutionTask implements ResultTaskHandler<Result<Entity>> {

		private final EditorTask<Entity> task;

		private ExecutionTask(EditorTask<Entity> task) {
			this.task = task;
		}

		@Override
		public Result<Entity> execute() throws Exception {
			return task.perform();
		}

		@Override
		public void onResult(Result<Entity> result) {
			if (currentTask == task) {
				currentTask = null;
				result.handle();
			}
		}

		@Override
		public void onDone() {
			if (currentTask == task) {
				worker = null;
			}
		}
	}

	/**
	 * Manages the combo box models used by a {@link SwingEntityEditor}.
	 */
	public interface SwingComboBoxModels extends ComboBoxModels {

		@Override
		Map<ForeignKey, SwingEntityComboBoxModel> foreignKey();

		@Override
		Map<Column<?>, SwingComboBoxModel<?>> column();

		@Override
		SwingEntityComboBoxModel get(ForeignKey foreignKey);

		@Override
		<T> SwingComboBoxModel<T> get(Column<T> column);

		@Override
		SwingEntityComboBoxModel create(ForeignKey foreignKey);

		@Override
		<T> SwingComboBoxModel<T> create(Column<T> column);
	}

	private static final class DefaultSwingComboBoxModels
					extends DefaultComboBoxModels<SwingEntityComboBoxModel, SwingComboBoxModel<?>> implements SwingComboBoxModels {

		private DefaultSwingComboBoxModels(AbstractEntityEditor<?> editor) {
			super(editor);
		}

		@Override
		public <T> SwingComboBoxModel<T> get(Column<T> column) {
			return (SwingComboBoxModel<T>) super.get(column);
		}

		@Override
		public <T> SwingComboBoxModel<T> create(Column<T> column) {
			return (SwingComboBoxModel<T>) super.create(column);
		}
	}

	/**
	 * <p>A {@link SwingComponentModels} extension providing foreign key based
	 * {@link SwingEntityComboBoxModel} and column based {@link SwingComboBoxModel}.
	 * <p>Override to customize combo box model creation.
	 */
	public interface SwingComponentModels extends ComponentModels {

		@Override
		default SwingEntityComboBoxModel comboBoxModel(ForeignKey foreignKey, EntityConnectionProvider connectionProvider) {
			return SwingEntityComboBoxModel.builder()
							.foreignKey(foreignKey)
							.connectionProvider(requireNonNull(connectionProvider))
							.build();
		}

		@Override
		default <T> SwingComboBoxModel<T> comboBoxModel(Column<T> column, EntityConnectionProvider connectionProvider) {
			EntityDefinition entityDefinition = requireNonNull(connectionProvider).entities()
							.definition(requireNonNull(column).entityType());
			boolean nullable = entityDefinition.columns().definition(column).nullable();

			return SwingComboBoxModel.builder()
							.items(() -> connectionProvider.connection().select(column))
							.nullItem(nullable ? ComponentModels.createNullItem(column) : null)
							.includeNull(nullable)
							.build();
		}
	}
}
