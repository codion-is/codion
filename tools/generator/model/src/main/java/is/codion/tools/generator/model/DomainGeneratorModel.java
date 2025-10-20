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
 * Copyright (c) 2020 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import is.codion.common.db.database.Database;
import is.codion.common.model.filter.FilterModel.IncludedItems;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.observer.Observable;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.domain.db.SchemaDomain;
import is.codion.framework.domain.db.SchemaDomain.SchemaSettings;
import is.codion.framework.domain.entity.EntityType;
import is.codion.swing.common.model.component.table.FilterTableModel;
import is.codion.swing.common.model.component.table.FilterTableModel.Editor;
import is.codion.swing.common.model.component.table.FilterTableModel.TableColumns;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressReporter;
import is.codion.swing.common.model.worker.ProgressWorker.ProgressTask;
import is.codion.tools.generator.domain.DomainSource;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.regex.Pattern;

import static is.codion.common.Configuration.stringValue;
import static is.codion.common.Text.nullOrEmpty;
import static is.codion.common.value.Value.Notify.SET;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.*;

/**
 * For instances use the factory method {@link #domainGeneratorModel(Database, User)}.
 */
public final class DomainGeneratorModel {

	/**
	 * The default domain package.
	 */
	public static final PropertyValue<String> DOMAIN_PACKAGE =
					stringValue("codion.domain.generator.domainPackage", "no.package");

	/**
	 * The combined source directory, relative to the user directory.
	 * <p>Default "combined".
	 */
	public static final PropertyValue<String> COMBINED_SOURCE_DIRECTORY =
					stringValue("codion.domain.generator.combinedSourceDirectory", "combined");

	/**
	 * The api source directory, relative to the user directory, used when saving the domain files when split into api/impl
	 * <p>Default "api".
	 */
	public static final PropertyValue<String> API_SOURCE_DIRECTORY =
					stringValue("codion.domain.generator.apiSourceDirectory", "api");

	/**
	 * The implementation source directory, relative to the user directory, used when saving the domain files when split into api/impl
	 * <p>Default "impl".
	 */
	public static final PropertyValue<String> IMPL_SOURCE_DIRECTORY =
					stringValue("codion.domain.generator.implSourceDirectory", "impl");

	private static final Preferences PREFERENCES = UserPreferences.file(DomainGeneratorModel.class.getName());
	private static final Pattern PACKAGE_PATTERN =
					Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*(?:\\.[A-Za-z_][A-Za-z0-9_]*)*$");
	private static final SchemaSettings DEFAULT_SCHEMA_SETTINGS = SchemaSettings.builder().build();
	private static final String HIDE_AUDIT_COLUMNS = "hideAuditColumns";
	private static final String AUDIT_COLUMN_NAMES = "auditColumnNames";
	private static final String PRIMARY_KEY_COLUMN_SUFFIX = "primaryKeyColumnSuffix";
	private static final String VIEW_SUFFIX = "viewSuffix";
	private static final String VIEW_PREFIX = "viewPrefix";
	private static final String LOWER_CASE_IDENTIFIERS = "lowerCaseIdentifiers";

	private final FilterTableModel<SchemaRow, String> schemaTableModel =
					FilterTableModel.builder()
									.columns(new SchemaColumns())
									.items(new SchemaItems())
									.build();
	private final State populatedSchemaSelected = State.state();
	private final FilterTableModel<EntityRow, String> entityTableModel =
					FilterTableModel.builder()
									.columns(new EntityColumns())
									.items(new EntityItems())
									.editor(EntityEditor::new)
									.build();
	private final Database database;
	private final User user;
	private final State dtos = State.builder()
					.listener(this::updateDomainSource)
					.build();
	private final State i18n = State.builder()
					.listener(this::updateDomainSource)
					.build();
	private final State domainPackageSpecified = State.state();
	private final Value<String> domainPackageValue = Value.builder()
					.nonNull(DOMAIN_PACKAGE.getOrThrow())
					.listener(this::domainPackageChanged)
					.build();
	private final Value<String> combinedSourceDirectoryValue = Value.builder()
					.nonNull(COMBINED_SOURCE_DIRECTORY.getOrThrow())
					.build();
	private final ObservableState combinedSourceDirectorySpecified = State.present(combinedSourceDirectoryValue);
	private final Value<String> apiSourceDirectoryValue = Value.builder()
					.nonNull(API_SOURCE_DIRECTORY.getOrThrow())
					.build();
	private final ObservableState apiSourceDirectorySpecified = State.present(apiSourceDirectoryValue);
	private final Value<String> implSourceDirectoryValue = Value.builder()
					.nonNull(IMPL_SOURCE_DIRECTORY.getOrThrow())
					.build();
	private final ObservableState implSourceDirectorySpecified = State.present(implSourceDirectoryValue);
	private final Value<String> domainImplValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> domainApiValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> domainCombinedValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> i18nPropertiesValue = Value.builder()
					.<String>nullable()
					.notify(SET)
					.build();
	private final Value<String> apiSearchValue = Value.nullable();
	private final Value<String> implSearchValue = Value.nullable();
	private final Value<String> i18nSearchValue = Value.nullable();

