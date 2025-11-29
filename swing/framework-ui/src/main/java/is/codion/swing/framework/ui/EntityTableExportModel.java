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
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityExportModel;
import is.codion.framework.model.EntityExportModel.AttributeNode;
import is.codion.framework.model.EntityExportModel.EntityNode;
import is.codion.framework.model.EntityExportModel.ForeignKeyNode;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.Utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.model.EntityExportModel.entityExportModel;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class EntityTableExportModel {

	private static final String ENTITY_TYPE_KEY = "entityType";
	private static final String ATTRIBUTES_KEY = "attributes";
	private static final String DIALOG_SIZE_KEY = "dialogSize";
	private static final String CONFIGURATION_FILES_KEY = "configurationFiles";
	private static final String SELECTED_CONFIGURATION_FILE_KEY = "selectedConfigurationFile";

	static final NullConfigurationFile NULL_CONFIGURATION_FILE = new NullConfigurationFile();
	static final String JSON = "json";

	private final EntityTableModel<?> tableModel;
	private final EntityExportModel exportModel;
	private final FilterComboBoxModel<ConfigurationFile> configurationFilesComboBoxModel;
	private final ExportTreeModel treeModel;
	private final Event<?> configurationChanged = Event.event();
	private final State selected;

	private @Nullable Dimension dialogSize;

	EntityTableExportModel(EntityTableModel<?> tableModel) {
		this.tableModel = tableModel;
		this.exportModel = entityExportModel(tableModel.entityDefinition().type(), tableModel.connectionProvider());
		this.treeModel = new ExportTreeModel(exportModel);
		this.configurationFilesComboBoxModel = FilterComboBoxModel.builder()
						.items(this::refreshConfigurationFiles)
						.nullItem(NULL_CONFIGURATION_FILE)
						.onSelection(this::configurationFileSelected)
						.build();
		this.selected = State.state(!tableModel.selection().empty().is());
		this.tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
		selectDefaults();
	}

	ExportTask exportToClipboard() {
		return new ExportToClipboard(selected.is() ?
						tableModel.selection().items().get() :
						tableModel.items().included().get());
	}

	ExportTask exportToFile(Path file) {
		return new ExportToFileTask(requireNonNull(file), selected.is() ?
						tableModel.selection().items().get() :
						tableModel.items().included().get());
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

	void setDialogSize(Dimension dialogSize) {
		this.dialogSize = dialogSize;
	}

	@Nullable Dimension getDialogSize() {
		return dialogSize;
	}

	void addConfigurationFiles(Collection<File> configurationFiles) {
		List<DefaultConfigurationFile> files = configurationFiles.stream()
						.map(DefaultConfigurationFile::new)
						.collect(toList());
		validateEntityType(files);
		if (files.size() == 1) {
			addAndSelect(files.get(0));
		}
		else {
			files.forEach(file -> configurationFilesComboBoxModel.items().add(file));
		}
	}

	void clearConfigurationFiles() {
		configurationFilesComboBoxModel.items().clear();
	}

	void applyPreferences(JSONObject preferences) {
		selectDefaults();
		if (preferences.has(CONFIGURATION_FILES_KEY)) {
			JSONArray fileArray = preferences.getJSONArray(CONFIGURATION_FILES_KEY);
			List<File> files = new ArrayList<>(fileArray.length());
			fileArray.forEach(filePath -> {
				if (filePath instanceof String) {
					File file = new File((String) filePath);
					if (file.exists()) {
						files.add(file);
					}
				}
			});
			addConfigurationFiles(files);
			if (preferences.has(SELECTED_CONFIGURATION_FILE_KEY)) {
				File file = new File(preferences.getString(SELECTED_CONFIGURATION_FILE_KEY));
				if (file.exists()) {
					configurationFilesComboBoxModel.items().get().stream()
									.filter(configurationFile -> configurationFile.file().equals(file))
									.findFirst()
									.ifPresent(configurationFile -> configurationFilesComboBoxModel.selection().item().set(configurationFile));
				}
			}
		}
		if (preferences.has(DIALOG_SIZE_KEY)) {
			String dialogSizePreferences = preferences.getString(DIALOG_SIZE_KEY);
			if (dialogSizePreferences != null) {
				String[] size = dialogSizePreferences.split("x");
				if (size.length == 2) {
					dialogSize = new Dimension(Integer.parseInt(size[0]), Integer.parseInt(size[1]));
				}
			}
		}
	}

	void writeConfig(File file) {
		try {
			Files.write(file.toPath(), createExportPreferences().toString(2).getBytes(UTF_8));
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
		if (dialogSize != null) {
			json.put(DIALOG_SIZE_KEY, dialogSize.width + "x" + dialogSize.height);
		}

		return json;
	}

	private JSONObject createExportPreferences() {
		JSONObject jsonObject = attributesToJson(treeModel.getRoot().children());
		jsonObject.put(ENTITY_TYPE_KEY, entityDefinition().type().name());

		return jsonObject;
	}

	private Collection<ConfigurationFile> refreshConfigurationFiles() {
		return configurationFilesComboBoxModel.items().get().stream()
						.filter(DefaultConfigurationFile.class::isInstance)
						.map(DefaultConfigurationFile.class::cast)
						.filter(configurationFile -> configurationFile.file.exists())
						.collect(toList());
	}

	private void configurationFileSelected(@Nullable ConfigurationFile configurationFile) {
		if (configurationFile == null || configurationFile.file() == null) {
			selectDefaults();
			configurationChanged.run();
		}
		else {
			applyAttributesAndForeignKeys(((DefaultConfigurationFile) configurationFile).json, treeModel.getRoot());
		}
	}

	void selectAll() {
		exportModel.selectAll();
		configurationChanged.run();
	}

	void selectNone() {
		exportModel.selectNone();
		configurationChanged.run();
	}

	void selectDefaults() {
		exportModel.selectDefaults();
		treeModel.setRoot(new MutableEntityNode(exportModel.root()));
		configurationChanged.run();
	}

	void sortChildren(DefaultMutableTreeNode parent, List<TreeNode> children) {
		parent.removeAllChildren();
		children.forEach(child -> parent.add((MutableTreeNode) child));
		((EntityNode) parent.getUserObject()).sort(new AttributeNodeComparator(children));
	}

	private static JSONObject attributesToJson(Enumeration<TreeNode> nodes) {
		JSONArray attributes = new JSONArray();
		while (nodes.hasMoreElements()) {
			MutableAttributeNode node = (MutableAttributeNode) nodes.nextElement();
			String attributeName = node.attribute().name();
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

	private void applyAttributesAndForeignKeys(JSONObject json, DefaultMutableTreeNode node) {
		if (!json.has(ATTRIBUTES_KEY)) {
			return;
		}
		Enumeration<TreeNode> childNodes = node.children();
		Map<String, MutableAttributeNode> children = new HashMap<>();
		while (childNodes.hasMoreElements()) {
			MutableAttributeNode child = (MutableAttributeNode) childNodes.nextElement();
			children.put(child.attribute().name(), child);
		}
		JSONArray attributes = json.getJSONArray(ATTRIBUTES_KEY);
		Set<String> processed = new HashSet<>();
		int insertIndex = 0;
		for (Object item : attributes) {
			if (item instanceof String) {// String = attribute/FK is selected
				String attributeName = (String) item;
				MutableAttributeNode child = children.get(attributeName);
				if (child != null && processed.add(attributeName)) {
					child.selected().set(true);
					reorderNode(node, child, insertIndex++);
				}
			}
			else if (item instanceof JSONObject) {// Object = FK with children structure
				JSONObject fkObject = (JSONObject) item;
				String fkName = fkObject.keys().next();
				MutableForeignKeyNode child = (MutableForeignKeyNode) children.get(fkName);
				if (child != null) {
					if (processed.add(fkName)) {// Only reorder if we haven't already processed this FK as a string
						reorderNode(node, child, insertIndex++);
					}
					if (child.isCyclicalStub()) {// Expand and apply children
						child.expand();
						treeModel.nodeStructureChanged(child);
					}
					applyAttributesAndForeignKeys(fkObject.getJSONObject(fkName), child);
				}
			}
		}
		for (MutableAttributeNode child : children.values()) {// Nodes not in JSON: deselect and leave at end in their current order
			if (!processed.contains(child.attribute().name())) {
				child.selected().set(false);
				if (child.getChildCount() > 0 || (child instanceof MutableForeignKeyNode && ((MutableForeignKeyNode) child).isCyclicalStub())) {
					deselectAll(child.children());
				}
			}
		}
		configurationChanged.run();
	}

	private static void deselectAll(Enumeration<TreeNode> nodes) {
		while (nodes.hasMoreElements()) {
			MutableAttributeNode node = (MutableAttributeNode) nodes.nextElement();
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

	private void validateEntityType(List<DefaultConfigurationFile> files) {
		List<File> incorrectEntityType = files.stream()
						.filter(file -> !entityDefinition().type().name().equals(file.entityType))
						.map(DefaultConfigurationFile::file)
						.collect(toList());
		if (!incorrectEntityType.isEmpty()) {
			throw new IllegalArgumentException("Incorrect entity type:\n" + incorrectEntityType);
		}
	}

	abstract static class ExportTask implements ProgressTask<Void> {

		protected final AtomicInteger counter = new AtomicInteger();
		protected final State cancelled = State.state(false);
		protected final List<Entity> entities;

		protected ExportTask(List<Entity> entities) {
			this.entities = entities;
		}

		@Override
		public final int maximum() {
			return entities.size();
		}

		final State cancelled() {
			return cancelled;
		}
	}

	private final class ExportToFileTask extends ExportTask {

		private final Path file;

		private ExportToFileTask(Path file, List<Entity> entities) {
			super(entities);
			this.file = file;
		}

		@Override
		public void execute(ProgressReporter<Void> progress) throws Exception {
			try (BufferedWriter output = Files.newBufferedWriter(file)) {
				exportModel.export(entities.iterator(), line -> write(line, output),
								() -> progress.report(counter.incrementAndGet()), cancelled.observable());
				if (cancelled.is()) {
					throw new CancelException();
				}
			}
			catch (CancelException e) {
				Files.deleteIfExists(file);
				throw e;
			}
		}

		private void write(String line, BufferedWriter output) {
			try {
				output.write(line);
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
		}
	}

	private final class ExportToClipboard extends ExportTask {

		private ExportToClipboard(List<Entity> entities) {
			super(entities);
		}

		@Override
		public void execute(ProgressReporter<Void> progress) {
			StringBuilder builder = new StringBuilder();
			exportModel.export(entities.iterator(), builder::append,
							() -> progress.report(counter.incrementAndGet()), cancelled.observable());
			if (cancelled.is()) {
				throw new CancelException();
			}
			Utilities.setClipboard(builder.toString());
		}
	}

	private static final class MutableEntityNode extends DefaultMutableTreeNode {

		private MutableEntityNode(EntityNode rootNode) {
			super(rootNode);
			populate(this, rootNode.children());
		}
	}

	static final class ExportTreeModel extends DefaultTreeModel {

		private ExportTreeModel(EntityExportModel exportModel) {
			super(new MutableEntityNode(exportModel.root()));
		}

		@Override
		public DefaultMutableTreeNode getRoot() {
			return (DefaultMutableTreeNode) super.getRoot();
		}
	}

	/**
	 * Swing tree node wrapping an ExportNode.
	 */
	static class MutableAttributeNode extends DefaultMutableTreeNode {

		private final AttributeNode node;

		private MutableAttributeNode(AttributeNode node) {
			super(node);
			this.node = node;
		}

		@Override
		public final String toString() {
			return node.selected().is() ? "+" + node.caption() : node.caption() + "  ";
		}

		final Attribute<?> attribute() {
			return node.attribute();
		}

		final State selected() {
			return node.selected();
		}

		AttributeNode node() {
			return node;
		}
	}

	static final class MutableForeignKeyNode extends MutableAttributeNode {

		private MutableForeignKeyNode(ForeignKeyNode node) {
			super(node);
			populate(this, node.children());
		}

		@Override
		public boolean isLeaf() {
			return !node().isCyclicalStub() && super.isLeaf();
		}

		@Override
		ForeignKeyNode node() {
			return (ForeignKeyNode) super.node();
		}

		boolean isCyclicalStub() {
			return node().isCyclicalStub();
		}

		void expand() {
			if (!node().isCyclicalStub()) {
				return;
			}
			node().expand();
			populate(this, node().children());
		}
	}

	private static void populate(DefaultMutableTreeNode parent, List<AttributeNode> children) {
		parent.removeAllChildren();
		for (AttributeNode child : children) {
			if (child instanceof ForeignKeyNode) {
				parent.add(new MutableForeignKeyNode((ForeignKeyNode) child));
			}
			else {
				parent.add(new MutableAttributeNode(child));
			}
		}
	}

	private static final class AttributeNodeComparator implements Comparator<AttributeNode> {

		private final Map<AttributeNode, Integer> indexes;

		private AttributeNodeComparator(List<TreeNode> children) {
			indexes = new HashMap<>();
			int i = 0;
			for (TreeNode node : children) {
				indexes.put(((MutableAttributeNode) node).node(), i++);
			}
		}

		@Override
		public int compare(AttributeNode node1, AttributeNode node2) {
			Integer index1 = indexes.get(node1);
			Integer index2 = indexes.get(node2);

			return index1.compareTo(index2);
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
		private final JSONObject json;
		private final String entityType;

		private DefaultConfigurationFile(File file) {
			this.file = file;
			try {
				this.json = new JSONObject(new String(Files.readAllBytes(file.toPath()), UTF_8));
				if (json.has(ENTITY_TYPE_KEY)) {
					this.entityType = json.getString(ENTITY_TYPE_KEY);
				}
				else {
					throw new IllegalStateException("Configuration file is missing '" + ENTITY_TYPE_KEY + "'");
				}
			}
			catch (IOException e) {
				throw new UncheckedIOException(e);
			}
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
