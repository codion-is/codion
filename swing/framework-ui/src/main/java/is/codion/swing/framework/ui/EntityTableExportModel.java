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

import is.codion.common.model.CancelException;
import is.codion.common.reactive.state.State;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.model.EntityExport;
import is.codion.framework.model.EntityTableModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel.ComboBoxItems;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.swing.common.ui.Utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jspecify.annotations.Nullable;

import java.awt.Dimension;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.swing.framework.ui.EntityTableExportTreeModel.ENTITY_TYPE_KEY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

final class EntityTableExportModel {

	private static final String DIALOG_SIZE_KEY = "dialogSize";
	private static final String CONFIGURATION_FILES_KEY = "configurationFiles";
	private static final String SELECTED_CONFIGURATION_FILE_KEY = "selectedConfigurationFile";

	static final NullConfigurationFile NULL_CONFIGURATION_FILE = new NullConfigurationFile();
	static final String JSON = "json";

	private final EntityTableModel<?> tableModel;

	private final EntityConnectionProvider connectionProvider;
	private final FilterComboBoxModel<ConfigurationFile> configurationFiles;
	private final EntityTableExportTreeModel treeModel;
	private final State selected;
	private final State all;

	private @Nullable Dimension dialogSize;

	EntityTableExportModel(EntityTableModel<?> tableModel) {
		this.tableModel = tableModel;
		this.connectionProvider = tableModel.connectionProvider();
		this.treeModel = new EntityTableExportTreeModel(tableModel.entityDefinition().type(), tableModel.connectionProvider().entities());
		this.configurationFiles = FilterComboBoxModel.builder()
						.items(this::refreshConfigurationFiles)
						.nullItem(NULL_CONFIGURATION_FILE)
						.onSelection(this::configurationFileSelected)
						.build();
		this.selected = State.state(!tableModel.selection().empty().is());
		this.all = State.state(!selected.is());
		State.group(selected, all);
		this.tableModel.selection().empty().addConsumer(empty -> selected.set(!empty));
		this.treeModel.includeNone();
		this.treeModel.includeAll();
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

	EntityTableExportTreeModel treeModel() {
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

	State all() {
		return all;
	}

	String defaultExportFileName() {
		return tableModel.entityDefinition().caption();
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
			Files.write(file.toPath(), treeModel.toJson().toString(2).getBytes(UTF_8));
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

	private Collection<ConfigurationFile> refreshConfigurationFiles() {
		return configurationFiles.items().get().stream()
						.filter(DefaultConfigurationFile.class::isInstance)
						.filter(configurationFile -> configurationFile.file().exists())
						.map(configurationFile -> new DefaultConfigurationFile(configurationFile.file()))
						.collect(toList());
	}

	private void configurationFileSelected(@Nullable ConfigurationFile configurationFile) {
		if (configurationFile == null) {
			treeModel.showHidden().set(false);
			treeModel.includeNone();
			treeModel.includeAll();
		}
		else {
			treeModel.applyConfiguration(configurationFile.json());
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
			return file.getName().substring(0, file.getName().length() - JSON.length() - 1);
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
