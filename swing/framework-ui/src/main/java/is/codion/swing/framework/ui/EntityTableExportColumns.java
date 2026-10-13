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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntityExport.Attributes;
import is.codion.swing.common.model.component.table.SwingFilterTableModel;
import is.codion.swing.common.model.component.table.SwingFilterTableModel.RowEditor;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

/**
 * <p>The export columns, the attributes to export in the order of the export, with their headers, a table model as
 * their view, and the attributes to choose them from, with a tree model as their view.
 * <p>A column is identified by its path, the attribute definitions from the top level down to it, the same attribute
 * under two foreign keys being two columns. The foreign keys make the tree infinite, Customer, Invoice, Customer and
 * so on, so its nodes are loaded on demand, while the columns may be below nodes not yet loaded.
 * <p>A column has the default header, the captions on its path, unless one is set, see
 * {@link #header(NodePath, String)}.
 */
final class EntityTableExportColumns {

	/**
	 * The table columns of the export columns.
	 */
	enum ExportColumn {
		/**
		 * The captions on the path of a column, separated
		 */
		ATTRIBUTE,
		/**
		 * The header of a column, editable
		 */
		HEADER
	}

	static final String ENTITY_TYPE_KEY = "entityType";

	private static final String COLUMNS_KEY = "columns";
	//the attributes on the path of a column, and in the first configuration format nested by foreign key, see flatten()
	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String HEADER_KEY = "header";
	private static final String PATH_SEPARATOR = " › ";
	//as the default header of EntityExport
	private static final String HEADER_SEPARATOR = " ";

	private static final Comparator<AttributeDefinition<?>> CAPTION_COMPARATOR =
					(definition, other) -> definition.caption().compareToIgnoreCase(other.caption());

	private final EntityType entityType;
	private final Entities entities;
	private final Supplier<List<Attribute<?>>> defaults;
	private final SwingFilterTableModel<NodePath<AttributeDefinition<?>>, ExportColumn> columns;
	//the headers set, the others being the default ones
	private final Map<NodePath<AttributeDefinition<?>>, String> headers = new HashMap<>();
	private final Event<?> changed = Event.event();
	private final SwingFilterTreeModel<AttributeDefinition<?>> attributes;
	//the attribute definitions available for export by entity type, ordered by caption
	private final Map<EntityType, List<AttributeDefinition<?>>> definitions = new HashMap<>();
	private final State showHidden = State.builder()
					.listener(this::showHiddenChanged)
					.build();
	private final Value<String> search = Value.nonNull("");
	private final State foreignKeySelected = State.state();

	//the columns as they were when last changed, for finding the nodes to repaint
	private Set<NodePath<AttributeDefinition<?>>> previous = emptySet();

	/**
	 * @param entityType the entity type being exported
	 * @param entities the entities
	 * @param defaults supplies the default columns, see {@link #reset()}
	 */
	EntityTableExportColumns(EntityType entityType, Entities entities, Supplier<List<Attribute<?>>> defaults) {
		this.entityType = entityType;
		this.entities = entities;
		this.defaults = defaults;
		this.columns = SwingFilterTableModel.builder()
						.columns(new ExportColumns())
						.items(this::defaultColumns)
						.rowEditor(table -> new HeaderEditor())
						.build();
		//in-memory, no need for a background thread
		this.columns.items().refresher().async().set(false);
		this.columns.items().included().addListener(this::columnsChanged);
		this.attributes = SwingFilterTreeModel.builder()
						.roots(() -> children(nodePath()))
						.children(this::children)
						.leaf(path -> !(path.item() instanceof ForeignKeyDefinition))
						.onSelectedItem(this::updateForeignKeySelected)
						.build();
		this.attributes.nodes().loader().async().set(false);
		this.attributes.nodes().refresh();
		this.search.addListener(this::searchChanged);
	}

	/**
	 * @return the columns, in the order of the export
	 */
	SwingFilterTableModel<NodePath<AttributeDefinition<?>>, ExportColumn> table() {
		return columns;
	}

	/**
	 * @return an observer notified when the columns or their headers change
	 */
	Observer<?> changed() {
		return changed.observer();
	}

	/**
	 * @param path the path of a column
	 * @return the header of the given column, the one set or the default one, the captions on its path
	 */
	String header(NodePath<AttributeDefinition<?>> path) {
		String header = headers.get(path);

		return header == null ? captions(path, HEADER_SEPARATOR) : header;
	}

