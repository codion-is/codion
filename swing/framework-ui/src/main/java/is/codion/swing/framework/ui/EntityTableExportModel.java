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

import is.codion.common.model.component.combobox.FilterComboBoxModel;
import is.codion.common.model.component.combobox.FilterComboBoxModel.ComboBoxItems;
import is.codion.common.model.component.tree.NodePath;
import is.codion.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.common.model.worker.ProgressWorker.ProgressResultTask;
import is.codion.common.reactive.event.Event;
import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.resource.MessageBundle;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.model.EntityExport;
import is.codion.framework.model.EntityExport.Attributes;
import is.codion.framework.model.EntityTableModel;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.common.utilities.resource.MessageBundle.messageBundle;
import static is.codion.swing.framework.ui.EntityTableExportColumns.ENTITY_TYPE_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.ResourceBundle.getBundle;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;

final class EntityTableExportModel {

	private static final MessageBundle MESSAGES =
					messageBundle(EntityTableExportPanel.class, getBundle(EntityTableExportPanel.class.getName()));

	private static final String CONFIGURATION_FILES_KEY = "configurationFiles";
	private static final String SELECTED_CONFIGURATION_FILE_KEY = "selectedConfigurationFile";

	static final NullConfigurationFile NULL_CONFIGURATION_FILE = new NullConfigurationFile();
	static final String JSON = "json";

	private final EntityTableModel<?, ?> tableModel;

	private final EntityConnection connection;
	private final FilterComboBoxModel<ConfigurationFile> configurationFiles;
	private final EntityTableExportColumns columns;
	private final State selected;
	private final State all;
	private final Event<?> rowCounts = Event.event();
	private final State modified = State.state();

	//the configuration selected as last applied or saved, the default columns in case of none
	private JSONObject saved = new JSONObject();

	/**
	 * @param tableModel the table model
	 * @param defaultColumns supplies the default columns, the visible table columns
	 */
	EntityTableExportModel(EntityTableModel<?, ?> tableModel, Supplier<List<Attribute<?>>> defaultColumns) {
		this.tableModel = tableModel;
		this.connection = tableModel.connection();
		this.columns = new EntityTableExportColumns(tableModel.entityDefinition().type(),
						tableModel.connection().entities(), defaultColumns);
		this.configurationFiles = FilterComboBoxModel.builder()
						.items(this::refreshConfigurationFiles)
						.nullItem(NULL_CONFIGURATION_FILE)
						.onSelectedItem(this::configurationFileSelected)
						.build();
		this.selected = State.state(tableModel.selection().present().is());
		this.all = State.state(!selected.is());
		State.group(selected, all).fallback(all);
		this.tableModel.selection().present().addConsumer(selected::set);
		this.tableModel.selection().indexes().addListener(rowCounts::run);
		this.tableModel.items().included().addListener(rowCounts::run);
		this.columns.changed().addListener(this::columnsChanged);
		this.columns.reset();
		saved();
	}

	/**
	 * Resets the columns to the current default ones, in case no configuration is selected.
	 */
	void initialize() {
		if (configurationFiles.selection().item().isNull()) {
			columns.reset();
			saved();
		}
	}

	ExportTask<String> exportToString() {
		//snapshot the rows, the task drains the iterator on a background thread while the EDT keeps
		//pumping events, and the all-rows path would otherwise iterate a live view of the table items
		List<Entity> entities = exportEntities();

		return new ExportTask.ExportToString(connection, tableModel.entityType(),
						columns::attributes, entities.iterator(), entities.size());
	}

	ExportTask<Path> exportToFile(Path file) {
		List<Entity> entities = exportEntities();

		return new ExportTask.ExportToFileTask(connection, tableModel.entityType(),
						columns::attributes, requireNonNull(file), entities.iterator(), entities.size());
	}

