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
package is.codion.swing.framework.model;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.condition.TableConditionModel;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.AbstractEntityTableModel;
import is.codion.framework.model.EntityConditionModel;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableSelectionModel;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.Color;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.framework.model.EntityConditionModel.entityConditionModel;
import static is.codion.framework.model.EntityQueryModel.entityQueryModel;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * A TableModel implementation for displaying and working with entities.
 */
public class SwingEntityTableModel extends AbstractEntityTableModel<SwingEntityEditModel>
				implements EntityTableModel<SwingEntityEditModel>, FilterTableModel<Entity, Attribute<?>> {

	/**
	 * Caches java.awt.Color instances parsed from hex strings via {@link #toColor(Object)}
	 */
	private static final Map<String, Color> COLOR_CACHE = new ConcurrentHashMap<>();

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
	 * @param conditionModel the entity condition model
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
						queryModel.conditionModel().connectionProvider()), queryModel);
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel) {
		this(editModel, entityQueryModel(entityConditionModel(editModel.entityType(), editModel.connectionProvider(),
						new SwingEntityConditionModelFactory(editModel.connectionProvider()))));
	}

	/**
	 * Instantiates a new SwingEntityTableModel.
	 * @param editModel the edit model
	 * @param queryModel the table query model
	 * @throws IllegalArgumentException in case the edit model and condition model entity type is not the same
	 */
	public SwingEntityTableModel(SwingEntityEditModel editModel, EntityQueryModel queryModel) {
		super(requireNonNull(editModel), createFilterTableModel(editModel.entityDefinition(), queryModel), requireNonNull(queryModel));
		addTableModelListener(this::onTableModelEvent);
	}

	private SwingEntityTableModel(SwingEntityEditModel editModel, Collection<Entity> items) {
		super(requireNonNull(editModel), createFilterTableModel(editModel.entityDefinition(), () -> items));
		refresh();
	}

	/**
	 * Returns true if the cell at <code>rowIndex</code> and <code>modelColumnIndex</code> is editable.
	 * @param rowIndex the row to edit
	 * @param modelColumnIndex the model index of the column to edit
	 * @return true if the cell is editable
	 * @see #setValueAt(Object, int, int)
	 */
	@Override
	public boolean isCellEditable(int rowIndex, int modelColumnIndex) {
		if (!editable().get() || editModel().readOnly().get() || !editModel().updateEnabled().get()) {
			return false;
		}
		Attribute<?> attribute = columns().identifier(modelColumnIndex);
		if (attribute instanceof ForeignKey) {
			return entityDefinition().foreignKeys().updatable((ForeignKey) attribute);
		}

		AttributeDefinition<?> attributeDefinition = entityDefinition().attributes().definition(attribute);

		return attributeDefinition instanceof ColumnDefinition && ((ColumnDefinition<?>) attributeDefinition).updatable();
	}

	/**
	 * Sets the value in the given cell and updates the underlying Entity.
	 * @param value the new value
	 * @param rowIndex the row whose value is to be changed
	 * @param modelColumnIndex the model index of the column to be changed
	 */
	@Override
	public final void setValueAt(Object value, int rowIndex, int modelColumnIndex) {
		if (!editable().get() || editModel().readOnly().get() || !editModel().updateEnabled().get()) {
			throw new IllegalStateException("This table model is readOnly or has disabled update");
		}
		Entity entity = items().visible().itemAt(rowIndex).copy();
		entity.put((Attribute<Object>) columns().identifier(modelColumnIndex), value);
		try {
			if (entity.modified()) {
				editModel().update(singletonList(entity));
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public Color backgroundColor(int row, Attribute<?> attribute) {
		Object color = super.backgroundColor(row, attribute);

		return color == null ? null : toColor(color);
	}

	@Override
	public Color foregroundColor(int row, Attribute<?> attribute) {
		Object color = super.foregroundColor(row, attribute);

		return color == null ? null : toColor(color);
	}

	@Override
	public final int getRowCount() {
		return filterModel().getRowCount();
	}

	@Override
	public final Object getValueAt(int rowIndex, int columnIndex) {
		return filterModel().getValueAt(rowIndex, columnIndex);
	}

	@Override
	public final String getStringAt(int rowIndex, Attribute<?> attribute) {
		return filterModel().getStringAt(rowIndex, attribute);
	}

	@Override
	public final void addItems(Collection<Entity> items) {
		filterModel().addItems(items);
	}

	@Override
	public final void addItemsSorted(Collection<Entity> items) {
		filterModel().addItemsSorted(items);
	}

	@Override
	public final void addItemsAt(int index, Collection<Entity> items) {
		filterModel().addItemsAt(index, items);
	}

	@Override
	public final void addItemsAtSorted(int index, Collection<Entity> items) {
		filterModel().addItemsAtSorted(index, items);
	}

	@Override
	public final void addItem(Entity item) {
		filterModel().addItem(item);
	}

	@Override
	public final void addItemAt(int index, Entity item) {
		filterModel().addItemAt(index, item);
	}

	@Override
	public final void addItemSorted(Entity item) {
		filterModel().addItemSorted(item);
	}

	@Override
	public final void setItemAt(int index, Entity item) {
		filterModel().setItemAt(index, item);
	}

	@Override
	public final void removeItems(Collection<Entity> items) {
		filterModel().removeItems(items);
	}

	@Override
	public final void removeItem(Entity item) {
		filterModel().removeItem(item);
	}

	@Override
	public final Entity removeItemAt(int index) {
		return filterModel().removeItemAt(index);
	}

	@Override
	public final List<Entity> removeItems(int fromIndex, int toIndex) {
		return filterModel().removeItems(fromIndex, toIndex);
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
	public final <T> Collection<T> values(Attribute<?> attribute) {
		return filterModel().values(attribute);
	}

	@Override
	public final Class<?> getColumnClass(Attribute<?> attribute) {
		return filterModel().getColumnClass(attribute);
	}

	@Override
	public final <T> Collection<T> selectedValues(Attribute<?> attribute) {
		return filterModel().selectedValues(attribute);
	}

	@Override
	public final Value<RefreshStrategy> refreshStrategy() {
		return filterModel().refreshStrategy();
	}

	@Override
	public final void sort() {
		filterModel().sort();
	}

	@Override
	public final FilterTableSelectionModel<Entity> selection() {
		return filterModel().selection();
	}

	@Override
	public final TableConditionModel<Attribute<?>> conditionModel() {
		return filterModel().conditionModel();
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
	public final Columns<Entity, Attribute<?>> columns() {
		return filterModel().columns();
	}

	@Override
	public final Value<Comparator<Entity>> comparator() {
		return filterModel().comparator();
	}

	/**
	 * Returns a {@link java.awt.Color} instance from the given Object.
	 * {@link java.awt.Color} instances are returned as-is, but instances of
	 * {@link java.lang.String} are assumed to be in HEX format (f.ex: #ffff00" or #00ff00)
	 * and are parsed with {@link Color#decode(String)}. Colors parsed from Strings are cached.
	 * Override to support other representations.
	 * @param color the object representing the color.
	 * @return a {@link java.awt.Color} instance based on the given Object
	 * @throws IllegalArgumentException in case the representation is not supported
	 * @throws NullPointerException in case color is null
	 */
	protected Color toColor(Object color) {
		requireNonNull(color);
		if (color instanceof Color) {
			return (Color) color;
		}
		if (color instanceof String) {
			return COLOR_CACHE.computeIfAbsent((String) color, Color::decode);
		}

		throw new IllegalArgumentException("Unsupported Color representation: " + color);
	}

	@Override
	protected final FilterTableModel<Entity, Attribute<?>> filterModel() {
		return (FilterTableModel<Entity, Attribute<?>>) super.filterModel();
	}

	@Override
	protected final void onRowsUpdated(int fromIndex, int toIndex) {
		fireTableRowsUpdated(fromIndex, toIndex);
	}

	private void onTableModelEvent(TableModelEvent tableModelEvent) {
		//if the selected row is updated via the table model, refresh the one in the edit model
		if (tableModelEvent.getType() == TableModelEvent.UPDATE && tableModelEvent.getFirstRow() == selection().index()
						.get()
						.intValue()) {
			editModel().entity().set(selection().item().get());
		}
	}

	private static FilterTableModel<Entity, Attribute<?>> createFilterTableModel(EntityDefinition definition,
																																							 Supplier<? extends Collection<Entity>> items) {
		return FilterTableModel.builder(new EntityTableColumns(definition))
						.filterModelFactory(new EntityFilterModelFactory(definition))
						.items(items)
						.validator(new EntityItemValidator(definition.entityType()))
						.build();
	}

	private static EntityType entityType(Collection<Entity> entities) {
		if (requireNonNull(entities).isEmpty()) {
			throw new IllegalArgumentException("One or more entities is required to base a table model on");
		}

		return entities.iterator().next().entityType();
	}

	private static final class EntityTableColumns implements Columns<Entity, Attribute<?>> {

		private final EntityDefinition entityDefinition;
		private final List<Attribute<?>> identifiers;

		private EntityTableColumns(EntityDefinition entityDefinition) {
			this.entityDefinition = entityDefinition;
			this.identifiers = unmodifiableList(entityDefinition.attributes().definitions().stream()
							.filter(attributeDefinition -> !attributeDefinition.hidden())
							.map(AttributeDefinition::attribute)
							.collect(toList()));
		}

		@Override
		public List<Attribute<?>> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> columnClass(Attribute<?> identifier) {
			return requireNonNull(identifier).type().valueClass();
		}

		@Override
		public Object value(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).get(attribute);
		}

		@Override
		public String string(Entity entity, Attribute<?> attribute) {
			return requireNonNull(entity).string(attribute);
		}

		@Override
		public Comparator<?> comparator(Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				return entityDefinition.foreignKeys().referencedBy((ForeignKey) attribute).comparator();
			}

			return entityDefinition.attributes().definition(attribute).comparator();
		}
	}

	private static final class EntityFilterModelFactory implements ConditionModel.Factory<Attribute<?>> {

		private final EntityDefinition entityDefinition;

		private EntityFilterModelFactory(EntityDefinition entityDefinition) {
			this.entityDefinition = requireNonNull(entityDefinition);
		}

		@Override
		public Optional<ConditionModel<Attribute<?>, ?>> createConditionModel(Attribute<?> attribute) {
			if (!include(attribute)) {
				return Optional.empty();
			}

			AttributeDefinition<?> attributeDefinition = entityDefinition.attributes().definition(attribute);
			ConditionModel<?, ?> model;
			if (useStringCondition(attribute, attributeDefinition)) {
				model = ConditionModel.builder(attribute, String.class).build();

				return Optional.of((ConditionModel<Attribute<?>, ?>) model);
			}

			model = ConditionModel.builder(attribute, attribute.type().valueClass())
							.format(attributeDefinition.format())
							.dateTimePattern(attributeDefinition.dateTimePattern())
							.build();

			return Optional.of((ConditionModel<Attribute<?>, ?>) model);
		}

		private boolean include(Attribute<?> attribute) {
			AttributeDefinition<?> definition = entityDefinition.attributes().definition(attribute);
			if (definition.hidden()) {
				return false;
			}

			return !(attribute instanceof ForeignKey);
		}

		private static boolean useStringCondition(Attribute<?> attribute, AttributeDefinition<?> attributeDefinition) {
			return attribute.type().isEntity() || // entities
							!attributeDefinition.items().isEmpty() || // items
							!Comparable.class.isAssignableFrom(attribute.type().valueClass()); // non-comparables
		}
	}

	private static final class EntityItemValidator implements Predicate<Entity> {

		private final EntityType entityType;

		private EntityItemValidator(EntityType entityType) {
			this.entityType = requireNonNull(entityType);
		}

		@Override
		public boolean test(Entity entity) {
			return entity.entityType().equals(entityType);
		}
	}
}