	private DomainGeneratorModel(Database database, User user) {
		this.database = requireNonNull(database);
		this.user = user;
		domainPackageChanged();
		schemaTableModel.sort().ascending(SchemaColumns.SCHEMA);
		entityTableModel.sort().ascending(EntityColumns.ENTITY);
		schemaTableModel.items().refresh();
		bindEvents();
	}

	public FilterTableModel<SchemaRow, String> schemaModel() {
		return schemaTableModel;
	}

	public FilterTableModel<EntityRow, String> entityModel() {
		return entityTableModel;
	}

	public Observable<String> domainImpl() {
		return domainImplValue.observable();
	}

	public Observable<String> domainApi() {
		return domainApiValue.observable();
	}

	public Observable<String> domainCombined() {
		return domainCombinedValue.observable();
	}

	public Observable<String> i18nProperties() {
		return i18nPropertiesValue.observable();
	}

	public State dtos() {
		return dtos;
	}

	public State i18n() {
		return i18n;
	}

	public Value<String> domainPackage() {
		return domainPackageValue;
	}

	public Value<String> combinedSourceDirectory() {
		return combinedSourceDirectoryValue;
	}

	public Value<String> apiSourceDirectory() {
		return apiSourceDirectoryValue;
	}

	public Value<String> implSourceDirectory() {
		return implSourceDirectoryValue;
	}

	public Value<String> apiSearchValue() {
		return apiSearchValue;
	}

	public Value<String> implSearchValue() {
		return implSearchValue;
	}

	public Value<String> i18nSearchValue() {
		return i18nSearchValue;
	}

	public PopulateTask populate() {
		return new PopulateTask();
	}

	public void setSchemaSettings(SchemaSettings schemaSettings) {
		schemaTableModel.selection().item().optional().ifPresent(schema -> {
			schema.setSchemaSettings(schemaSettings);
			writeSchemaSettings(schema.name(), schemaSettings);
			schema.setDomain(schemaDomain(schema));
			schemaSelectionChanged();
		});
	}

	public boolean saveApiImpl(BooleanSupplier overwrite) throws IOException {
		SchemaDomain domain = selectedDomain();
		if (domain != null) {
			return domainSource(domain)
							.writeApiImpl(
											sourcePath(Path.of(apiSourceDirectoryValue.getOrThrow()), "java"),
											sourcePath(Path.of(implSourceDirectoryValue.getOrThrow()), "java"),
											sourcePath(Path.of(apiSourceDirectoryValue.getOrThrow()), "resources"), overwrite);
		}

		return false;
	}

	public boolean saveCombined(BooleanSupplier overwrite) throws IOException {
		SchemaDomain domain = selectedDomain();
		if (domain != null) {
			return domainSource(domain)
							.writeCombined(
											sourcePath(Path.of(combinedSourceDirectoryValue.getOrThrow()), "java"),
											sourcePath(Path.of(combinedSourceDirectoryValue.getOrThrow()), "resources"), overwrite);
		}

		return false;
	}

	public ObservableState combinedSaveEnabled() {
		return State.and(domainPackageSpecified, combinedSourceDirectorySpecified, populatedSchemaSelected);
	}

	public ObservableState apiImplSaveEnabled() {
		return State.and(domainPackageSpecified, apiSourceDirectorySpecified, implSourceDirectorySpecified, populatedSchemaSelected);
	}

	private void bindEvents() {
		schemaTableModel.selection().indexes().addListener(this::schemaSelectionChanged);
		entityModel().selection().item().addConsumer(this::search);
	}

