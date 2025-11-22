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
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
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
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.table.FilterTableColumnModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class EntityTableExportModel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String TAB = "\t";
	private static final String SPACE = " ";
	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String CONFIGURATION_FILES_KEY = "configurationFiles";
	private static final String SELECTED_CONFIGURATION_FILE_KEY = "selectedConfigurationFile";

	static final NullConfigurationFile NULL_CONFIGURATION_FILE = new NullConfigurationFile();
	static final String JSON = "json";

	private final EntityTableModel<?> tableModel;
	private final EntityConnectionProvider connectionProvider;
	private final FilterTableColumnModel<Attribute<?>> columnModel;
	private final FilterComboBoxModel<ConfigurationFile> configurationFilesComboBoxModel;
	private final ExportTreeModel treeModel;
	private final Event<?> configurationChanged = Event.event();
	private final State selected;

	EntityTableExportModel(EntityTableModel<?> tableModel, FilterTableColumnModel<Attribute<?>> columnModel) {
		this.tableModel = tableModel;
		this.columnModel = columnModel;
		this.connectionProvider = tableModel.connectionProvider();
		this.treeModel = new ExportTreeModel(tableModel.entityDefinition(), tableModel.connectionProvider().entities());
		this.configurationFilesComboBoxModel = FilterComboBoxModel.builder()
						.items(this::refreshConfigurationFiles)
						.nullItem(NULL_CONFIGURATION_FILE)
						.onSelection(this::configurationFileSelected)
						.build();
		this.selected = State.state(!tableModel.selection().empty().is());
		this.tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
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

	FilterComboBoxModel<ConfigurationFile> configurationFiles() {
		return configurationFilesComboBoxModel;
	}

	EntityDefinition entityDefinition() {
		return tableModel.entityDefinition();
	}

	State selected() {
		return selected;
	}

	String defaultExportFileName() {
		return tableModel.entityDefinition().caption();
	}

	Observer<?> configurationChanged() {
		return configurationChanged.observer();
	}

	void addConfigurationFiles(Collection<File> configurationFiles) {
		Iterator<File> iterator = configurationFiles.iterator();
		if (iterator.hasNext()) {
			addAndSelect(new DefaultConfigurationFile(iterator.next()));
		}
		while (iterator.hasNext()) {
			configurationFilesComboBoxModel.items().add(new DefaultConfigurationFile(iterator.next()));
		}
	}

	void applyAttributePreferences(File file) {
		try {
			applyAttributePreferences(new JSONObject(new String(Files.readAllBytes(file.toPath()), UTF_8)));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	void applyAttributePreferences(JSONObject preferences) {
		applyAttributesAndForeignKeys(preferences, treeModel.getRoot());
	}

	void applyPreferences(JSONObject preferences) {
		selectDefaults();
		if (preferences.has(CONFIGURATION_FILES_KEY)) {
			JSONArray files = preferences.getJSONArray(CONFIGURATION_FILES_KEY);
			files.forEach(filePath -> {
				if (filePath instanceof String) {
					File file = new File((String) filePath);
					if (file.exists()) {
						configurationFilesComboBoxModel.items().add(new DefaultConfigurationFile(file));
					}
				}
			});
			if (preferences.has(SELECTED_CONFIGURATION_FILE_KEY)) {
				File file = new File(preferences.getString(SELECTED_CONFIGURATION_FILE_KEY));
				if (file.exists()) {
					configurationFilesComboBoxModel.selection().item().set(new DefaultConfigurationFile(file));
				}
			}
		}
	}

	void writeConfig(File file) {
		try {
			Files.write(file.toPath(), createExportPreferences().toString().getBytes(UTF_8));
			addAndSelect(new DefaultConfigurationFile(file));
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	private void addAndSelect(ConfigurationFile fileItem) {
		configurationFilesComboBoxModel.items().add(fileItem);
		configurationFilesComboBoxModel.selection().item().set(fileItem);
	}

	JSONObject createPreferences() {
		JSONObject json = new JSONObject();
		if (!configurationFilesComboBoxModel.items().get().isEmpty()) {
			JSONArray recentFiles = new JSONArray();
			configurationFilesComboBoxModel.items().get()
							.forEach(fileItem -> recentFiles.put(fileItem.file().getAbsolutePath()));
			json.put(CONFIGURATION_FILES_KEY, recentFiles);
			configurationFilesComboBoxModel.selection().item().optional().ifPresent(selectedConfigurationFile ->
							json.put(SELECTED_CONFIGURATION_FILE_KEY, selectedConfigurationFile.file().getAbsolutePath()));
		}

		return json;
	}

	private JSONObject createExportPreferences() {
		if (isDefaultConfiguration()) {
			return new JSONObject("{}");
		}

		return attributesToJson(treeModel.getRoot().children());
	}

	private Collection<ConfigurationFile> refreshConfigurationFiles() {
		return configurationFilesComboBoxModel.items().get().stream()
						.filter(DefaultConfigurationFile.class::isInstance)
						.map(DefaultConfigurationFile.class::cast)
						.filter(configurationFile -> configurationFile.file.exists())
						.collect(toList());
	}

	private void configurationFileSelected(@Nullable ConfigurationFile fileItem) {
		if (fileItem == null || fileItem.file() == null) {
			selectDefaults();
			configurationChanged.run();
		}
		else {
			applyAttributePreferences(fileItem.file());
		}
	}

	void selectAll() {
		select(true);
		configurationChanged.run();
	}

	void selectNone() {
		select(false);
		configurationChanged.run();
	}

	void selectDefaults() {
		treeModel.setRoot(new EntityNode(entityDefinition(), tableModel.entities()));
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
		configurationChanged.run();
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
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.definition().attribute().name();
			boolean isForeignKey = node.getChildCount() > 0;

			if (isForeignKey) {
				JSONObject fkChildren = attributesToJson(node.children());
				if (!node.selected().is() && fkChildren.isEmpty()) {
					continue;
				}
				if (node.selected().is()) {// If FK itself is selected, add its name to the attributes array
					attributes.put(attributeName);
				}
				if (!fkChildren.isEmpty()) {// If FK has selected children, add the structure object
					JSONObject fkObject = new JSONObject();
					fkObject.put(attributeName, fkChildren);
					attributes.put(fkObject);
				}
			}
			else if (node.selected().is()) {// Simple attributes are just strings
				attributes.put(attributeName);
			}
		}

		JSONObject result = new JSONObject();
		if (!attributes.isEmpty()) {
			result.put(ATTRIBUTES_KEY, attributes);
		}

		return result;
	}

	private void applyAttributesAndForeignKeys(JSONObject json, MutableTreeNode node) {
		if (!json.has(ATTRIBUTES_KEY)) {
			return;
		}
		Enumeration<TreeNode> childNodes = (Enumeration<TreeNode>) node.children();
		Map<String, AttributeNode> children = new HashMap<>();
		while (childNodes.hasMoreElements()) {
			AttributeNode child = (AttributeNode) childNodes.nextElement();
			children.put(child.definition().attribute().name(), child);
		}
		JSONArray attributes = json.getJSONArray(ATTRIBUTES_KEY);
		Set<String> processed = new HashSet<>();
		int insertIndex = 0;
		for (Object item : attributes) {
			if (item instanceof String) {// String = attribute/FK is selected
				String attributeName = (String) item;
				AttributeNode child = children.get(attributeName);
				if (child != null && processed.add(attributeName)) {
					child.selected().set(true);
					reorderNode(node, child, insertIndex++);
				}
			}
			else if (item instanceof JSONObject) {// Object = FK with children structure
				JSONObject fkObject = (JSONObject) item;
				String fkName = fkObject.keys().next();
				AttributeNode child = children.get(fkName);
				if (child != null) {
					if (processed.add(fkName)) {// Only reorder if we haven't already processed this FK as a string
						reorderNode(node, child, insertIndex++);
					}
					if (child.isCyclicalStub()) {// Expand and apply children
						child.expand();
						treeModel.nodeStructureChanged(child);
					}
					JSONObject fkChildren = fkObject.getJSONObject(fkName);
					applyAttributesAndForeignKeys(fkChildren, child);
				}
			}
		}
		for (AttributeNode child : children.values()) {// Nodes not in JSON: deselect and leave at end in their current order
			if (!processed.contains(child.definition().attribute().name())) {
				child.selected().set(false);
				if (child.getChildCount() > 0 || child.isCyclicalStub()) {
					deselectAll(child.children());
				}
			}
		}
		configurationChanged.run();
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

	private void reorderNode(MutableTreeNode parent, MutableTreeNode node, int index) {
		int currentIndex = parent.getIndex(node);
		if (currentIndex != index) {
			parent.remove(currentIndex);
			parent.insert(node, index);
			treeModel.nodeStructureChanged(parent);
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

	interface ConfigurationFile {

		File file();

		String filename();
	}

	private static final class NullConfigurationFile implements ConfigurationFile {

		@Override
		public File file() {
			throw new IllegalStateException();
		}

		@Override
		public String filename() {
			throw new IllegalStateException();
		}

		@Override
		public String toString() {
			return "-";
		}
	}

	private static final class DefaultConfigurationFile implements ConfigurationFile {

		private final File file;

		private DefaultConfigurationFile(File file) {
			this.file = file;
		}

		@Override
		public File file() {
			return file;
		}

		@Override
		public String filename() {
			return file.getName().substring(0, file.getName().length() - JSON.length() - 1);
		}

		@Override
		public String toString() {
			return filename();
		}

		@Override
		public boolean equals(Object object) {
			if (object == null || getClass() != object.getClass()) {
				return false;
			}
			DefaultConfigurationFile that = (DefaultConfigurationFile) object;

			return Objects.equals(file, that.file);
		}

		@Override
		public int hashCode() {
			return Objects.hashCode(file);
		}
	}
}