	EntityTableExportColumns columns() {
		return columns;
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

	State all() {
		return all;
	}

	/**
	 * @return the number of rows exported when all rows are exported, the ones the table includes
	 */
	int allCount() {
		return tableModel.items().included().size();
	}

	/**
	 * @return the number of selected rows
	 */
	int selectedCount() {
		return tableModel.selection().count();
	}

	/**
	 * @return notified when the row counts may have changed
	 */
	Observer<?> rowCounts() {
		return rowCounts.observer();
	}

	/**
	 * @return true while the columns or their headers differ from the ones of the selected configuration, as last
	 * applied or saved, or the default ones in case of none
	 */
	ObservableState modified() {
		return modified.observable();
	}

	String defaultExportFileName() {
		return tableModel.entityDefinition().caption();
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

	/**
	 * Removes the selected configuration file from the list, the file itself left as is.
	 */
	void removeConfigurationFile() {
		configurationFiles.selection().item().optional().ifPresent(configurationFiles.items()::remove);
	}

	void restore(JSONObject preferences) {
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
	}

	void writeConfig(File file) {
		try {
			Files.write(file.toPath(), columns.toJson().toString(2).getBytes(UTF_8));
			saved();
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

	private List<Entity> exportEntities() {
		return new ArrayList<>(selected.is() ?
						tableModel.selection().items().get() :
						tableModel.items().included().get());
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

		return json;
	}

	private Collection<ConfigurationFile> refreshConfigurationFiles() {
		return configurationFiles.items().get().stream()
						.filter(DefaultConfigurationFile.class::isInstance)
						.filter(configurationFile -> configurationFile.file().exists())
						.map(configurationFile -> new DefaultConfigurationFile(configurationFile.file()))
						.collect(toList());
	}

	private void configurationFileSelected(@Nullable ConfigurationFile configurationFile) {
		List<NodePath<AttributeDefinition<?>>> previous = new ArrayList<>(columns.table().items().get());
		if (configurationFile == null) {
			columns.reset();
		}
		else {
			columns.apply(configurationFile.json());
		}
		//the columns unchanged when a saved configuration is selected, leaving the tree as is
		if (!previous.equals(columns.table().items().get())) {
			columns.expand();
		}
		saved();
	}

	private void columnsChanged() {
		modified.set(!saved.similar(columns.toJson()));
	}

	private void saved() {
		saved = columns.toJson();
		modified.set(false);
	}

	private void validateEntityType(List<DefaultConfigurationFile> files) {
		List<File> incorrectEntityType = files.stream()
						.filter(file -> !entityDefinition().type().name().equals(file.entityType))
						.map(DefaultConfigurationFile::file)
						.collect(toList());
		if (!incorrectEntityType.isEmpty()) {
			throw new IllegalArgumentException(MessageFormat.format(MESSAGES.getString("wrong_entity_type"),
							entityDefinition().caption(), incorrectEntityType.stream()
											.map(File::getAbsolutePath)
											.collect(joining("\n"))));
		}
	}

	abstract static class ExportTask<T> implements ProgressResultTask<T, Void> {

		protected final EntityConnection connection;
		protected final EntityType entityType;
		protected final Consumer<Attributes> attributes;
		protected final Iterator<Entity> entities;
		protected final int maximum;

		protected final AtomicInteger counter = new AtomicInteger();
		protected final State cancel = State.state(false);

		protected ExportTask(EntityConnection connection, EntityType entityType,
		                     Consumer<Attributes> attributes,
		                     Iterator<Entity> entities, int maximum) {
			this.connection = connection;
			this.entityType = entityType;
			this.attributes = attributes;
			this.entities = entities;
			this.maximum = maximum;
		}

		@Override
		public final int maximum() {
			return maximum;
		}

		final State cancel() {
			return cancel;
		}

		private static final class ExportToFileTask extends ExportTask<Path> {

			private final Path file;

			private ExportToFileTask(EntityConnection connection, EntityType entityType,
			                         Consumer<Attributes> attributes,
			                         Path file, Iterator<Entity> entities, int maximum) {
				super(connection, entityType, attributes, entities, maximum);
				this.file = file;
			}

			@Override
			public Path execute(ProgressReporter<Void> progress) throws Exception {
				try (BufferedWriter output = Files.newBufferedWriter(file)) {
					EntityExport.builder(connection)
									.entityType(entityType)
									.attributes(attributes)
									.entities(entities)
									.output(line -> write(line, output))
									.processed(entity -> progress.report(counter.incrementAndGet()))
									.cancel(cancel.observable())
									.export();
				}
				catch (Exception e) {
					Files.deleteIfExists(file);
					throw e;
				}

				return file;
			}

			private static void write(String line, BufferedWriter output) {
				try {
					output.write(line);
				}
				catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			}
		}

		private static final class ExportToString extends ExportTask<String> {

			private ExportToString(EntityConnection connection, EntityType entityType,
														 Consumer<Attributes> attributes,
														 Iterator<Entity> entities, int maximum) {
				super(connection, entityType, attributes, entities, maximum);
			}

			@Override
			public String execute(ProgressReporter<Void> progress) {
				StringBuilder builder = new StringBuilder();
				EntityExport.builder(connection)
								.entityType(entityType)
								.attributes(attributes)
								.entities(entities)
								.output(builder::append)
								.processed(entity -> progress.report(counter.incrementAndGet()))
								.cancel(cancel.observable())
								.export();

				return builder.toString();
			}
		}
	}

	interface ConfigurationFile {

		File file();

		String filename();

		JSONObject json();
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
		public JSONObject json() {
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
			String name = file.getName();
			String suffix = "." + JSON;

			return name.toLowerCase(Locale.ROOT).endsWith(suffix) ? name.substring(0, name.length() - suffix.length()) : name;
		}

		@Override
		public JSONObject json() {
			return json;
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
