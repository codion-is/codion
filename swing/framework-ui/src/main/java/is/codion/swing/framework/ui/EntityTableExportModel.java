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
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.EntityExport;
import is.codion.framework.model.EntityExport.ExportAttributes;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Comparator.comparing;
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
	private final AttributeNodeComparator attributeComparator;
	private final FilterComboBoxModel<ConfigurationFile> configurationFiles;
	private final ExportTreeModel treeModel;
	private final Event<?> configurationChanged = Event.event();
	private final State selected;

	private @Nullable Dimension dialogSize;

	EntityTableExportModel(EntityTableModel<?> tableModel) {
		this.tableModel = tableModel;
		this.attributeComparator = new AttributeNodeComparator(tableModel.entities());
		this.connectionProvider = tableModel.connectionProvider();
		this.treeModel = new ExportTreeModel(tableModel.entityDefinition().type(), tableModel.connectionProvider().entities());
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

	Entities entities() {
		return connectionProvider.entities();
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
			treeModel.refresh();
			populate(treeModel.getRoot(), ((DefaultConfigurationFile) configurationFile).json);
			configurationChanged.run();
		}
	}

	void includeAll() {
		include(Collections.list(treeModel.getRoot().children()), true);
		configurationChanged.run();
	}

	void includeNone() {
		treeModel.refresh();
		configurationChanged.run();
	}

	void includeDefault() {
		treeModel.refresh();
		Collections.list(treeModel.getRoot().children()).forEach(node ->
						((AttributeNode) node).include().set(true));
		configurationChanged.run();
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

	private static JSONObject attributesToJson(Enumeration<TreeNode> nodes) {
		JSONArray attributes = new JSONArray();
		while (nodes.hasMoreElements()) {
			AttributeNode node = (AttributeNode) nodes.nextElement();
			String attributeName = node.attribute().name();
			if (((DefaultMutableTreeNode) node).getChildCount() > 0) {
				JSONObject fkChildren = attributesToJson(((DefaultMutableTreeNode) node).children());
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

	private void populate(DefaultMutableTreeNode node, JSONObject json) {
		if (!json.has(ATTRIBUTES_KEY)) {
			return;
		}
		((EntityNode) node).populate();
		Enumeration<TreeNode> childNodes = node.children();
		Map<String, DefaultMutableTreeNode> children = new HashMap<>();
		while (childNodes.hasMoreElements()) {
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) childNodes.nextElement();
			children.put(((AttributeNode) child).attribute().name(), child);
		}
		for (Object jsonAttribute : json.getJSONArray(ATTRIBUTES_KEY)) {
			String attributeName = attributeName(jsonAttribute);
			DefaultMutableTreeNode child = children.get(attributeName);
			int index = node.getIndex(child);
			if (index >= 0) {
				node.remove(child);
			}
			node.add(child);
			treeModel.nodeStructureChanged(node);
			if (jsonAttribute instanceof String) {
				((AttributeNode) child).include().set(true);
			}
			else {
				populate(child, ((JSONObject) jsonAttribute).getJSONObject(attributeName));
			}
		}
		List<DefaultMutableTreeNode> sorted = Collections.list(node.children()).stream()
						.map(AttributeNode.class::cast)
						.sorted(attributeComparator)
						.map(DefaultMutableTreeNode.class::cast)
						.collect(toList());
		node.removeAllChildren();
		sorted.forEach(node::add);
		treeModel.nodeStructureChanged(node);
	}

	private static String attributeName(Object item) {
		if (item instanceof String) {
			return (String) item;
		}

		return ((JSONObject) item).keys().next();
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
				EntityExport.builder(connectionProvider)
								.entityType(tableModel.entityType())
								.attributes(treeModel::attributes)
								.entities(entities.iterator())
								.output(line -> write(line, output))
								.processed(entity -> progress.report(counter.incrementAndGet()))
								.cancel(cancel.observable())
								.export();
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
			EntityExport.builder(connectionProvider)
							.entityType(tableModel.entityType())
							.attributes(treeModel::attributes)
							.entities(entities.iterator())
							.output(builder::append)
							.processed(entity -> progress.report(counter.incrementAndGet()))
							.cancel(cancel.observable())
							.export();
			Utilities.setClipboard(builder.toString());
		}
	}

	static final class ExportTreeModel extends DefaultTreeModel {

		private final Entities entities;

		private ExportTreeModel(EntityType entityType, Entities entities) {
			super(null);
			this.entities = entities;
			RootNode rootNode = new RootNode(this, entityType);
			rootNode.populate();
			setRoot(rootNode);
		}

		@Override
		public DefaultMutableTreeNode getRoot() {
			return (DefaultMutableTreeNode) super.getRoot();
		}

		private ExportAttributes attributes(ExportAttributes.Builder attributes) {
			RootNode rootNode = (RootNode) getRoot();
			rootNode.populate(attributes);

			return attributes.build();
		}

		private void refresh() {
			((RootNode) getRoot()).refresh();
			reload();
		}
	}

	abstract static class EntityNode extends DefaultMutableTreeNode {

		private final EntityType entityType;
		private final ExportTreeModel treeModel;

		protected boolean populated;

		protected EntityNode(EntityType entityType, ExportTreeModel treeModel) {
			this.entityType = entityType;
			this.treeModel = treeModel;
		}

		final void populate() {
			if (!populated) {
				removeAllChildren();
				treeModel.entities.definition(entityType).attributes().definitions().stream()
								.filter(attributeDefinition -> !attributeDefinition.hidden())
								.sorted(comparing(AttributeDefinition::caption))
								.map(AttributeDefinition::attribute)
								.forEach(this::addNode);
				treeModel.nodeStructureChanged(this);
				populated = true;
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

		private void addNode(Attribute<?> attribute) {
			if (attribute instanceof ForeignKey) {
				super.add(new MutableForeignKeyNode(treeModel, (ForeignKey) attribute));
			}
			else {
				super.add(new MutableAttributeNode(treeModel, attribute));
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

	private static class RootNode extends EntityNode {

		private RootNode(ExportTreeModel treeModel, EntityType entityType) {
			super(entityType, treeModel);
		}

		private void refresh() {
			populated = false;
			super.populate();
		}
	}

	interface AttributeNode {

		Attribute<?> attribute();

		State include();
	}

	static final class MutableAttributeNode extends DefaultMutableTreeNode implements AttributeNode {

		private final Attribute<?> attribute;
		private final State include;

		private MutableAttributeNode(DefaultTreeModel treeModel, Attribute<?> attribute) {
			this.attribute = attribute;
			this.include = State.builder()
							.listener(new NodeChanged(treeModel, this))
							.build();
		}

		public Attribute<?> attribute() {
			return attribute;
		}

		public State include() {
			return include;
		}
	}

	static final class MutableForeignKeyNode extends EntityNode implements AttributeNode {

		private final ForeignKey foreignKey;
		private final State include;

		private MutableForeignKeyNode(ExportTreeModel treeModel, ForeignKey foreignKey) {
			super(foreignKey.referencedType(), treeModel);
			this.foreignKey = foreignKey;
			this.include = State.builder()
							.listener(new NodeChanged(treeModel, this))
							.build();
		}

		@Override
		public boolean isLeaf() {
			return false;
		}

		@Override
		public ForeignKey attribute() {
			return foreignKey;
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

	private static class AttributeNodeComparator implements Comparator<AttributeNode> {

		private final Entities entities;

		private AttributeNodeComparator(Entities entities) {
			this.entities = entities;
		}

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

			return entities.definition(o1.attribute().entityType()).attributes().definition(o1.attribute()).caption()
							.compareTo(entities.definition(o2.attribute().entityType()).attributes().definition(o2.attribute()).caption());
		}

		private static boolean included(AttributeNode node) {
			if (node instanceof MutableForeignKeyNode) {
				return node.include().is() || ((MutableForeignKeyNode) node).includedCount() > 0;
			}

			return node.include().is();
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
