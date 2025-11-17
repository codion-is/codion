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

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.attribute.ForeignKeyDefinition;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;

import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.lang.String.join;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class EntityTableExportModel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TAB = "\t";
	private static final String SPACE = " ";
	private static final String TSV = ".tsv";
	private static final String JSON = ".json";
	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String FOREIGN_KEYS_KEY = "foreignKeys";

	private final EntityTableModel<?> tableModel;
	private final EntityConnectionProvider connectionProvider;
	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final ExportTreeModel treeModel;
	private final State selected;

	EntityTableExportModel(EntityTableModel<?> tableModel, FilterTableColumnModel<Attribute<?>> columnModel) {
		this.tableModel = tableModel;
		this.columnModel = columnModel;
		this.connectionProvider = tableModel.connectionProvider();
		this.treeModel = new ExportTreeModel(tableModel.entityDefinition(), tableModel.connectionProvider().entities());
		this.selected = State.state(!tableModel.selection().empty().is());
		tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
	}

	ExportTask exportToClipboard() {
		return new ExportToClipboard();
	}

	ExportTask exportToFile(Path file) {
		return new ExportToFileTask(file);
	}

	ExportTreeModel treeModel() {
		return treeModel;
	}

	State selected() {
		return selected;
	}

	String defaultExportFileName() {
		return tableModel.entityDefinition().caption() + TSV;
	}

	String defaultConfigFileName() {
		return tableModel.entityDefinition().caption() + JSON;
	}

	void applyPreferences(JSONObject preferences) {
		if (preferences.isEmpty()) {
			selectDefaults();
		}
		else {
			applyAttributesAndForeignKeys(preferences, treeModel.getRoot().children());
		}
	}

	JSONObject createPreferences() {
		if (isDefaultConfiguration()) {
			return new JSONObject("{}");
		}

		return attributesToJson(treeModel.getRoot().children());
	}

	void selectAll() {
		select(true);
	}

	void selectNone() {
		select(false);
	}

	void selectDefaults() {
		selectNone();
		Enumeration<TreeNode> children = treeModel.getRoot().children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				Attribute<?> attribute = ((AttributeNode) child).definition().attribute();
				if (columnModel.contains(attribute) && columnModel.visible(attribute).is()) {
					((AttributeNode) child).selected().set(true);
				}
			}
		}
	}

	private boolean isDefaultConfiguration() {
		Enumeration<TreeNode> children = treeModel.getRoot().children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode node = (AttributeNode) child;
				Attribute<?> attribute = node.definition().attribute();
				// Default: first-level visible columns are selected, hidden are not
				boolean shouldBeSelected = columnModel.contains(attribute) && columnModel.visible(attribute).is();
				if (node.selected().is() != shouldBeSelected) {
					return false;
				}
				if (hasSelectedChildren(node)) {
					return false;
				}
			}
		}

		return true;
	}

	private static boolean hasSelectedChildren(AttributeNode node) {
		Enumeration<TreeNode> children = node.children();
		while (children.hasMoreElements()) {
			TreeNode child = children.nextElement();
			if (child instanceof AttributeNode) {
				AttributeNode childNode = (AttributeNode) child;
				if (childNode.selected().is()) {
					return true;
				}
				if (hasSelectedChildren(childNode)) {
					return true;
				}
			}
		}

		return false;
	}

	private void select(boolean select) {
		Enumeration<TreeNode> enumeration = treeModel.getRoot().breadthFirstEnumeration();
		enumeration.nextElement();// root
		while (enumeration.hasMoreElements()) {
			((AttributeNode) enumeration.nextElement()).selected().set(select);
		}
	}

	private static JSONObject attributesToJson(Enumeration<TreeNode> nodes) {
		JSONArray attributes = new JSONArray();
		JSONObject foreignKeys = new JSONObject();
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.definition().attribute().name();
			boolean isForeignKey = node.getChildCount() > 0;

			if (isForeignKey) {
				JSONObject fkChildren = attributesToJson(node.children());
				if (!node.selected().is() && fkChildren.isEmpty()) {
					continue;
				}
				if (node.selected().is()) {
					attributes.put(attributeName);
				}
				if (!fkChildren.isEmpty()) {
					foreignKeys.put(attributeName, fkChildren);
				}
			}
			else if (node.selected().is()) {
				attributes.put(attributeName);
			}
		}

		JSONObject result = new JSONObject();
		if (!attributes.isEmpty()) {
			result.put(ATTRIBUTES_KEY, attributes);
		}
		if (!foreignKeys.isEmpty()) {
			result.put(FOREIGN_KEYS_KEY, foreignKeys);
		}

		return result;
	}

	private void applyAttributesAndForeignKeys(JSONObject json, Enumeration<TreeNode> nodes) {
		Set<String> selectedAttributes = new HashSet<>();
		if (json.has(ATTRIBUTES_KEY)) {
			JSONArray attributes = json.getJSONArray(ATTRIBUTES_KEY);
			for (int i = 0; i < attributes.length(); i++) {
				selectedAttributes.add(attributes.getString(i));
			}
		}

		JSONObject foreignKeys = json.has(FOREIGN_KEYS_KEY) ? json.getJSONObject(FOREIGN_KEYS_KEY) : new JSONObject();
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.definition().attribute().name();
			boolean isForeignKey = node.getChildCount() > 0 || node.isCyclicalStub();

			node.selected().set(selectedAttributes.contains(attributeName));

			if (isForeignKey && foreignKeys.has(attributeName)) {
				if (node.isCyclicalStub()) {
					node.expand();
					treeModel.nodeStructureChanged(node);
				}
				applyAttributesAndForeignKeys(foreignKeys.getJSONObject(attributeName), node.children());
			}
			else if (isForeignKey) {
				deselectAll(node.children());
			}
		}
	}

	private static void deselectAll(Enumeration<TreeNode> nodes) {
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			node.selected().set(false);
			if (node.getChildCount() > 0) {
				deselectAll(node.children());
			}
		}
	}

	private List<String> createHeader() {
		return addToHeader(treeModel.getRoot().children(), new ArrayList<>(), "");
	}

	private static List<String> addToHeader(Enumeration<TreeNode> nodes, List<String> header, String prefix) {
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String caption = node.definition.caption();
			String columnHeader = prefix.isEmpty() ? caption : (prefix + SPACE + caption);
			if (node.selected.is()) {
				header.add(columnHeader);
			}
			if (node.definition.attribute() instanceof ForeignKey) {
				addToHeader(node.children(), header, columnHeader);
			}
		}

		return header;
	}

	private List<String> createRow(Entity entity) {
		return addToRow(treeModel.getRoot().children(), entity.primaryKey(),
						new ArrayList<>(), new HashMap<>(), connectionProvider.connection());
	}

	private static List<String> addToRow(Enumeration<TreeNode> attributeNodes, Entity.Key key, List<String> row,
																			 Map<Entity.Key, Entity> cache, EntityConnection connection) {
		Entity entity = cache.computeIfAbsent(key, k -> connection.select(key));
		while (attributeNodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) attributeNodes.nextElement();
			Attribute<?> attribute = node.definition.attribute();
			if (node.selected.is()) {
				row.add(replaceNewlinesAndTabs(entity.formatted(attribute)));
			}
			if (attribute instanceof ForeignKey) {
				Entity.Key referencedKey = entity.key((ForeignKey) attribute);
				if (referencedKey != null) {
					addToRow(node.children(), referencedKey, row, cache, connection);
				}
			}
		}

		return row;
	}

	private static String replaceNewlinesAndTabs(String string) {
		return string.replace("\r\n", SPACE)
						.replace("\n", SPACE)
						.replace("\r", SPACE)
						.replace(TAB, SPACE);
	}

	abstract class ExportTask implements ProgressTask<Void> {

		protected final AtomicInteger counter = new AtomicInteger();
		protected final State cancelled = State.state(false);
		protected final List<Entity> entities;

		protected ExportTask() {
			entities = selected.is() ?
							tableModel.selection().items().get() :
							tableModel.items().included().get();
		}

		@Override
		public final int maximum() {
			return entities.size();
		}

		final State cancelled() {
			return cancelled;
		}

		abstract String successMessage();
	}

	private final class ExportToFileTask extends ExportTask {

		private final Path file;

		private ExportToFileTask(Path file) {
			this.file = file;
		}

		@Override
		public void execute(ProgressReporter<Void> progress) throws Exception {
			try (BufferedWriter output = Files.newBufferedWriter(file)) {
				output.write(createHeader().stream()
								.collect(joining(TAB, "", "\n")));
				for (Entity entity : entities) {
					if (cancelled.is()) {
						throw new CancelException();
					}
					output.write(join(TAB, createRow(entity)));
					output.write("\n");
					progress.report(counter.incrementAndGet());
				}
			}
			catch (CancelException e) {
				Files.deleteIfExists(file);
				throw e;
			}
		}

		@Override
		String successMessage() {
			return MESSAGES.getString("exported_to_file") + ": " + file;
		}
	}

	private final class ExportToClipboard extends ExportTask {

		@Override
		public void execute(ProgressReporter<Void> progress) {
			String result = entities.stream()
							.map(entity -> createLine(entity, progress))
							.map(line -> join(TAB, line))
							.collect(joining("\n", createHeader().stream()
											.collect(joining(TAB, "", "\n")), ""));
			if (cancelled.is()) {
				throw new CancelException();
			}

			Utilities.setClipboard(result);
		}

		@Override
		String successMessage() {
			return MESSAGES.getString("exported_to_clipboard");
		}

		private List<String> createLine(Entity entity, ProgressReporter<Void> progress) {
			if (cancelled.is()) {
				throw new CancelException();
			}
			List<String> row = createRow(entity);
			progress.report(counter.incrementAndGet());

			return row;
		}
	}

	static final class EntityNode extends DefaultMutableTreeNode {

		private static final AttributeDefinitionComparator ATTRIBUTE_COMPARATOR = new AttributeDefinitionComparator();

		private EntityNode(EntityDefinition definition, Entities entities) {
			populate(this, definition, entities, new HashSet<>(), definition.type());
		}

		private static void populate(DefaultMutableTreeNode parent, EntityDefinition definition,
																 Entities entities, Set<ForeignKey> visited, EntityType rootType) {
			for (AttributeDefinition<?> attributeDefinition : definition.attributes().definitions()
							.stream()
							.sorted(ATTRIBUTE_COMPARATOR)
							.collect(toList())) {
				if (attributeDefinition instanceof ForeignKeyDefinition) {
					ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) attributeDefinition;
					ForeignKey foreignKey = foreignKeyDefinition.attribute();
					boolean isCyclical = visited.contains(foreignKey) || foreignKey.referencedType().equals(rootType);
					AttributeNode foreignKeyNode = new AttributeNode(foreignKeyDefinition, isCyclical, entities, new HashSet<>(visited));
					parent.add(foreignKeyNode);
					if (!isCyclical) {
						visited.add(foreignKey);
						populate(foreignKeyNode, entities.definition(foreignKey.referencedType()), entities, visited, rootType);
					}
				}
				else if (!attributeDefinition.hidden()) {
					parent.add(new AttributeNode(attributeDefinition));
				}
			}
		}

		private static final class AttributeDefinitionComparator implements Comparator<AttributeDefinition<?>> {

			private final Collator collator = Collator.getInstance();

			@Override
			public int compare(AttributeDefinition<?> definition1, AttributeDefinition<?> definition2) {
				return collator.compare(definition1.caption().toLowerCase(), definition2.caption().toLowerCase());
			}
		}
	}

	static final class ExportTreeModel extends DefaultTreeModel {

		private ExportTreeModel(EntityDefinition definition, Entities entities) {
			super(new EntityNode(definition, entities));
		}

		@Override
		public EntityNode getRoot() {
			return (EntityNode) super.getRoot();
		}
	}

	static class AttributeNode extends DefaultMutableTreeNode {

		private final AttributeDefinition<?> definition;
		private final State selected = State.state();
		private final boolean isCyclicalStub;
		private final Entities entities;
		private final Set<ForeignKey> visitedPath;

		private AttributeNode(AttributeDefinition<?> definition) {
			this(definition, false, null, null);
		}

		private AttributeNode(AttributeDefinition<?> definition, boolean isCyclicalStub,
													Entities entities, Set<ForeignKey> visitedPath) {
			this.definition = definition;
			this.isCyclicalStub = isCyclicalStub;
			this.entities = entities;
			this.visitedPath = visitedPath;
		}

		@Override
		public boolean isLeaf() {
			return !isCyclicalStub && super.isLeaf();
		}

		@Override
		public String toString() {
			return selected.is() ? "+" + definition.caption() : definition.caption() + "  ";
		}

		AttributeDefinition<?> definition() {
			return definition;
		}

		State selected() {
			return selected;
		}

		boolean isCyclicalStub() {
			return isCyclicalStub;
		}

		void expand() {
			if (!isCyclicalStub || !(definition instanceof ForeignKeyDefinition)) {
				return;
			}
			ForeignKeyDefinition foreignKeyDefinition = (ForeignKeyDefinition) definition;
			ForeignKey foreignKey = foreignKeyDefinition.attribute();

			Set<ForeignKey> newVisited = new HashSet<>(visitedPath);
			newVisited.add(foreignKey);

			EntityDefinition referencedDefinition = entities.definition(foreignKey.referencedType());
			EntityNode.populate(this, referencedDefinition, entities, newVisited, referencedDefinition.type());
		}
	}
}