	/**
	 * Sets the header of the given column, trimmed, a null or blank one, or the default one, resetting it to the
	 * default.
	 * @param path the path of a column
	 * @param header the header
	 * @throws IllegalArgumentException in case the given path is not a column
	 */
	void header(NodePath<AttributeDefinition<?>> path, @Nullable String header) {
		if (!included(path)) {
			throw new IllegalArgumentException("Not a column: " + path);
		}
		String headerSet = headerSet(path, header);
		String previous = headerSet == null ? headers.remove(path) : headers.put(path, headerSet);
		if (!Objects.equals(previous, headerSet)) {
			int index = columns.items().included().indexOf(path);
			columns.fireTableRowsUpdated(index, index);
			changed.run();
		}
	}

	/**
	 * @param path the path of a column
	 * @return true if the given column has the default header
	 */
	boolean defaultHeader(NodePath<AttributeDefinition<?>> path) {
		return !headers.containsKey(path);
	}

	/**
	 * @return the attributes to choose the columns from
	 */
	SwingFilterTreeModel<AttributeDefinition<?>> tree() {
		return attributes;
	}

	/**
	 * @return the {@link State} controlling whether hidden attributes are shown, included ones always being shown
	 */
	State showHidden() {
		return showHidden;
	}

	/**
	 * @return the {@link Value} controlling the search, the attributes shown being the ones whose caption, or the
	 * caption of a foreign key above them, contains it, ignoring case
	 */
	Value<String> search() {
		return search;
	}

	/**
	 * @return an observable state enabled when a single foreign key is selected
	 */
	ObservableState foreignKeySelected() {
		return foreignKeySelected.observable();
	}

	/**
	 * @param path the path
	 * @return true if the attribute is a column
	 */
	boolean included(NodePath<AttributeDefinition<?>> path) {
		return columns.items().contains(path);
	}

	/**
	 * @param path the path
	 * @return the number of columns below the given path
	 */
	int includedCount(NodePath<AttributeDefinition<?>> path) {
		return (int) columns.items().get().stream()
						.filter(column -> column.depth() > path.depth() && path.contains(column))
						.count();
	}

	/**
	 * Removes the given attributes in case they are all columns, otherwise adds the ones which are not, in order,
	 * selecting them. Each is added after the last column from the same foreign key, the foreign key itself included,
	 * otherwise after the last one further below it, otherwise the same for the foreign key above that one and so on,
	 * otherwise last.
	 * @param paths the paths of the attributes
	 */
	void toggle(Collection<NodePath<AttributeDefinition<?>>> paths) {
		if (paths.isEmpty()) {
			return;
		}
		if (paths.stream().allMatch(this::included)) {
			remove(paths);
		}
		else {
			List<NodePath<AttributeDefinition<?>>> added = paths.stream()
							.filter(path -> !included(path))
							.collect(toList());
			List<NodePath<AttributeDefinition<?>>> items = new ArrayList<>(columns.items().get());
			added.forEach(path -> items.add(insertIndex(items, path), path));
			columns.items().set(items);
			columns.selection().items().set(added);
		}
	}

	/**
	 * Toggles the attributes shown below the given foreign keys, see {@link #toggle(Collection)}, whether or not their
	 * nodes have been loaded.
	 * @param paths the paths of the foreign keys, other attributes being ignored
	 */
	void toggleChildren(Collection<NodePath<AttributeDefinition<?>>> paths) {
		toggle(paths.stream()
						.filter(path -> path.item() instanceof ForeignKeyDefinition)
						.flatMap(path -> children(path).stream()
										.map(path::child))
						.collect(toList()));
	}

	/**
	 * Inserts the given attributes at the given index, moving the ones which are columns already.
	 * @param paths the paths of the attributes
	 * @param index the index to insert them at, before any of them are moved
	 */
	void insert(List<NodePath<AttributeDefinition<?>>> paths, int index) {
		List<NodePath<AttributeDefinition<?>>> items = new ArrayList<>(columns.items().get());
		//count the columns before the index not themselves being moved, so inserting
		//a column at its own position leaves it there
		int insertIndex = (int) items.subList(0, Math.min(index, items.size())).stream()
						.filter(path -> !paths.contains(path))
						.count();
		items.removeAll(paths);
		items.addAll(insertIndex, paths);
		columns.items().set(items);
		columns.selection().items().set(paths);
	}

	/**
	 * Removes the given columns.
	 * @param paths the paths of the columns
	 */
	void remove(Collection<NodePath<AttributeDefinition<?>>> paths) {
		columns.items().remove(paths);
	}

