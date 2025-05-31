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
package is.codion.demos.chinook.migration;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static is.codion.framework.domain.DomainType.domainType;
import static java.lang.Integer.parseInt;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.joining;

/**
 * Domain model for migration tracking - used only for recording migration history
 */
final class MigrationDomain extends DomainModel {

	static final User MIGRATION_USER = User.parse("scott:tiger");

	private static final String MIGRATION_PATH = "/db/migration/";
	private static final Pattern MIGRATION_PATTERN = Pattern.compile("V(\\d+)__(.+)\\.sql");

	private static final DomainType DOMAIN = domainType(MigrationDomain.class);
	private static final String MIGRATION_TABLE = "CHINOOK.SCHEMA_MIGRATION";
	private static final String CREATE_MIGRATION_TABLE = """
					CREATE TABLE IF NOT EXISTS CHINOOK.SCHEMA_MIGRATION (
						VERSION INTEGER NOT NULL,
						DESCRIPTION VARCHAR(200) NOT NULL,
						SCRIPT VARCHAR(200) NOT NULL,
						CHECKSUM INTEGER NOT NULL,
						EXECUTED_BY VARCHAR(100) NOT NULL,
						EXECUTED_AT TIMESTAMP NOT NULL,
						EXECUTION_TIME INTEGER NOT NULL,
						SUCCESS BOOLEAN NOT NULL,
						CONSTRAINT PK_SCHEMA_MIGRATION PRIMARY KEY (VERSION)
					)
					""";
	private static final String APPLIED_VERSIONS_QUERY = "SELECT VERSION FROM " + MIGRATION_TABLE + " WHERE SUCCESS = TRUE ORDER BY VERSION";
	private static final ResultPacker<Integer> VERSION_PACKER = resultSet -> resultSet.getInt("VERSION");

	record Migration(int version, String description, String filename, String content) {

		int checksum() {
			return Objects.hash(content);
		}
	}

	interface SchemaMigration {
		EntityType TYPE = DOMAIN.entityType("chinook.schema_migration");

		Column<Integer> VERSION = TYPE.integerColumn("version");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> SCRIPT = TYPE.stringColumn("script");
		Column<Integer> CHECKSUM = TYPE.integerColumn("checksum");
		Column<String> EXECUTED_BY = TYPE.stringColumn("executed_by");
		Column<LocalDateTime> EXECUTED_AT = TYPE.localDateTimeColumn("executed_at");
		Column<Integer> EXECUTION_TIME = TYPE.integerColumn("execution_time");
		Column<Boolean> SUCCESS = TYPE.booleanColumn("success");
	}

	MigrationDomain() {
		super(DOMAIN);
		add(SchemaMigration.TYPE.define(
										SchemaMigration.VERSION.define()
														.primaryKey(),
										SchemaMigration.DESCRIPTION.define()
														.column()
														.maximumLength(200)
														.nullable(false),
										SchemaMigration.SCRIPT.define()
														.column()
														.maximumLength(200)
														.nullable(false),
										SchemaMigration.CHECKSUM.define()
														.column()
														.nullable(false),
										SchemaMigration.EXECUTED_BY.define()
														.column()
														.maximumLength(100)
														.nullable(false),
										SchemaMigration.EXECUTED_AT.define()
														.column()
														.nullable(false),
										SchemaMigration.EXECUTION_TIME.define()
														.column()
														.nullable(false),
										SchemaMigration.SUCCESS.define()
														.column()
														.nullable(false))
						.build());
	}

	List<Migration> pendingMigrations(Connection connection, String[] migrationFiles) throws SQLException, IOException {
		createMigrationTable(connection);
		List<Integer> appliedVersions = appliedVersions(connection);

		return loadMigrations(migrationFiles).stream()
						.filter(migration -> !appliedVersions.contains(migration.version))
						.toList();
	}

	void applyMigration(Connection connection, Database database, Migration migration) throws SQLException {
		System.out.println("[MigrationManager] Applying migration V" + migration.version + ": " + migration.description);
		long startTime = currentTimeMillis();
		try (Statement statement = connection.createStatement()) {
			// Execute the migration script
			statement.execute(migration.content);
			// Record successful migration
			recordMigration(database, migration, true, currentTimeMillis() - startTime);
			System.out.println("[MigrationManager] Migration V" + migration.version + " completed successfully");
		}
		catch (SQLException | DatabaseException e) {
			// Record failed migration
			recordMigration(database, migration, false, currentTimeMillis() - startTime);
			throw new SQLException("Migration V" + migration.version + " failed: " + e.getMessage(), e);
		}
	}

	private void recordMigration(Database database, Migration migration, boolean success,
															 long executionTime) throws SQLException, DatabaseException {
		// Create a separate connection for entity operations to avoid auto-commit conflicts
		try (Connection connection = database.createConnection(MIGRATION_USER);
				 EntityConnection entityConnection = LocalEntityConnection.localEntityConnection(database, this, connection)) {
			entityConnection.insert(entityConnection.entities().builder(SchemaMigration.TYPE)
							.with(SchemaMigration.VERSION, migration.version)
							.with(SchemaMigration.DESCRIPTION, migration.description)
							.with(SchemaMigration.SCRIPT, migration.filename)
							.with(SchemaMigration.CHECKSUM, migration.checksum())
							.with(SchemaMigration.EXECUTED_BY, getProperty("user.name", "unknown"))
							.with(SchemaMigration.EXECUTED_AT, LocalDateTime.now())
							.with(SchemaMigration.EXECUTION_TIME, (int) executionTime)
							.with(SchemaMigration.SUCCESS, success)
							.build());
		}
	}

	private static void createMigrationTable(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement()) {
			statement.execute(CREATE_MIGRATION_TABLE);
		}
	}

	private static List<Integer> appliedVersions(Connection connection) throws SQLException {
		try (Statement statement = connection.createStatement();
				 ResultSet resultSet = statement.executeQuery(APPLIED_VERSIONS_QUERY)) {
			return VERSION_PACKER.pack(resultSet);
		}
	}

	private static List<Migration> loadMigrations(String[] migrationFiles) throws IOException {
		List<Migration> migrations = new ArrayList<>();
		for (String filename : migrationFiles) {
			Matcher matcher = MIGRATION_PATTERN.matcher(filename);
			if (matcher.matches()) {
				int version = parseInt(matcher.group(1));
				String description = matcher.group(2).replace('_', ' ');
				String content = loadMigrationContent(MIGRATION_PATH + filename);
				migrations.add(new Migration(version, description, filename, content));
			}
		}
		migrations.sort(comparingInt(migration -> migration.version));

		return migrations;
	}

	private static String loadMigrationContent(String resourcePath) throws IOException {
		try (InputStream stream = MigrationDomain.class.getResourceAsStream(resourcePath)) {
			if (stream == null) {
				throw new IOException("Migration script not found: " + resourcePath);
			}
			return new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
							.lines()
							.collect(joining("\n"));
		}
	}
}