	private SchemaDomain schemaDomain(SchemaRow schema) {
		try (Connection connection = createConnection()) {
			return SchemaDomain.schemaDomain(connection.getMetaData(), schema.name(), schema.schemaSettings());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Connection createConnection() {
		return user == null ? database.createConnection() : database.createConnection(user);
	}

	private void schemaSelectionChanged() {
		entityTableModel.items().refresh();
		updateDomainSource();
		populatedSchemaSelected.set(schemaTableModel.selection().item().optional()
						.map(SchemaRow::populated)
						.orElse(false));
	}

	private void search(EntityRow entityRow) {
		apiSearchValue.set(entityRow == null ? null : DomainSource.apiSearchString(entityRow.definition));
		implSearchValue.set(entityRow == null ? null : DomainSource.implSearchString(entityRow.definition));
		i18nSearchValue.set(entityRow == null ? null : DomainSource.i18nSearchString(entityRow.definition));
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance, assuming the underlying database does not require a user to connect.
	 * @param database the database to connect to
	 * @return a new {@link DomainGeneratorModel} instance
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database) {
		return new DomainGeneratorModel(database, null);
	}

	/**
	 * Instantiates a new {@link DomainGeneratorModel} instance.
	 * @param database the database to connect to
	 * @param user the user to connect with
	 * @return a new {@link DomainGeneratorModel} instance
	 */
	public static DomainGeneratorModel domainGeneratorModel(Database database, User user) {
		return new DomainGeneratorModel(database, requireNonNull(user));
	}

	private void domainPackageChanged() {
		domainPackageSpecified.set(!nullOrEmpty(domainPackageValue.get()));
		updateDomainSource();
	}

	private void updateDomainSource() {
		SchemaDomain domain = selectedDomain();
		if (domain != null) {
			DomainSource domainSource = domainSource(domain);
			domainApiValue.set(domainSource.api());
			domainImplValue.set(domainSource.implementation());
			domainCombinedValue.set(domainSource.combined());
			i18nPropertiesValue.set(domainSource.i18n());
		}
		else {
			domainApiValue.clear();
			domainImplValue.clear();
			domainCombinedValue.clear();
			i18nPropertiesValue.clear();
		}
	}

	private DomainSource domainSource(SchemaDomain domain) {
		return DomainSource.builder()
						.domain(domain)
						.domainPackage(domainPackageValue.optional()
										.filter(DomainGeneratorModel::validPackageName).orElse(""))
						.dtos(dtoEntities())
						.i18n(i18n.is())
						.build();
	}

	private Set<EntityType> dtoEntities() {
		return dtos.is() ? entityTableModel.items().get()
						.stream()
						.filter(entityRow -> entityRow.dto)
						.map(entityRow -> entityRow.definition.type())
						.collect(toSet()) : emptySet();
	}

	private SchemaDomain selectedDomain() {
		return schemaTableModel.selection().item().optional()
						.flatMap(SchemaRow::domain)
						.orElse(null);
	}

	private Path sourcePath(Path path, String dir) {
		path = path.resolve("src/main/" + dir);
		String domainPackage = domainPackageValue.get();
		if (domainPackage != null) {
			for (String pkg : domainPackage.split("\\.")) {
				path = path.resolve(pkg);
			}
		}

		return path;
	}

	private void writeSchemaSettings(String tableSchem, SchemaSettings schemaSettings) {
		JSONObject json = new JSONObject();
		json.put(HIDE_AUDIT_COLUMNS, schemaSettings.hideAuditColumns());
		json.put(AUDIT_COLUMN_NAMES, schemaSettings.auditColumnNames().stream().collect(joining(",")));
		json.put(PRIMARY_KEY_COLUMN_SUFFIX, schemaSettings.primaryKeyColumnSuffix());
		json.put(VIEW_SUFFIX, schemaSettings.viewSuffix());
		json.put(VIEW_PREFIX, schemaSettings.viewPrefix());
		json.put(LOWER_CASE_IDENTIFIERS, schemaSettings.lowerCaseIdentifiers());

		PREFERENCES.put(database.name() + "." + tableSchem, json.toString());
		try {
			PREFERENCES.flush();
		}
		catch (BackingStoreException e) {
			throw new RuntimeException(e);
		}
	}

	private static boolean validPackageName(String packageName) {
		return PACKAGE_PATTERN.matcher(packageName).matches();
	}

	public final class PopulateTask implements ProgressTask<String> {

		private final State cancelled = State.state();

		@Override
		public void execute(ProgressReporter<String> progress) throws Exception {
			AtomicInteger counter = new AtomicInteger();
			schemaTableModel.selection().items().get().forEach(schema -> {
				if (!cancelled.is()) {
					progress.publish(schema.name());
					schema.setDomain(schemaDomain(schema));
					progress.report(counter.incrementAndGet());
				}
			});
		}

		@Override
		public int maximum() {
			return schemaModel().selection().count();
		}

		public State cancelled() {
			return cancelled;
		}

		public void finish() {
			schemaTableModel.selection().indexes().get().forEach(index ->
							schemaTableModel.fireTableRowsUpdated(index, index));
			schemaSelectionChanged();
		}
	}

	private final class SchemaItems implements Supplier<Collection<SchemaRow>> {

		@Override
		public Collection<SchemaRow> get() {
			try (Connection connection = createConnection();
					 ResultSet resultSet = connection.getMetaData().getSchemas()) {
				return schemaRows(resultSet);
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
		}

		private Collection<SchemaRow> schemaRows(ResultSet resultSet) throws SQLException {
			Collection<SchemaRow> schemaRows = new ArrayList<>();
			while (resultSet.next()) {
				String tableSchem = resultSet.getString("TABLE_SCHEM");
				if (tableSchem != null) {
					schemaRows.add(new SchemaRow(resultSet.getString("TABLE_CATALOG"), tableSchem, loadSchemaSettings(tableSchem)));
				}
			}
			if (schemaRows.isEmpty()) {
				schemaRows.add(new SchemaRow(DEFAULT_SCHEMA_SETTINGS));
			}

			return schemaRows;
		}

		private SchemaSettings loadSchemaSettings(String tableSchem) {
			JSONObject json = new JSONObject(PREFERENCES.get(database.name() + "." + tableSchem, "{}"));

			return SchemaSettings.builder()
							.hideAuditColumns(json.optBoolean(HIDE_AUDIT_COLUMNS, false))
							.auditColumnNames(json.has(AUDIT_COLUMN_NAMES) ? json.getString(AUDIT_COLUMN_NAMES).split(",") : new String[0])
							.primaryKeyColumnSuffix(json.optString(PRIMARY_KEY_COLUMN_SUFFIX, null))
							.viewSuffix(json.optString(VIEW_SUFFIX, null))
							.viewPrefix(json.optString(VIEW_PREFIX, null))
							.lowerCaseIdentifiers(json.optBoolean(LOWER_CASE_IDENTIFIERS, true))
							.build();
		}
	}

	private final class EntityItems implements Supplier<Collection<EntityRow>> {

		@Override
		public Collection<EntityRow> get() {
			return schemaTableModel.selection().item().optional()
							.map(SchemaRow::domain)
							.filter(Optional::isPresent)
							.map(Optional::get)
							.map(domain -> domain.entities().definitions().stream()
											.map(definition -> new EntityRow(definition, domain.tableType(definition.type()), false))
											.collect(toList()))
							.orElse(emptyList());
		}
	}

	private final class EntityEditor implements Editor<EntityRow, String> {

		private final IncludedItems<EntityRow> included;

		private EntityEditor(FilterTableModel<EntityRow, String> tableModel) {
			included = tableModel.items().included();
		}

		@Override
		public boolean editable(EntityRow row, String identifier) {
			return identifier.equals(EntityColumns.DTO);
		}

		@Override
		public void set(Object value, int rowIndex, EntityRow row, String identifier) {
			included.set(rowIndex, new EntityRow(row.definition, row.tableType, (Boolean) value));
			updateDomainSource();
		}
	}

	public static final class SchemaColumns implements TableColumns<SchemaRow, String> {

		public static final String CATALOG = "Catalog";
		public static final String SCHEMA = "Schema";
		public static final String POPULATED = "Populated";

		private static final List<String> IDENTIFIERS = unmodifiableList(asList(CATALOG, SCHEMA, POPULATED));

		@Override
		public List<String> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			if (identifier.equals(POPULATED)) {
				return Boolean.class;
			}

			return String.class;
		}

		@Override
		public Object value(SchemaRow row, String identifier) {
			switch (identifier) {
				case CATALOG:
					return row.catalog();
				case SCHEMA:
					return row.name();
				case POPULATED:
					return row.populated();
				default:
					throw new IllegalArgumentException();
			}
		}
	}

	public static final class EntityColumns implements TableColumns<EntityRow, String> {

		public static final String ENTITY = "Entity";
		public static final String TABLE_TYPE = "Type";
		public static final String DTO = "Dto";

		private static final List<String> IDENTIFIERS = unmodifiableList(asList(ENTITY, TABLE_TYPE, DTO));

		@Override
		public List<String> identifiers() {
			return IDENTIFIERS;
		}

		@Override
		public Class<?> columnClass(String identifier) {
			if (identifier.equals(DTO)) {
				return Boolean.class;
			}

			return String.class;
		}

		@Override
		public Object value(EntityRow row, String identifier) {
			switch (identifier) {
				case ENTITY:
					return row.definition.type().name();
				case TABLE_TYPE:
					return row.tableType;
				case DTO:
					return row.dto;
				default:
					throw new IllegalArgumentException();
			}
		}
	}
}