	/**
	 * Moves the given columns one position up or down.
	 * @param paths the paths of the columns
	 * @param up true to move up
	 */
	void move(List<NodePath<AttributeDefinition<?>>> paths, boolean up) {
		List<NodePath<AttributeDefinition<?>>> items = new ArrayList<>(columns.items().get());
		int[] indexes = paths.stream()
						.mapToInt(items::indexOf)
						.filter(index -> index >= 0)
						.sorted()
						.toArray();
		if (indexes.length == 0) {
			return;
		}
		if (up && indexes[0] > 0) {
			for (int index : indexes) {
				items.add(index - 1, items.remove(index));
			}
		}
		else if (!up && indexes[indexes.length - 1] < items.size() - 1) {
			for (int i = indexes.length - 1; i >= 0; i--) {
				items.add(indexes[i] + 1, items.remove(indexes[i]));
			}
		}
		columns.items().set(items);
	}

	/**
	 * Removes all columns.
	 */
	void clear() {
		columns.items().clear();
	}

	/**
	 * Resets the columns to the default ones, the ones currently supplied.
	 */
	void reset() {
		columns.items().refresh();
	}

	/**
	 * Expands the foreign keys above the columns, collapsing all others, the tree showing every column.
	 */
	void expand() {
		Set<NodePath<AttributeDefinition<?>>> foreignKeys = new LinkedHashSet<>();
		columns.items().get().forEach(column -> {
			for (NodePath<AttributeDefinition<?>> parent = column.parent(); !parent.root(); parent = parent.parent()) {
				foreignKeys.add(parent);
			}
		});
		attributes.expansion().set(foreignKeys);
	}

	/**
	 * Applies the given configuration, the columns it names which exist, with their headers, any other column fields
	 * ignored. Reads the first, nested configuration format as well, as the columns it exported.
	 * @param json the configuration
	 * @throws org.json.JSONException in case a column is not an object naming the attributes on its path, or its
	 * header is not a string
	 */
	void apply(JSONObject json) {
		List<NodePath<AttributeDefinition<?>>> paths = new ArrayList<>();
		Map<NodePath<AttributeDefinition<?>>, String> headersSet = new HashMap<>();
		if (json.has(COLUMNS_KEY)) {
			JSONArray array = json.getJSONArray(COLUMNS_KEY);
			for (int i = 0; i < array.length(); i++) {
				JSONObject column = array.getJSONObject(i);
				String header = column.has(HEADER_KEY) ? column.getString(HEADER_KEY) : null;
				path(column.getJSONArray(ATTRIBUTES_KEY)).ifPresent(path -> {
					if (!paths.contains(path)) {
						paths.add(path);
						String headerSet = headerSet(path, header);
						if (headerSet != null) {
							headersSet.put(path, headerSet);
						}
					}
				});
			}
		}
		else {
			flatten(nodePath(), json, paths);
		}
		headers.clear();
		headers.putAll(headersSet);
		columns.items().set(paths);
		//in case only the headers changed
		int count = columns.items().included().size();
		if (count > 0) {
			columns.fireTableRowsUpdated(0, count - 1);
		}
		changed.run();
	}

	/**
	 * @return the configuration, the entity type and the columns, each column an object with the names of the
	 * attributes on its path, and its header, in case one is set
	 */
	JSONObject toJson() {
		JSONArray array = new JSONArray();
		columns.items().get().forEach(path -> {
			JSONObject column = new JSONObject()
							.put(ATTRIBUTES_KEY, new JSONArray(path.items().stream()
											.map(definition -> definition.attribute().name())
											.collect(toList())));
			String header = headers.get(path);
			if (header != null) {
				column.put(HEADER_KEY, header);
			}
			array.put(column);
		});

		return new JSONObject()
						.put(ENTITY_TYPE_KEY, entityType.name())
						.put(COLUMNS_KEY, array);
	}

	/**
	 * Adds the columns to the given export attributes, in order, with their headers.
	 * @param attributes the attributes to add to
	 */
	void attributes(Attributes attributes) {
		columns.items().get().forEach(path -> attributes.add(header(path), path.items().stream()
						.map(AttributeDefinition::attribute)
						.collect(toList())));
	}

	private void updateForeignKeySelected(AttributeDefinition<?> attribute) {
		foreignKeySelected.set(attribute instanceof ForeignKeyDefinition && attributes.selection().single().is());
	}

