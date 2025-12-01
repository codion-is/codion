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
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.model.EntityExport;
import is.codion.framework.model.EntityExport.Settings;
import is.codion.framework.model.EntityExport.Settings.AttributeExport;
import is.codion.framework.model.EntityExport.Settings.Attributes;
import is.codion.framework.model.EntityExport.Settings.ForeignKeyExport;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ComboBoxItems;
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
	private final EntityConnectionProvider connectionProvider;
	private final Settings settings;
	private final FilterComboBoxModel<ConfigurationFile> configurationFiles;
	private final ExportTreeModel treeModel;
	private final Event<?> configurationChanged = Event.event();
	private final State selected;

	private @Nullable Dimension dialogSize;

	EntityTableExportModel(EntityTableModel<?> tableModel) {
		this.tableModel = tableModel;
		this.connectionProvider = tableModel.connectionProvider();
		this.settings = EntityExport.settings(tableModel.entityDefinition().type(), tableModel.connectionProvider().entities());
		this.treeModel = new ExportTreeModel(settings);
		this.configurationFiles = FilterComboBoxModel.builder()
						.items(this::refreshConfigurationFiles)
						.nullItem(NULL_CONFIGURATION_FILE)
						.onSelection(this::configurationFileSelected)
						.build();
		this.selected = State.state(!tableModel.selection().empty().is());
		this.tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
		includeDefault();
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
		return configurationFiles;
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
			files.forEach(this::add);
		}
	}

	void clearConfigurationFiles() {
		configurationFiles.items().clear();
	}

	void applyPreferences(JSONObject preferences) {
		includeDefault();
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
					configurationFiles.items().get().stream()
									.filter(configurationFile -> configurationFile.file().equals(file))
									.findFirst()
									.ifPresent(configurationFile -> configurationFiles.selection().item().set(configurationFile));
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

	private void add(ConfigurationFile configurationFile) {
		ComboBoxItems<ConfigurationFile> items = configurationFiles.items();
		if (items.contains(configurationFile)) {
			items.replace(configurationFile, configurationFile);
		}
		else {
			items.add(configurationFile);
		}
	}

	private void addAndSelect(ConfigurationFile configurationFile) {
		add(configurationFile);
		configurationFiles.selection().item().set(configurationFile);
	}

	JSONObject createPreferences() {
		JSONObject json = new JSONObject();
		if (!configurationFiles.items().get().isEmpty()) {
			JSONArray recentFiles = new JSONArray();
			configurationFiles.items().get()
							.forEach(configurationFile -> recentFiles.put(configurationFile.file().getAbsolutePath()));
			json.put(CONFIGURATION_FILES_KEY, recentFiles);
			configurationFiles.selection().item().optional().ifPresent(selectedConfigurationFile ->
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
		return configurationFiles.items().get().stream()
						.filter(DefaultConfigurationFile.class::isInstance)
						.filter(configurationFile -> configurationFile.file().exists())
						.map(configurationFile -> new DefaultConfigurationFile(configurationFile.file()))
						.collect(toList());
	}

	private void configurationFileSelected(@Nullable ConfigurationFile configurationFile) {
		if (configurationFile == null || configurationFile.file() == null) {
			includeDefault();
			configurationChanged.run();
		}
		else {
			applyAttributesAndForeignKeys(((DefaultConfigurationFile) configurationFile).json, treeModel.getRoot());
		}
	}

	void includeAll() {
		include(settings.attributes().get(), true);
		configurationChanged.run();
	}

	void includeNone() {
		include(settings.attributes().get(), false);
		configurationChanged.run();
	}

	void includeDefault() {
		includeNone();
		settings.attributes().get().forEach(node -> node.include().set(true));
		configurationChanged.run();
	}

	void sortAttributes(DefaultMutableTreeNode parent, List<TreeNode> children) {
		parent.removeAllChildren();
		children.forEach(child -> parent.add((MutableTreeNode) child));
		Attributes attributes = (Attributes) parent.getUserObject();
		attributes.sort(new AttributeExportComparator(children));
	}

	private static void include(List<AttributeExport> nodes, boolean include) {
		for (AttributeExport node : nodes) {
			node.include().set(include);
			if (node instanceof ForeignKeyExport) {
				include(((ForeignKeyExport) node).attributes().get(), include);
			}
		}
	}

	private static JSONObject attributesToJson(Enumeration<TreeNode> nodes) {
		JSONArray attributes = new JSONArray();
		while (nodes.hasMoreElements()) {
			MutableAttributeNode node = (MutableAttributeNode) nodes.nextElement();
			String attributeName = node.attribute().name();
			boolean isForeignKey = node.getChildCount() > 0;

			if (isForeignKey) {
				JSONObject fkChildren = attributesToJson(node.children());
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
			if (item instanceof String) {// String = attribute/FK is included
				String attributeName = (String) item;
				MutableAttributeNode child = children.get(attributeName);
				if (child != null && processed.add(attributeName)) {
					child.include().set(true);
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
					if (child.expandable()) {// Expand and apply children
						child.expand();
						treeModel.nodeStructureChanged(child);
					}
					applyAttributesAndForeignKeys(fkObject.getJSONObject(fkName), child);
				}
			}
		}
		for (MutableAttributeNode child : children.values()) {// Nodes not in JSON: exclude and leave at end in their current order
			if (!processed.contains(child.attribute().name())) {
				child.include().set(false);
				if (child.getChildCount() > 0 || (child instanceof MutableForeignKeyNode && ((MutableForeignKeyNode) child).expandable())) {
					excludeAll(child.children());
				}
			}
		}
		configurationChanged.run();
	}

	private static void excludeAll(Enumeration<TreeNode> nodes) {
		while (nodes.hasMoreElements()) {
			MutableAttributeNode node = (MutableAttributeNode) nodes.nextElement();
			node.include().set(false);
			if (node.getChildCount() > 0) {
				excludeAll(node.children());
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
		protected final State cancel = State.state(false);
		protected final List<Entity> entities;

		protected ExportTask(List<Entity> entities) {
			this.entities = entities;
		}

		@Override
		public final int maximum() {
			return entities.size();
		}

		final State cancel() {
			return cancel;
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
				EntityExport.builder()
								.entities(entities.iterator())
								.connectionProvider(connectionProvider)
								.output(line -> write(line, output))
								.settings(settings)
								.handler(entity -> progress.report(counter.incrementAndGet()))
								.cancel(cancel.observable())
								.export();
				if (cancel.is()) {
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
			EntityExport.builder()
							.entities(entities.iterator())
							.connectionProvider(connectionProvider)
							.output(builder::append)
							.settings(settings)
							.handler(entity -> progress.report(counter.incrementAndGet()))
							.cancel(cancel.observable())
							.export();
			if (cancel.is()) {
				throw new CancelException();
			}
			Utilities.setClipboard(builder.toString());
		}
	}

	final class ExportTreeModel extends DefaultTreeModel {

		private ExportTreeModel(Settings settings) {
			super(new MutableEntityNode(settings));
		}

		@Override
		public DefaultMutableTreeNode getRoot() {
			return (DefaultMutableTreeNode) super.getRoot();
		}
	}

	private final class MutableEntityNode extends DefaultMutableTreeNode {

		private MutableEntityNode(Settings settings) {
			super(settings.attributes());
			populate(this, settings.attributes().get());
		}
	}

	class MutableAttributeNode extends DefaultMutableTreeNode {

		private final AttributeExport node;
		private final String caption;

		private MutableAttributeNode(AttributeExport node) {
			this.node = node;
			this.caption = connectionProvider.entities()
							.definition(node.attribute().entityType())
							.attributes().definition(node.attribute()).caption();
			node.include().addListener(this::includeChanged);
		}

		@Override
		public String toString() {
			return node().include().is() ? "+" + caption : caption;
		}

		final Attribute<?> attribute() {
			return node().attribute();
		}

		final State include() {
			return node().include();
		}

		AttributeExport node() {
			return node;
		}

		private void includeChanged() {
			treeModel.nodeChanged(this);
			TreeNode parent = getParent();
			while (parent != null) {
				treeModel.nodeChanged(parent);
				parent = parent.getParent();
			}
		}
	}

	final class MutableForeignKeyNode extends MutableAttributeNode {

		private MutableForeignKeyNode(ForeignKeyExport node) {
			super(node);
			setUserObject(node.attributes());
			populate(this, node.attributes().get());
		}

		@Override
		public boolean isLeaf() {
			return !expandable() && super.isLeaf();
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder(super.toString());
			int includedChildrenCount = includedChildrenCount(this);
			if (includedChildrenCount > 0) {
				builder.append(" (").append(includedChildrenCount).append(")");
			}

			return builder.toString();
		}

		@Override
		ForeignKeyExport node() {
			return (ForeignKeyExport) super.node();
		}

		boolean expandable() {
			return node().expandable();
		}

		void expand() {
			if (expandable()) {
				node().expand();
				populate(this, node().attributes().get());
			}
		}

		private int includedChildrenCount(MutableForeignKeyNode node) {
			int counter = 0;
			Enumeration<? extends TreeNode> children = node.children();
			while (children.hasMoreElements()) {
				MutableAttributeNode child = (MutableAttributeNode) children.nextElement();
				if (child.include().is()) {
					counter++;
				}
				if (child instanceof MutableForeignKeyNode) {
					counter += includedChildrenCount((MutableForeignKeyNode) child);
				}
			}

			return counter;
		}
	}

	private void populate(DefaultMutableTreeNode parent, List<AttributeExport> children) {
		parent.removeAllChildren();
		for (AttributeExport child : children) {
			if (child instanceof ForeignKeyExport) {
				parent.add(new MutableForeignKeyNode((ForeignKeyExport) child));
			}
			else {
				parent.add(new MutableAttributeNode(child));
			}
		}
	}

	private static final class AttributeExportComparator implements Comparator<AttributeExport> {

		private final Map<AttributeExport, Integer> indexes;

		private AttributeExportComparator(List<TreeNode> children) {
			indexes = new HashMap<>();
			int i = 0;
			for (TreeNode node : children) {
				indexes.put(((MutableAttributeNode) node).node(), i++);
			}
		}

		@Override
		public int compare(AttributeExport node1, AttributeExport node2) {
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
