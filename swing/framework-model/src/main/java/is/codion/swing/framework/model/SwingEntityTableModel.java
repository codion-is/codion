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

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.domain.entity.attribute.ValueAttributeDefinition;
import is.codion.framework.model.AbstractEntityTableModel;
import is.codion.framework.model.EntityConditionModel;
import is.codion.framework.model.EntityEditor.EditorEntity;
import is.codion.framework.model.EntityQueryModel;
import is.codion.swing.common.model.component.list.FilterListSelection;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSort;
import is.codion.swing.common.model.component.table.FilterTableSort.ColumnSortOrder;
import is.codion.swing.common.model.worker.ProgressWorker;
import is.codion.swing.common.model.worker.ProgressWorker.ResultTaskHandler;

import org.jspecify.annotations.Nullable;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.condition.Condition.keys;
import static is.codion.framework.model.EntityQueryModel.entityQueryModel;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static javax.swing.SwingUtilities.isEventDispatchThread;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends AbstractEntityTableModel<SwingEntityModel, SwingEntityEditModel,
				SwingEntityTableModel, SwingEntityEditor> implements FilterTableModel<Entity, Attribute<?>> {

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param entityType the entityType
	 * @param connectionProvider the connection provider
	 */
	public SwingEntityTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
		this(new SwingEntityEditModel(entityType, connectionProvider));
	}

	/**
	 * Instantiates a new SwingEntityTableModel containing the given entites.
	 * @param entities the entities to populate with model with
	 * @param connectionProvider the connection provider
	 * @throws IllegalArgumentException in case {@code entities} is empty
	 */
	public SwingEntityTableModel(Collection<Entity> entities, EntityConnectionProvider connectionProvider) {
		this(entityType(entities), entities, connectionProvider);
	}

	/**
	 * Instantiates a new SwingEntityTableModel containing the given entites.
	 * @param entityType the entity type
	 * @param entities the entities to populate with model with
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
	 * @throws IllegalArgumentException in case the edit model and query model entity type is not the same
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel, EntityQueryModel queryModel) {
		super(requireNonNull(editModel), queryModel, tableModelBuilder(editModel.editor())
						.items(requireNonNull(queryModel)::query)
						.build());
		addTableModelListener(this::onTableModelEvent);
	}

	private SwingEntityTableModel(SwingEntityEditModel editModel, Collection<Entity> items) {
		super(requireNonNull(editModel), tableModelBuilder(editModel.editor()).build());
		items().add(requireNonNull(items));
	}

	/**
	 * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
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
	public final Items<Entity> items() {
		return filterModel().items();
	}

	@Override
	public final ColumnValues<Attribute<?>> values() {
		return filterModel().values();
	}

	@Override
	public final Class<?> getColumnClass(Attribute<?> attribute) {
		return filterModel().getColumnClass(attribute);
	}

	@Override
	public final FilterListSelection<Entity> selection() {
		return filterModel().selection();
	}

	@Override
	public final TableConditionModel<Attribute<?>> filters() {
		return filterModel().filters();
	}

	@Override
	public final FilterTableSort<Entity, Attribute<?>> sort() {
		return filterModel().sort();
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
	public final TableColumns<Entity, Attribute<?>> columns() {
		return filterModel().columns();
	}

	@Override
	public final Export<Attribute<?>> export() {
		return filterModel().export();
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
	protected final FilterTableModel<Entity, Attribute<?>> filterModel() {
		return (FilterTableModel<Entity, Attribute<?>>) super.filterModel();
	}

	@Override
	protected final void onRowsUpdated(int fromIndex, int toIndex) {
		fireTableRowsUpdated(fromIndex, toIndex);
	}

	@Override
	protected final Optional<OrderBy> orderBy() {
		List<ColumnSortOrder<Attribute<?>>> columnSortOrder = sort().columns().get().stream()
						.filter(sortOrder -> sortOrder.identifier() instanceof Column)
						.collect(toList());
		if (columnSortOrder.isEmpty()) {
			return Optional.empty();
		}
		OrderBy.Builder builder = OrderBy.builder();
		columnSortOrder.forEach(sortOrder -> {
			switch (sortOrder.sortOrder()) {
				case ASCENDING:
					builder.ascending((Column<?>) sortOrder.identifier());
					break;
				case DESCENDING:
					builder.descending((Column<?>) sortOrder.identifier());
					break;
				default:
					break;
			}
		});

		return Optional.of(builder.build());
	}

	private void onTableModelEvent(TableModelEvent tableModelEvent) {
		// Syncs the editor when the selected row is updated via the table (e.g. inline cell or multi item editing).
		// The equalValues() guard prevents a redundant editor set when the update originated
		// from the editor itself, since the editor already holds the updated entity.
		if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == selection().index()
						.optional()
						.orElse(-1)
						.intValue()) {
			Entity selected = selection().item().get();
			EditorEntity editorEntity = editor().entity();
			if (!editorEntity.get().equalValues(selected)) {
				editorEntity.set(selected);
			}
		}
	}

	private static FilterTableModel.Builder<Entity, Attribute<?>> tableModelBuilder(SwingEntityEditor editor) {
		return FilterTableModel.builder()
						.columns(new EntityTableColumns(editor.entityDefinition()))
						.filters(new EntityFilters(editor.entityDefinition()))
						.validator(new EntityItemValidator(editor.entityDefinition().type()))
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

	private static final class EntityTableColumns implements TableColumns<Entity, Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final List<Attribute<?>> identifiers;

		private EntityTableColumns(EntityDefinition entityDefinition) {
			this.entityDefinition = entityDefinition;
			this.identifiers = unmodifiableList(entityDefinition.attributes().definitions().stream()
							.filter(attributeDefinition -> !attributeDefinition.hidden())
							.map(AttributeDefinition::attribute)
							.collect(toList()));
			if (this.identifiers.isEmpty()) {
				throw new IllegalArgumentException("No visible attributes found for entity '" +
								entityDefinition.type() + "'. Ensure at least one attribute has a caption() " +
								"defined to make it visible in table views. Attributes without captions are " +
								"hidden by default.");
			}
		}

		@Override
		public List<Attribute<?>> identifiers() {
			return identifiers;
		}

		@Override
		public String caption(Attribute<?> identifier) {
			return entityDefinition.attributes().definition(identifier).caption();
		}

		@Override
		public Optional<String> description(Attribute<?> identifier) {
			return entityDefinition.attributes().definition(identifier).description();
		}

		@Override
		public Class<?> columnClass(Attribute<?> identifier) {
			return requireNonNull(identifier).type().valueClass();
		}

		@Override
		public @Nullable Object value(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).get(attribute);
		}

		@Override
		public String formatted(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).formatted(attribute);
		}

		@Override
		public Comparator<?> comparator(Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				return entityDefinition.foreignKeys().referencedBy((ForeignKey) attribute).comparator();
			}

			return entityDefinition.attributes().definition(attribute).comparator();
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
							.exclude(query().attributes().exclude().get())
							.build());
		}

		@Override
		public void onResult(Collection<Entity> entities) {
			replace(entities);
		}
	}

	private static final class EntityFilters implements Supplier<Map<Attribute<?>, ConditionModel<?>>> {

		private final EntityDefinition entityDefinition;

		private EntityFilters(EntityDefinition entityDefinition) {
			this.entityDefinition = requireNonNull(entityDefinition);
		}

		@Override
		public Map<Attribute<?>, ConditionModel<?>> get() {
			return entityDefinition.attributes().definitions().stream()
							.filter(EntityFilters::include)
							.collect(toMap(AttributeDefinition::attribute, EntityFilters::condition));
		}

		private static ConditionModel<?> condition(AttributeDefinition<?> definition) {
			if (useStringCondition(definition)) {
				// Covers foreign keys
				return ConditionModel.builder()
								.valueClass(String.class)
								.build();
			}

			ValueAttributeDefinition<?> attributeDefinition = (ValueAttributeDefinition<?>) definition;
			return ConditionModel.builder()
							.valueClass(attributeDefinition.attribute().type().valueClass())
							.format(attributeDefinition.format().orElse(null))
							.dateTimePattern(attributeDefinition.dateTimePattern().orElse(null))
							.build();
		}

		private static boolean include(AttributeDefinition<?> definition) {
			return !definition.hidden() && (definition instanceof ForeignKeyDefinition || definition instanceof ValueAttributeDefinition<?>);
		}

		private static boolean useStringCondition(AttributeDefinition<?> definition) {
			return definition.attribute().type().isEntity() || // entities
							itemBased(definition) || // items
							!Comparable.class.isAssignableFrom(definition.attribute().type().valueClass()); // non-comparables
		}

		private static boolean itemBased(AttributeDefinition<?> definition) {
			return definition instanceof ValueAttributeDefinition<?> && !((ValueAttributeDefinition<?>) definition).items().isEmpty();
		}
	}

	private static final class EntityItemValidator implements Predicate<Entity> {

		private final EntityType entityType;

		private EntityItemValidator(EntityType entityType) {
			this.entityType = requireNonNull(entityType);
		}

		@Override
		public boolean test(Entity entity) {
			return entity.type().equals(entityType);
		}
	}
}