	private Collection<NodePath<AttributeDefinition<?>>> defaultColumns() {
		NodePath<AttributeDefinition<?>> root = nodePath();
		List<AttributeDefinition<?>> available = definitions(root);

		return defaults.get().stream()
						.map(attribute -> available.stream()
										.filter(definition -> definition.attribute().equals(attribute))
										.findFirst())
						.filter(Optional::isPresent)
						.map(definition -> root.child(definition.get()))
						.collect(toList());
	}

	/**
	 * Drops the headers of the columns removed, repaints the nodes of the columns added or removed, along with their
	 * ancestors, the counts of which change, and refreshes their parents in case they are hidden, so they are shown
	 * while columns, and hidden again.
	 */
	private void columnsChanged() {
		Set<NodePath<AttributeDefinition<?>>> current = new HashSet<>(columns.items().get());
		headers.keySet().retainAll(current);
		Set<NodePath<AttributeDefinition<?>>> addedOrRemoved = new HashSet<>(current);
		addedOrRemoved.addAll(previous);
		Set<NodePath<AttributeDefinition<?>>> retained = new HashSet<>(current);
		retained.retainAll(previous);
		addedOrRemoved.removeAll(retained);
		previous = current;
		Set<NodePath<AttributeDefinition<?>>> nodes = new LinkedHashSet<>();
		Set<NodePath<AttributeDefinition<?>>> refresh = new LinkedHashSet<>();
		for (NodePath<AttributeDefinition<?>> path : addedOrRemoved) {
			if (!showHidden.is() && path.item().hidden()) {
				refresh.add(path.parent());
			}
			NodePath<AttributeDefinition<?>> node = path;
			while (!node.root()) {
				nodes.add(node);
				node = node.parent();
			}
		}
		attributes.fireNodesChanged(nodes);
		refresh.stream()
						.filter(parent -> parent.root() || attributes.nodes().loaded(parent))
						.forEach(attributes.nodes()::refresh);
		changed.run();
	}

	private void showHiddenChanged() {
		//keeps the expansion and selection, nodes being kept by attribute
		attributes.nodes().refresh();
	}

	private void searchChanged() {
		String text = search.getOrThrow().trim().toLowerCase(Locale.ROOT);
		if (text.isEmpty()) {
			attributes.nodes().predicate().clear();
		}
		else {
			attributes.nodes().predicate().set(path -> path.items().stream()
							.anyMatch(definition -> definition.caption().toLowerCase(Locale.ROOT).contains(text)));
		}
	}

	/**
	 * The attributes below a node, the hidden ones left out unless shown, or columns or above columns.
	 */
	private List<AttributeDefinition<?>> children(NodePath<AttributeDefinition<?>> path) {
		return definitions(path).stream()
						.filter(definition -> showHidden.is() || !definition.hidden() || includesData(path.child(definition)))
						.collect(toList());
	}

	private boolean includesData(NodePath<AttributeDefinition<?>> path) {
		return included(path) || includedCount(path) > 0;
	}

	/**
	 * @return the attributes available for export below the given path, ordered by caption
	 */
	private List<AttributeDefinition<?>> definitions(NodePath<AttributeDefinition<?>> path) {
		EntityType type = path.root() ? entityType : ((ForeignKeyDefinition) path.item()).attribute().referencedType();

		return definitions.computeIfAbsent(type, k -> entities.definition(type).attributes().definitions().stream()
						.filter(EntityTableExportColumns::selectedColumnOrAttribute)
						.sorted(CAPTION_COMPARATOR)
						.collect(toList()));
	}

	/**
	 * @return the path named by the given attribute names, an empty Optional in case an attribute no longer exists
	 */
	private Optional<NodePath<AttributeDefinition<?>>> path(JSONArray names) {
		NodePath<AttributeDefinition<?>> path = nodePath();
		for (Object name : names) {
			Optional<NodePath<AttributeDefinition<?>>> child = child(path, String.valueOf(name));
			if (!child.isPresent()) {
				return Optional.empty();
			}
			path = child.get();
		}

		return path.root() ? Optional.empty() : Optional.of(path);
	}

	private Optional<NodePath<AttributeDefinition<?>>> child(NodePath<AttributeDefinition<?>> parent, String attributeName) {
		if (!parent.root() && !(parent.item() instanceof ForeignKeyDefinition)) {
			return Optional.empty();
		}

		return definitions(parent).stream()
						.filter(definition -> definition.attribute().name().equals(attributeName))
						.findFirst()
						.map(parent::child);
	}

