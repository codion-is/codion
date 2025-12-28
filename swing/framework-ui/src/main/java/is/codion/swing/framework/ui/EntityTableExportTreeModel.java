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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntityExport.ExportAttributes;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

final class EntityTableExportTreeModel extends DefaultTreeModel {

	static final String ENTITY_TYPE_KEY = "entityType";

	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String SHOW_HIDDEN_KEY = "showHidden";

	private static final AttributeCaptionComparator CAPTION_COMPARATOR = new AttributeCaptionComparator();
	private static final AttributeNodeComparator NODE_COMPARATOR = new AttributeNodeComparator();

	private final Entities entities;
	private final State showHidden = State.builder()
					.listener(this::showHiddenChanged)
					.build();
	private final Event<?> configuration = Event.event();

	EntityTableExportTreeModel(EntityType entityType, Entities entities) {
		super(null);
		this.entities = entities;
		EntityNode rootNode = new EntityNode(entityType, this);
		rootNode.populate();
		setRoot(rootNode);
	}

	@Override
	public EntityNode getRoot() {
		return (EntityNode) super.getRoot();
	}

	Observer<?> configuration() {
		return configuration.observer();
	}

	State showHidden() {
		return showHidden;
	}

	void includeAll() {
		getRoot().populate();
		include(Collections.list(getRoot().children()), true);
		configuration.run();
	}

	void includeNone() {
		refresh();
		configuration.run();
	}

	void applyConfiguration(JSONObject json) {
		showHidden.set(json.has(SHOW_HIDDEN_KEY) && json.getBoolean(SHOW_HIDDEN_KEY));
		refresh();
		getRoot().apply(json);
		hideExcluded();
		configuration.run();
	}

	JSONObject toJson() {
		JSONObject jsonObject = getRoot().toJson();
		jsonObject.put(ENTITY_TYPE_KEY, getRoot().entityType.name());
		jsonObject.put(SHOW_HIDDEN_KEY, showHidden().is());

		return jsonObject;
	}

	ExportAttributes attributes(ExportAttributes.Builder attributes) {
		getRoot().populate(attributes);

		return attributes.build();
	}

	void hideExcluded() {
		getRoot().reload(true);
		nodeStructureChanged(getRoot());
		configuration.run();
	}

	void showExcluded() {
		getRoot().populate();
		getRoot().reload(false);
		nodeStructureChanged(getRoot());
		configuration.run();
	}

	private void showHiddenChanged() {
		getRoot().reload(false);
		nodeStructureChanged(getRoot());
		configuration.run();
	}

	private void refresh() {
		getRoot().removeAllChildren();
		getRoot().populate();
		nodeStructureChanged(getRoot());
	}

	private static void include(List<TreeNode> nodes, boolean include) {
		for (TreeNode node : nodes) {
			AttributeNode attributeNode = (AttributeNode) node;
			attributeNode.include().set(include);
			if (attributeNode instanceof MutableForeignKeyNode) {
				include(Collections.list(((MutableForeignKeyNode) attributeNode).children()), include);
			}
		}
	}

	interface AttributeNode extends MutableTreeNode {

		AttributeDefinition<?> definition();

		Attribute<?> attribute();

		boolean hidden();

		State include();
	}

	static class EntityNode extends DefaultMutableTreeNode {

		private final EntityType entityType;
		private final EntityTableExportTreeModel treeModel;

		private EntityNode(EntityType entityType, EntityTableExportTreeModel treeModel) {
			this.entityType = entityType;
			this.treeModel = treeModel;
		}

		final void populate() {
			if (getChildCount() == 0) {
				attributeNodes().forEach(this::add);
				treeModel.nodeStructureChanged(this);
			}
		}

		final void move(List<TreeNode> nodes, boolean up) {
			int[] indexes = nodes.stream()
							.mapToInt(children::indexOf)
							.sorted()
							.toArray();
			if (up) {
				moveUp(indexes);
			}
			else {
				moveDown(indexes);
			}
			treeModel.nodeStructureChanged(this);
		}

		protected void populate(ExportAttributes.Builder attributes) {
			attributes.include(include()).order(order());
			Collections.list(children()).stream()
							.filter(MutableForeignKeyNode.class::isInstance)
							.map(MutableForeignKeyNode.class::cast)
							.forEach(foreignKeyNode ->
											attributes.attributes(foreignKeyNode.attribute(), foreignKeyNode::populate));
		}

