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

import is.codion.common.model.component.tree.NodePath;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntityExport.ExportAttributes;
import is.codion.swing.common.model.component.tree.SwingFilterTreeModel;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static is.codion.common.model.component.tree.NodePath.nodePath;
import static java.util.stream.Collectors.toList;

/**
 * <p>The export configuration, the attributes to include and their order, with a tree model as its view.
 * <p>A node is identified by its path, the attribute definitions from the top level down to it, the same attribute
 * under two foreign keys being two nodes, each with its own configuration. The foreign keys make the tree infinite,
 * Customer, Invoice, Customer and so on, so nodes are loaded on demand.
 * <p>The configuration is kept apart from the tree model, which hides attributes via its children function and
 * excluded attributes via its predicate, both reading the configuration.
 */
final class EntityTableExportTreeModel {

	static final String ENTITY_TYPE_KEY = "entityType";

	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String SHOW_HIDDEN_KEY = "showHidden";

	private static final Comparator<AttributeDefinition<?>> CAPTION_COMPARATOR =
					(definition, other) -> definition.caption().compareToIgnoreCase(other.caption());

	private final EntityType entityType;
	private final Entities entities;
	private final SwingFilterTreeModel<AttributeDefinition<?>> treeModel;
	//the order of the children of each foreign key path, the root included, hidden attributes included
	private final Map<NodePath<AttributeDefinition<?>>, List<AttributeDefinition<?>>> order = new HashMap<>();
	//the paths of the included attributes
	private final Set<NodePath<AttributeDefinition<?>>> included = new HashSet<>();
	private final State showHidden = State.builder()
					.listener(this::showHiddenChanged)
					.build();
	private final Event<?> configuration = Event.event();

	EntityTableExportTreeModel(EntityType entityType, Entities entities) {
		this.entityType = entityType;
		this.entities = entities;
		this.treeModel = SwingFilterTreeModel.builder()
						.roots(() -> children(nodePath()))
						.children(this::children)
						.leaf(path -> !(path.item() instanceof ForeignKeyDefinition))
						.build();
		//in-memory, no need for a background thread
		this.treeModel.nodes().loader().async().set(false);
		this.treeModel.nodes().refresh();
	}

	SwingFilterTreeModel<AttributeDefinition<?>> treeModel() {
		return treeModel;
	}

	Observer<?> configuration() {
		return configuration.observer();
	}

	State showHidden() {
		return showHidden;
	}

	/**
	 * @param path the path
	 * @return true if the attribute is included
	 */
	boolean included(NodePath<AttributeDefinition<?>> path) {
		return included.contains(path);
	}

	/**
	 * @param path the path
	 * @return the number of included attributes below the given one
	 */
	int includedCount(NodePath<AttributeDefinition<?>> path) {
		int count = 0;
		for (NodePath<AttributeDefinition<?>> includedPath : included) {
			if (includedPath.depth() > path.depth() && path.contains(includedPath)) {
				count++;
			}
		}

		return count;
	}

	/**
	 * Includes the displayed attributes, the top level and the loaded foreign keys.
	 */
	void includeAll() {
		List<NodePath<AttributeDefinition<?>>> paths = new ArrayList<>();
		displayed(nodePath(), paths);
		included.addAll(paths);
		changed(paths);
		configuration.run();
	}

	/**
	 * Resets the configuration, excluding all attributes, in their default order, the excluded ones shown again.
	 */
	void includeNone() {
		reset();
		configuration.run();
	}

	/**
	 * Toggles the inclusion of the given attributes.
	 * @param paths the paths of the attributes
	 */
	void toggle(Collection<NodePath<AttributeDefinition<?>>> paths) {
		for (NodePath<AttributeDefinition<?>> path : paths) {
			if (!included.remove(path)) {
				included.add(path);
			}
		}
		changed(paths);
	}

	/**
	 * Includes the given attributes, or excludes them in case they are all included.
	 * @param paths the paths of the attributes
	 */
	void toggleAll(Collection<NodePath<AttributeDefinition<?>>> paths) {
		if (included.containsAll(paths)) {
			included.removeAll(paths);
		}
		else {
			included.addAll(paths);
		}
		changed(paths);
	}

	void hideExcluded() {
		treeModel.nodes().predicate().set(this::includesData);
		configuration.run();
	}

	void showExcluded() {
		treeModel.nodes().predicate().clear();
		configuration.run();
	}