	/**
	 * <p>Adds the columns of the first configuration format below the given path, in the order they were exported.
	 * <p>The attributes of each foreign key were configured separately, each one named either on its own, when
	 * included, or with the configuration of the attributes below it, in the order of the export, by first mention.
	 * A foreign key preceded the attributes below it.
	 */
	private void flatten(NodePath<AttributeDefinition<?>> parent, JSONObject json,
											 List<NodePath<AttributeDefinition<?>>> paths) {
		if (!json.has(ATTRIBUTES_KEY)) {
			return;
		}
		Map<String, Boolean> included = new LinkedHashMap<>();
		Map<String, JSONObject> below = new HashMap<>();
		for (Object attribute : json.getJSONArray(ATTRIBUTES_KEY)) {
			if (attribute instanceof String) {
				included.put((String) attribute, true);
			}
			else {
				JSONObject foreignKey = (JSONObject) attribute;
				String attributeName = foreignKey.keys().next();
				included.putIfAbsent(attributeName, false);
				below.put(attributeName, foreignKey.getJSONObject(attributeName));
			}
		}
		included.forEach((attributeName, include) -> child(parent, attributeName).ifPresent(path -> {
			if (include) {
				paths.add(path);
			}
			if (below.containsKey(attributeName)) {
				flatten(path, below.get(attributeName), paths);
			}
		}));
	}

	/**
	 * @return the index after the last of the given columns from the foreign key above the given path, the foreign key
	 * itself included, otherwise after the last one further below it, otherwise the same for the foreign key above that
	 * one and so on, the number of columns in case there is none
	 */
	private static int insertIndex(List<NodePath<AttributeDefinition<?>>> columns, NodePath<AttributeDefinition<?>> path) {
		for (NodePath<AttributeDefinition<?>> foreignKey = path.parent(); !foreignKey.root(); foreignKey = foreignKey.parent()) {
			NodePath<AttributeDefinition<?>> parent = foreignKey;
			int index = lastIndex(columns, column -> column.equals(parent) || column.parent().equals(parent));
			if (index == -1) {
				index = lastIndex(columns, parent::contains);
			}
			if (index != -1) {
				return index + 1;
			}
		}

		return columns.size();
	}

	private static int lastIndex(List<NodePath<AttributeDefinition<?>>> columns,
															 Predicate<NodePath<AttributeDefinition<?>>> predicate) {
		for (int index = columns.size() - 1; index >= 0; index--) {
			if (predicate.test(columns.get(index))) {
				return index;
			}
		}

		return -1;
	}

	private static boolean selectedColumnOrAttribute(AttributeDefinition<?> definition) {
		if (definition instanceof ColumnDefinition) {
			return ((ColumnDefinition<?>) definition).selected();
		}

		return true;
	}

	/**
	 * @return the given header trimmed, null in case it is null, blank or the default header of the given column
	 */
	private static @Nullable String headerSet(NodePath<AttributeDefinition<?>> path, @Nullable String header) {
		String trimmed = header == null ? "" : header.trim();

		return trimmed.isEmpty() || trimmed.equals(captions(path, HEADER_SEPARATOR)) ? null : trimmed;
	}

	private static String captions(NodePath<AttributeDefinition<?>> path, String separator) {
		return path.items().stream()
						.map(AttributeDefinition::caption)
						.collect(joining(separator));
	}

	private final class ExportColumns implements TableColumns<NodePath<AttributeDefinition<?>>, ExportColumn> {

		private final List<ExportColumn> identifiers = unmodifiableList(asList(ExportColumn.values()));

		@Override
		public List<ExportColumn> identifiers() {
			return identifiers;
		}

		@Override
		public Class<?> type(ExportColumn identifier) {
			return String.class;
		}

		@Override
		public Object value(NodePath<AttributeDefinition<?>> path, ExportColumn identifier) {
			switch (identifier) {
				case ATTRIBUTE:
					return captions(path, PATH_SEPARATOR);
				case HEADER:
					return header(path);
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}

	private final class HeaderEditor implements RowEditor<NodePath<AttributeDefinition<?>>, ExportColumn> {

		@Override
		public boolean editable(NodePath<AttributeDefinition<?>> path, ExportColumn identifier) {
			return identifier == ExportColumn.HEADER;
		}

		@Override
		public void set(@Nullable Object value, int rowIndex, NodePath<AttributeDefinition<?>> path, ExportColumn identifier) {
			if (identifier != ExportColumn.HEADER) {
				throw new IllegalStateException("Not editable: " + identifier);
			}
			header(path, (String) value);
		}
	}
}
