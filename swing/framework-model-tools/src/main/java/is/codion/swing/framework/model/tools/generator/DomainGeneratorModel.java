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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.common.value.Value.Notify;
import is.codion.common.value.ValueObserver;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Columns;
import is.codion.swing.framework.model.tools.metadata.MetaDataModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static is.codion.common.Configuration.stringValue;
import static is.codion.common.Text.nullOrEmpty;
import static is.codion.swing.framework.model.tools.generator.DomainToString.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	/**
	 * The default package.
	 */
	public static final PropertyValue<String> DEFAULT_DOMAIN_PACKAGE =
					stringValue("codion.domain.generator.defaultDomainPackage", "");

	/**
	 * The default source directory.
	 */
	public static final PropertyValue<String> DEFAULT_SOURCE_DIRECTORY =
					stringValue("codion.domain.generator.defaultSourceDirectory", System.getProperty("user.dir"));

	private static final Pattern PACKAGE_PATTERN =
					Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*$");

	private final MetaDataModel metaDataModel;
	private final FilterTableModel<SchemaRow, SchemaColumns.Id> schemaTableModel =
					FilterTableModel.builder(new SchemaColumns())
									.items(new SchemaItems())
									.build();
	private final FilterTableModel<EntityRow, EntityColumns.Id> entityTableModel =
					FilterTableModel.builder(new EntityColumns())
									.items(new EntityItems())
									.build();
	private final Connection connection;
	private final Value<String> domainPackageValue = Value.nonNull(DEFAULT_DOMAIN_PACKAGE.get()).build();
	private final Value<String> sourceDirectoryValue = Value.nonNull(DEFAULT_SOURCE_DIRECTORY.get()).build();
	private final State domainPackageSpecified = State.state();
	private final State sourceDirectorySpecified = State.state();
	private final Value<String> domainImplValue = Value.<String>nullable()
					.notify(Notify.WHEN_SET)
					.build();
	private final Value<String> domainApiValue = Value.<String>nullable()
					.notify(Notify.WHEN_SET)
					.build();
	private final Value<String> domainCombinedValue = Value.<String>nullable()
					.notify(Notify.WHEN_SET)
					.build();
	private final Value<String> apiSearchValue = Value.value();
	private final Value<String> implSearchValue = Value.value();

	private DomainGeneratorModel(Database database, User user) throws DatabaseException {
		this.connection = requireNonNull(database, "database").createConnection(user);
		try {
			this.metaDataModel = new MetaDataModel(connection.getMetaData());
			this.schemaTableModel.refresh();
			bindEvents();
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}

	public FilterTableModel<SchemaRow, SchemaColumns.Id> schemaModel() {
		return schemaTableModel;
	}

	public FilterTableModel<EntityRow, EntityColumns.Id> entityModel() {
		return entityTableModel;
	}

	public ValueObserver<String> domainImpl() {
		return domainImplValue.observer();
	}

	public ValueObserver<String> domainApi() {
		return domainApiValue.observer();
	}

	public ValueObserver<String> domainCombined() {
		return domainCombinedValue.observer();
	}

	public Value<String> domainPackage() {
		return domainPackageValue;
	}

	public Value<String> sourceDirectory() {
		return sourceDirectoryValue;
	}

	public Value<String> apiSearchValue() {
		return apiSearchValue;
	}

	public Value<String> implSearchValue() {
		return implSearchValue;
	}

	public void close() {
		try {
			connection.close();
		}
		catch (Exception ignored) {/*ignored*/}
	}

	public void populateSelected(Consumer<String> schemaNotifier) {
		schemaTableModel.selectionModel().selectedItem().ifPresent(schema -> {
			metaDataModel.populateSchema(schema.name(), schemaNotifier);
			schema.setDomain(new DatabaseDomain(schema.metadata));
			int index = schemaTableModel.indexOf(schema);
			schemaTableModel.fireTableRowsUpdated(index, index);
		});
		entityTableModel.refresh();
		updateDomainSource();
	}

	public void saveApiImpl() throws IOException {
		if (schemaTableModel.selectionModel().selectionNotEmpty().get()) {
			SchemaRow schema = schemaTableModel.selectionModel().getSelectedItem();
			saveApiImpl(savePath(new File(sourceDirectoryValue.get())), schema);
		}
	}

	public void saveCombined() throws IOException {
		if (schemaTableModel.selectionModel().selectionNotEmpty().get()) {
			SchemaRow schema = schemaTableModel.selectionModel().getSelectedItem();
			saveCombined(savePath(new File(sourceDirectoryValue.get())), schema);
		}
	}

	public StateObserver saveEnabled() {
		return State.and(domainPackageSpecified, sourceDirectorySpecified);
	}

	private void bindEvents() {
		schemaTableModel.selectionModel().selectionEvent().addListener(entityTableModel::refresh);
		schemaTableModel.selectionModel().selectionEvent().addListener(this::updateDomainSource);
		domainPackageValue.addListener(this::updateDomainSource);
		domainPackageValue.addConsumer(pkg -> domainPackageSpecified.set(!nullOrEmpty(pkg)));
		sourceDirectoryValue.addConsumer(dir -> sourceDirectorySpecified.set(nonNull(dir)));
		entityModel().selectionModel().selectedItemEvent().addConsumer(this::search);
	}

	private void search(EntityRow entityRow) {
		apiSearchValue.set(entityRow == null ? null : apiSearchString(entityRow.definition));
		implSearchValue.set(entityRow == null ? null : implSearchString(entityRow.definition));
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance.
	 * @param database the database to connect to
	 * @param user the user to connect with
	 * @return a new {@link DomainGeneratorModel} instance
	 * @throws DatabaseException in case of an exception while connecting to the database
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database, User user) throws DatabaseException {
		return new DomainGeneratorModel(database, user);
	}

	private void updateDomainSource() {
		String packageName = domainPackageValue.get();
		if (!PACKAGE_PATTERN.matcher(packageName).matches()) {
			packageName = "";
		}
		domainApiValue.set(DomainToString.apiString(selectedDomains(), packageName));
		domainImplValue.set(DomainToString.implementationString(selectedDomains(), packageName));
		domainCombinedValue.set(DomainToString.combinedString(selectedDomains(), packageName));
	}

	private List<DatabaseDomain> selectedDomains() {
		return schemaTableModel.selectionModel().getSelectedItems().stream()
						.map(SchemaRow::domain)
						.filter(Optional::isPresent)
						.map(Optional::get)
						.collect(toList());
	}

	private Path savePath(File directory) {
		Path path = directory.toPath();
		String domainPackage = domainPackageValue.get();
		if (domainPackage != null) {
			for (String pkg : domainPackage.split("\\.")) {
				path = path.resolve(pkg);
			}
		}

		return path;
	}

	private void saveApiImpl(Path path, SchemaRow schema) throws IOException {
		String interfaceName = interfaceName(schema.domainModel.type().name(), true);
		Files.createDirectories(path);
		Path filePath = path.resolve(interfaceName + ".java");
		Files.write(filePath, singleton(domainApiValue.get()));
		path = path.resolve("impl");
		Files.createDirectories(path);
		filePath = path.resolve(interfaceName + "Impl.java");
		Files.write(filePath, singleton(domainImplValue.get()));
	}

	private void saveCombined(Path path, SchemaRow schema) throws IOException {
		String interfaceName = interfaceName(schema.domainModel.type().name(), true);
		Files.createDirectories(path);
		Files.write(path.resolve(interfaceName + ".java"), singleton(domainCombinedValue.get()));
	}

	private final class SchemaItems implements Supplier<Collection<SchemaRow>> {

		@Override
		public Collection<SchemaRow> get() {
			return metaDataModel.schemas()
							.stream()
							.map(metaDataSchema ->
											new SchemaRow(metaDataSchema, metaDataSchema.catalog(), metaDataSchema.name(), false))
							.collect(Collectors.toList());
		}
	}

	private final class EntityItems implements Supplier<Collection<EntityRow>> {

		@Override
		public Collection<EntityRow> get() {
			return schemaTableModel.selectionModel().getSelectedItems().stream()
							.map(SchemaRow::domain)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.flatMap(domain -> domain.entities().definitions().stream()
											.map(definition -> new EntityRow(definition, domain.tableType(definition.entityType()))))
							.collect(toList());
		}
	}

	public static final class SchemaColumns implements Columns<SchemaRow, SchemaColumns.Id> {

		public enum Id {
			CATALOG,
			SCHEMA,
			POPULATED
		}

		private static final List<Id> IDENTIFIERS = unmodifiableList(asList(Id.values()));

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			if (identifier == Id.POPULATED) {
				return Boolean.class;
			}

			return String.class;
		}

		@Override
		public Object value(SchemaRow row, Id identifier) {
			switch (identifier) {
				case CATALOG:
					return row.catalog();
				case SCHEMA:
					return row.name();
				case POPULATED:
					return row.populated();
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}

	public static final class EntityColumns implements Columns<EntityRow, EntityColumns.Id> {

		public enum Id {
			ENTITY,
			TABLE_TYPE
		}

		private static final List<Id> IDENTIFIERS = unmodifiableList(asList(Id.values()));

		@Override
		public List<Id> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(Id identifier) {
			return String.class;
		}

		@Override
		public Object value(EntityRow row, Id identifier) {
			switch (identifier) {
				case ENTITY:
					return row.definition.entityType().name();
				case TABLE_TYPE:
					return row.tableType;
				default:
					throw new IllegalArgumentException("Unknown column: " + identifier);
			}
		}
	}
}