		private final JSONObject toJson() {
			Enumeration<TreeNode> nodes = children();
			JSONArray attributes = new JSONArray();
			while (nodes.hasMoreElements()) {
				AttributeNode node = (AttributeNode) nodes.nextElement();
				String attributeName = node.attribute().name();
				if (node.getChildCount() > 0) {
					JSONObject fkChildren = ((EntityNode) node).toJson();
					if (!node.include().is() && fkChildren.isEmpty()) {
						continue;
					}
					if (node.include().is()) {// If FK itself is included, add its name to the attributes array
						attributes.put(attributeName);
					}
					if (!fkChildren.isEmpty()) {// If FK has included children, add the structure object
						JSONObject fkObject = new JSONObject();
						fkObject.put(attributeName, fkChildren);
						attributes.put(fkObject);
					}
				}
				else if (node.include().is()) {// Simple attributes are just strings
					attributes.put(attributeName);
				}
			}

			JSONObject result = new JSONObject();
			if (!attributes.isEmpty()) {
				result.put(ATTRIBUTES_KEY, attributes);
			}

			return result;
		}

		private void apply(JSONObject json) {
			if (!json.has(ATTRIBUTES_KEY)) {
				return;
			}
			populate();
			Enumeration<TreeNode> childNodes = children();
			Map<String, DefaultMutableTreeNode> children = new HashMap<>();
			while (childNodes.hasMoreElements()) {
				DefaultMutableTreeNode child = (DefaultMutableTreeNode) childNodes.nextElement();
				children.put(((AttributeNode) child).attribute().name(), child);
			}
			for (Object jsonAttribute : json.getJSONArray(ATTRIBUTES_KEY)) {
				String attributeName = attributeName(jsonAttribute);
				DefaultMutableTreeNode child = children.get(attributeName);
				if (child != null) {// missing attribute, removed or hidden f.ex.
					int index = getIndex(child);
					if (index >= 0) {
						remove(child);
					}
					add(child);
					treeModel.nodeStructureChanged(this);
					if (jsonAttribute instanceof String) {
						((AttributeNode) child).include().set(true);
					}
					else {
						((EntityNode) child).apply(((JSONObject) jsonAttribute).getJSONObject(attributeName));
					}
				}
			}
			List<AttributeNode> sorted = Collections.list(children()).stream()
							.map(AttributeNode.class::cast)
							.sorted(NODE_COMPARATOR)
							.collect(toList());
			removeAllChildren();
			sorted.forEach(this::add);
			treeModel.nodeStructureChanged(this);
		}

		private void reload(boolean hideExcluded) {
			if (getChildCount() > 0) {
				List<AttributeNode> nodes = Collections.list(children()).stream()
								.map(AttributeNode.class::cast)
								.collect(toList());
				Map<AttributeDefinition<?>, AttributeNode> nodeMap = Collections.list(children()).stream()
								.map(AttributeNode.class::cast)
								.collect(toMap(AttributeNode::definition, identity()));
				removeAllChildren();
				attributeNodes().stream()
								.map(node -> nodeMap.getOrDefault(node.definition(), node))
								.sorted(NODE_COMPARATOR)
								.filter(node -> displayNode(node, hideExcluded))
								.sorted((node1, node2) -> {
									int index1 = nodes.indexOf(node1);
									int index2 = nodes.indexOf(node2);
									if (index1 == -1 || index2 == -1) {
										return 0;
									}

									return Integer.compare(index1, index2);
								})
								.forEach(this::add);
				Collections.list(children()).stream()
								.filter(EntityNode.class::isInstance)
								.map(EntityNode.class::cast)
								.forEach(node -> node.reload(hideExcluded));
			}
		}

		private static boolean displayNode(AttributeNode node, boolean hideExcluded) {
			if (node.include().is() || (node instanceof MutableForeignKeyNode && ((MutableForeignKeyNode) node).includedCount() > 0)) {
				return true;
			}

			return !hideExcluded;
		}

		private List<AttributeNode> attributeNodes() {
			return treeModel.entities.definition(entityType).attributes().definitions().stream()
							.filter(EntityNode::selectedColumnOrAttribute)
							.filter(attributeDefinition -> treeModel.showHidden.is() || !attributeDefinition.hidden())
							.sorted(CAPTION_COMPARATOR)
							.map(this::createNode)
							.collect(toList());
		}

		private AttributeNode createNode(AttributeDefinition<?> attributeDefinition) {
			if (attributeDefinition instanceof ForeignKeyDefinition) {
				return new MutableForeignKeyNode(treeModel, (ForeignKeyDefinition) attributeDefinition);
			}
			else {
				return new MutableAttributeNode(treeModel, attributeDefinition);
			}
		}

		private List<Attribute<?>> include() {
			return Collections.list(children()).stream()
							.map(AttributeNode.class::cast)
							.filter(attribute -> attribute.include().is())
							.map(AttributeNode::attribute)
							.collect(toList());
		}

		private List<Attribute<?>> order() {
			return Collections.list(children()).stream()
							.map(AttributeNode.class::cast)
							.filter(EntityNode::order)
							.map(AttributeNode::attribute)
							.collect(toList());
		}