	/**
	 * Moves the given sibling attributes one position up or down among the displayed attributes.
	 * @param paths the paths of the attributes, sharing a parent
	 * @param up true to move up
	 */
	void move(List<NodePath<AttributeDefinition<?>>> paths, boolean up) {
		NodePath<AttributeDefinition<?>> parent = paths.get(0).parent();
		List<NodePath<AttributeDefinition<?>>> displayed = new ArrayList<>(treeModel.nodes().children(parent));
		int[] indexes = paths.stream()
						.mapToInt(displayed::indexOf)
						.filter(index -> index >= 0)
						.sorted()
						.toArray();
		if (indexes.length == 0) {
			return;
		}
		if (up && indexes[0] > 0) {
			for (int index : indexes) {
				displayed.add(index - 1, displayed.remove(index));
			}
		}
		else if (!up && indexes[indexes.length - 1] < displayed.size() - 1) {
			for (int i = indexes.length - 1; i >= 0; i--) {
				displayed.add(indexes[i] + 1, displayed.remove(indexes[i]));
			}
		}
		order(parent, displayed);
	}

	/**
	 * Moves the given sibling attributes to the given index among the displayed attributes.
	 * @param paths the paths of the attributes, sharing a parent
	 * @param index the index to move them to, before removing them
	 */
	void move(List<NodePath<AttributeDefinition<?>>> paths, int index) {
		NodePath<AttributeDefinition<?>> parent = paths.get(0).parent();
		List<NodePath<AttributeDefinition<?>>> displayed = new ArrayList<>(treeModel.nodes().children(parent));
		//count the siblings before the drop index not themselves being moved, so dropping
		//a node onto its own position leaves it there
		int insertIndex = (int) displayed.subList(0, Math.min(index, displayed.size())).stream()
						.filter(path -> !paths.contains(path))
						.count();
		displayed.removeAll(paths);
		displayed.addAll(insertIndex, paths);
		order(parent, displayed);
	}

	void applyConfiguration(JSONObject json) {
		showHidden.set(json.has(SHOW_HIDDEN_KEY) && json.getBoolean(SHOW_HIDDEN_KEY));
		included.clear();
		order.clear();
		apply(nodePath(), json);
		treeModel.nodes().refresh();
		hideExcluded();
	}

	JSONObject toJson() {
		JSONObject json = toJson(nodePath());
		json.put(ENTITY_TYPE_KEY, entityType.name());
		json.put(SHOW_HIDDEN_KEY, showHidden.is());

		return json;
	}

	ExportAttributes attributes(ExportAttributes.Builder attributes) {
		populate(nodePath(), attributes);

		return attributes.build();
	}

	/**
	 * @return the paths of the foreign keys with included attributes below them, for expanding
	 */
	Collection<NodePath<AttributeDefinition<?>>> includedParents() {
		Set<NodePath<AttributeDefinition<?>>> parents = new LinkedHashSet<>();
		for (NodePath<AttributeDefinition<?>> path : included) {
			NodePath<AttributeDefinition<?>> parent = path.parent();
			while (!parent.root()) {
				parents.add(parent);
				parent = parent.parent();
			}
		}

		return parents;
	}

	private void showHiddenChanged() {
		//keeps the expansion and selection, nodes being kept by attribute
		treeModel.nodes().refresh();
	}

	private void reset() {
		included.clear();
		order.clear();
		//all being excluded now, the excluded ones are shown again
		treeModel.nodes().predicate().clear();
		treeModel.nodes().refresh();
	}

	/**
	 * The children of a node, in the configured order, the hidden attributes left out unless shown or included.
	 */
	private List<AttributeDefinition<?>> children(NodePath<AttributeDefinition<?>> path) {
		return order(path).stream()
						.filter(definition -> showHidden.is() || !definition.hidden() || includesData(path.child(definition)))
						.collect(toList());
	}

	private List<AttributeDefinition<?>> order(NodePath<AttributeDefinition<?>> path) {
		return order.computeIfAbsent(path, this::defaultOrder);
	}

	/**
	 * Orders the displayed attributes, the ones not displayed following.
	 */
	private void order(NodePath<AttributeDefinition<?>> parent, List<NodePath<AttributeDefinition<?>>> displayed) {
		List<AttributeDefinition<?>> ordered = displayed.stream()
						.map(NodePath::item)
						.collect(toList());
		order(parent).stream()
						.filter(definition -> !ordered.contains(definition))
						.forEach(ordered::add);
		order.put(parent, ordered);
		treeModel.nodes().refresh(parent);
	}

	private List<AttributeDefinition<?>> defaultOrder(NodePath<AttributeDefinition<?>> path) {
		EntityType type = path.root() ? entityType : ((ForeignKeyDefinition) path.item()).attribute().referencedType();

		return entities.definition(type).attributes().definitions().stream()
						.filter(EntityTableExportTreeModel::selectedColumnOrAttribute)
						.sorted(CAPTION_COMPARATOR)
						.collect(toList());
	}