		private static boolean order(AttributeNode node) {
			if (node.include().is()) {
				return true;
			}
			if (node instanceof MutableForeignKeyNode) {
				return ((MutableForeignKeyNode) node).includedCount() > 0;
			}

			return false;
		}

		private static boolean selectedColumnOrAttribute(AttributeDefinition<?> definition) {
			if (definition instanceof ColumnDefinition) {
				return ((ColumnDefinition<?>) definition).selected();
			}

			return true;
		}

		private static String attributeName(Object item) {
			if (item instanceof String) {
				return (String) item;
			}

			return ((JSONObject) item).keys().next();
		}

		private void moveDown(int[] indexes) {
			if (indexes[indexes.length - 1] < children.size() - 1) {
				for (int i = indexes.length - 1; i >= 0; i--) {
					children.add(indexes[i] + 1, children.remove(indexes[i]));
				}
			}
		}

		private void moveUp(int[] indexes) {
			if (indexes[0] > 0) {
				for (int i = 0; i < indexes.length; i++) {
					children.add(indexes[i] - 1, children.remove(indexes[i]));
				}
			}
		}
	}

	static final class MutableAttributeNode extends DefaultMutableTreeNode implements AttributeNode {

		private final AttributeDefinition<?> definition;
		private final State include;

		private MutableAttributeNode(DefaultTreeModel treeModel, AttributeDefinition<?> definition) {
			this.definition = definition;
			this.include = State.builder()
							.listener(new NodeChanged(treeModel, this))
							.build();
		}

		@Override
		public AttributeDefinition<?> definition() {
			return definition;
		}

		public Attribute<?> attribute() {
			return definition.attribute();
		}

		@Override
		public boolean hidden() {
			return definition.hidden();
		}

		public State include() {
			return include;
		}
	}

	static final class MutableForeignKeyNode extends EntityNode implements AttributeNode {

		private final ForeignKeyDefinition definition;
		private final State include;

		private MutableForeignKeyNode(EntityTableExportTreeModel treeModel, ForeignKeyDefinition definition) {
			super(definition.attribute().referencedType(), treeModel);
			this.definition = definition;
			this.include = State.builder()
							.listener(new NodeChanged(treeModel, this))
							.build();
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public AttributeDefinition<?> definition() {
			return definition;
		}

		@Override
		public ForeignKey attribute() {
			return definition.attribute();
		}

		@Override
		public boolean hidden() {
			return definition.hidden();
		}

		@Override
		public State include() {
			return include;
		}

		@Override
		protected void populate(ExportAttributes.Builder attributes) {
			if (includedCount() > 0) {
				super.populate(attributes);
			}
		}

		int includedCount() {
			return includedCount(this);
		}

		private static int includedCount(DefaultMutableTreeNode node) {
			int counter = 0;
			Enumeration<? extends TreeNode> children = node.children();
			while (children.hasMoreElements()) {
				AttributeNode child = (AttributeNode) children.nextElement();
				if (child.include().is()) {
					counter++;
				}
				if (child instanceof MutableForeignKeyNode) {
					counter += includedCount((MutableForeignKeyNode) child);
				}
			}

			return counter;
		}
	}

	private static final class NodeChanged implements Runnable {

		private final DefaultTreeModel model;
		private final DefaultMutableTreeNode node;

		private NodeChanged(DefaultTreeModel model, DefaultMutableTreeNode node) {
			this.model = model;
			this.node = node;
		}

		@Override
		public void run() {
			model.nodeChanged(node);
			TreeNode parent = node.getParent();
			while (parent != null) {
				model.nodeChanged(parent);
				parent = parent.getParent();
			}
		}
	}

	private static final class AttributeCaptionComparator implements Comparator<AttributeDefinition<?>> {

		@Override
		public int compare(AttributeDefinition<?> d1, AttributeDefinition<?> d2) {
			return d1.caption().compareToIgnoreCase(d2.caption());
		}
	}

	private static class AttributeNodeComparator implements Comparator<AttributeNode> {

		@Override
		public int compare(AttributeNode o1, AttributeNode o2) {
			boolean o1Included = included(o1);
			boolean o2Included = included(o2);
			if (o1Included && o2Included) {
				return 0;
			}
			if (o1Included && !o2Included) {
				return -1;
			}
			if (!o1Included && o2Included) {
				return 1;
			}

			return CAPTION_COMPARATOR.compare(o1.definition(), o2.definition());
		}

		private static boolean included(AttributeNode node) {
			if (node instanceof MutableForeignKeyNode) {
				return node.include().is() || ((MutableForeignKeyNode) node).includedCount() > 0;
			}

			return node.include().is();
		}
	}
}