	private boolean includesData(NodePath<AttributeDefinition<?>> path) {
		return included.contains(path) || includedCount(path) > 0;
	}

	private void displayed(NodePath<AttributeDefinition<?>> parent, List<NodePath<AttributeDefinition<?>>> paths) {
		for (NodePath<AttributeDefinition<?>> child : treeModel.nodes().children(parent)) {
			paths.add(child);
			if (treeModel.nodes().loaded(child)) {
				displayed(child, paths);
			}
		}
	}

	/**
	 * Notifies that the given nodes and their ancestors changed, an inclusion changing the counts of the ancestors.
	 */
	private void changed(Collection<NodePath<AttributeDefinition<?>>> paths) {
		Set<NodePath<AttributeDefinition<?>>> changed = new LinkedHashSet<>();
		for (NodePath<AttributeDefinition<?>> path : paths) {
			NodePath<AttributeDefinition<?>> node = path;
			while (!node.root()) {
				changed.add(node);
				node = node.parent();
			}
		}
		treeModel.fireNodesChanged(changed);
		if (!treeModel.nodes().predicate().isNull()) {
			treeModel.nodes().filter();
		}
	}

	private JSONObject toJson(NodePath<AttributeDefinition<?>> parent) {
		JSONArray attributes = new JSONArray();
		for (AttributeDefinition<?> definition : order(parent)) {
			NodePath<AttributeDefinition<?>> path = parent.child(definition);
			String attributeName = definition.attribute().name();
			if (definition instanceof ForeignKeyDefinition) {
				JSONObject foreignKeyAttributes = includedCount(path) > 0 ? toJson(path) : new JSONObject();
				if (included.contains(path)) {
					attributes.put(attributeName);
				}
				if (!foreignKeyAttributes.isEmpty()) {
					attributes.put(new JSONObject().put(attributeName, foreignKeyAttributes));
				}
			}
			else if (included.contains(path)) {
				attributes.put(attributeName);
			}
		}
		JSONObject result = new JSONObject();
		if (!attributes.isEmpty()) {
			result.put(ATTRIBUTES_KEY, attributes);
		}

		return result;
	}

	/**
	 * Applies the given configuration below the given path, the attributes it names ordered first, in the order it
	 * names them, the rest following in their default order. Attributes no longer present are ignored.
	 */
	private void apply(NodePath<AttributeDefinition<?>> parent, JSONObject json) {
		if (!json.has(ATTRIBUTES_KEY)) {
			return;
		}
		List<AttributeDefinition<?>> defaultOrder = order(parent);
		List<AttributeDefinition<?>> ordered = new ArrayList<>(defaultOrder.size());
		for (Object attribute : json.getJSONArray(ATTRIBUTES_KEY)) {
			String attributeName = attributeName(attribute);
			for (AttributeDefinition<?> definition : defaultOrder) {
				if (definition.attribute().name().equals(attributeName)) {
					if (!ordered.contains(definition)) {
						ordered.add(definition);
					}
					NodePath<AttributeDefinition<?>> path = parent.child(definition);
					if (attribute instanceof String) {
						included.add(path);
					}
					else {
						apply(path, ((JSONObject) attribute).getJSONObject(attributeName));
					}
				}
			}
		}
		defaultOrder.stream()
						.filter(definition -> !ordered.contains(definition))
						.forEach(ordered::add);
		order.put(parent, ordered);
	}

	private void populate(NodePath<AttributeDefinition<?>> parent, ExportAttributes.Builder attributes) {
		List<Attribute<?>> include = new ArrayList<>();
		List<Attribute<?>> attributeOrder = new ArrayList<>();
		for (AttributeDefinition<?> definition : order(parent)) {
			NodePath<AttributeDefinition<?>> path = parent.child(definition);
			if (included.contains(path)) {
				include.add(definition.attribute());
			}
			if (includesData(path)) {
				attributeOrder.add(definition.attribute());
			}
		}
		attributes.include(include).order(attributeOrder);
		for (AttributeDefinition<?> definition : order(parent)) {
			NodePath<AttributeDefinition<?>> path = parent.child(definition);
			if (definition instanceof ForeignKeyDefinition && includedCount(path) > 0) {
				attributes.attributes(((ForeignKeyDefinition) definition).attribute(), foreignKeyAttributes ->
								populate(path, foreignKeyAttributes));
			}
		}
	}

	private static boolean selectedColumnOrAttribute(AttributeDefinition<?> definition) {
		if (definition instanceof ColumnDefinition) {
			return ((ColumnDefinition<?>) definition).selected();
		}

		return true;
	}

	private static String attributeName(Object attribute) {
		if (attribute instanceof String) {
			return (String) attribute;
		}

		return ((JSONObject) attribute).keys().next();
	}
}
