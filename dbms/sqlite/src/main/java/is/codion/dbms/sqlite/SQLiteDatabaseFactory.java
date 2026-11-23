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
 * Copyright (c) 2019 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.property.PropertyValue;

import org.jspecify.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;

import static is.codion.common.utilities.Configuration.stringValue;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * Provides sqlite database implementations.
 * <p>
 * A database in a file runs its init scripts, {@link Database#INIT_SCRIPTS}, each once in its life, when an instance is
 * created for it, once per JVM until the database is closed, its schemas attached, see {@link #SCHEMAS}. They are
 * recorded in the table {@code codion_init_script} of the main database. {@link #validateInitScripts(Connection, String...)}
 * tells whether a database would be refused, without opening it, for a copy of a database, say.
 * A database whose files hold nothing, neither the main one nor a schema's, is a new one, running every script, not an
 * existing one without the record: a first start killed before it committed anything leaves such files behind.
 * <p>
 * A script runs in the transaction recording it, SQLite's DDL being transactional, so a script that fails leaves nothing
 * behind. Its statements, the driver running only the first of several, are split at the semicolons ending them, those
 * within quotes, comments and a trigger's body left alone, a trigger ending with {@code END} and a semicolon. A
 * {@code CASE} expression in a trigger's body, its {@code END} followed by a semicolon, ends the trigger early. A statement
 * that can not run in a transaction, {@code ATTACH} or {@code VACUUM}, or has no effect in one, {@code PRAGMA foreign_keys},
 * does not belong in a script. A script holding a statement beginning or ending a transaction, {@code BEGIN},
 * {@code COMMIT}, {@code END} or {@code ROLLBACK}, as one written for another database may end with, is refused, none of
 * it run; a savepoint, and a rollback to one, are within the script's transaction.
 * <p>
 * The scripts of a database whose connections enforce foreign keys, {@code foreign_keys=true} in its URL, run with the
 * foreign keys not enforced, so that a script can replace a table other tables reference, as SQLite documents a change
 * {@code ALTER TABLE} can not make: a new table created and filled, the old one dropped and the new one renamed, which
 * is refused while they are enforced, and they can not be turned off within a transaction. Each script's foreign keys
 * are checked before it is committed, {@code PRAGMA foreign_key_check}, and a script leaving a row referencing none is
 * refused, nothing of it left. A delete in a script therefore neither cascades nor sets null, an action of a foreign key
 * being part of its enforcing.
 * <p>
 * A transaction takes the database's write lock as it starts, {@code BEGIN IMMEDIATE}, as does a write outside of one,
 * waiting for another writer within the connection's busy timeout, so that one reading before it writes can not fail
 * for another having written meanwhile; a transaction that only reads waits for a writer as well. A transaction's
 * changes are kept in memory until it commits, {@code PRAGMA cache_spill = off} on every connection, so that other
 * connections read while it writes, however much, a large one taking that much memory, where SQLite would otherwise
 * write the database file once its page cache, 2 MB by default, is full, locking readers out until the transaction ends.
 * <p>
 * Text is compared as on other databases: {@code upper()} and {@code lower()} are Java's, registered on every
 * connection in place of SQLite's own, which change the letters of ASCII alone, so that a case-insensitive condition
 * finds {@code Þingvellir} by {@code þing%}, and {@code LIKE} is case-sensitive, {@code PRAGMA case_sensitive_like = on},
 * where SQLite's ignores the case of an ASCII letter. The functions are registered by the driver's own classes, which
 * this module reads with the driver a module as well, {@code org.xerial.sqlitejdbc}: with this module on the module
 * path and the driver on the class path a connection is refused, an {@link IllegalStateException} saying so, rather
 * than made with SQLite's own functions. A connection made past this dialect, another tool's or the
 * driver's own, has SQLite's, so neither {@code upper()}, {@code lower()} nor {@code LIKE} belongs in the database's
 * schema, an index on an expression, a check constraint or a generated column: an index on {@code upper(name)} written
 * by one definition is damaged to the other, {@code PRAGMA integrity_check} reporting its rows missing, and a row
 * written by the other is not found by it.
 * <p>
 * A key the database generates, one read back by {@code Column.Generator.identity()}, is declared exactly
 * {@code INTEGER PRIMARY KEY AUTOINCREMENT}: SQLite's row id, which the driver returns for an insert, never handed out
 * twice. A key of another type, {@code INT} or {@code BIGINT PRIMARY KEY}, is no row id, the driver then returning a
 * row id the row's own key, left null, does not hold. Without {@code AUTOINCREMENT} the next id is the one after the
 * highest there is, so the id of the newest row, once that row is deleted, goes to the next row inserted, and an entity
 * held from before the delete, or a reference to it about to be saved, then names that row, where a database never
 * handing an id out twice refuses it. An init script replacing a table with such a key, as above, carries over how far
 * its ids have come, the old table's entry in {@code sqlite_sequence} to the new table's, which otherwise starts after
 * the highest id it was given.
 * <p>
 * A date or time is stored as text, a column of SQLite's having no such type: {@code LocalDate}, {@code LocalTime} and
 * {@code LocalDateTime} values in SQLite's own format, as its date and time functions write them, {@code yyyy-MM-dd},
 * {@code HH:mm:ss} and {@code yyyy-MM-dd HH:mm:ss}, a fraction of a second following where there is one, which a script
 * writes its literals in, for conditions to compare them, and {@code OffsetDateTime} and {@code OffsetTime} values as
 * their ISO text. A time is written to the microsecond, what is past it left out, where a column without a precision
 * would keep every digit a value has, nine from a clock counting nanoseconds, a database with a precision, a server
 * the database is synced with say, keeping six by default, the two then differing. A value written to a database
 * keeping fewer digits still, a time without a fraction for example, is the application's to shorten.
 * <p>
 * A decimal is stored as SQLite's {@code NUMERIC} affinity has it, in a column declared {@code DECIMAL} or
 * {@code NUMERIC} for example: a whole number as an integer, exactly within the range of a {@code long}, and a number
 * with a fraction as a double, which keeps 15 significant digits, a value with more possibly read back changed. A column
 * declared {@code TEXT} keeps a decimal exactly, but compares and orders it as text in SQL, {@code 10} before {@code 9}.
 */
public final class SQLiteDatabaseFactory implements DatabaseFactory {

	/**
	 * The schemas attached to every connection, a comma separated list of names, each a database file of its own beside
	 * the main one, named after it and the schema, {@code fieldbook-sync.db} for the schema {@code sync} of
	 * {@code fieldbook.db}, created where it does not exist. Tables are then named by schema, {@code sync.row_state} for
	 * example, as on a database with schemas.
	 * <p>
	 * A schema is attached with the main database's journal mode and synchronous setting. A transaction writing to
	 * several files is atomic across them in a rollback journal mode only, not in WAL mode, in which a crash commits it
	 * in some of the files and not in others, so a main database in WAL mode is refused its schemas, creating a
	 * connection throwing an {@link IllegalStateException}.
	 * <ul>
	 * <li>Value type: String
	 * <li>Default value: null, no schemas attached
	 * </ul>
	 */
	public static final PropertyValue<String> SCHEMAS = stringValue("is.codion.dbms.sqlite.SQLiteDatabaseFactory.schemas");

	private static final String DRIVER_PACKAGE = "org.sqlite";

	@Override
	public boolean supports(String driverClassName) {
		return requireNonNull(driverClassName).startsWith(DRIVER_PACKAGE);
	}

	@Override
	public Database create(String url) {
		return new SQLiteDatabase(url, SCHEMAS.get(), Text.parseCSV(Database.INIT_SCRIPTS.get()));
	}

	/**
	 * Creates a SQLite Database instance with the given schemas and init scripts, rather than those of {@link #SCHEMAS}
	 * and {@link Database#INIT_SCRIPTS}, which {@link #create(String)} takes.
	 * @param url the jdbc url
	 * @param schemas the schemas attached to every connection, as {@link #SCHEMAS} takes them, null for none
	 * @param initScripts the init scripts, each run once in the database's life, see {@link Database#INIT_SCRIPTS}
	 * @return a SQLite Database instance
	 * @throws IllegalArgumentException in case a schema is not a valid name, or schemas are given for a database without a
	 * file, in memory or temporary
	 */
	public static Database database(String url, @Nullable String schemas, String... initScripts) {
		return new SQLiteDatabase(requireNonNull(url), schemas, asList(requireNonNull(initScripts)));
	}

	/**
	 * Validates a database against the init scripts it would be opened with, refusing it as opening it would, see
	 * {@link Database#INIT_SCRIPTS}, changing nothing, so the connection may be read only. A database without the record of
	 * its scripts is never refused, nor one opened with none listed.
	 * @param connection a connection to the database
	 * @param initScripts the init scripts it would be opened with
	 * @throws DatabaseException in case the database would be refused, or a script listed can not be read
	 * @throws SQLException in case the record of the scripts run can not be read
	 */
	public static void validateInitScripts(Connection connection, String... initScripts) throws SQLException {
		SQLiteDatabase.validateInitScripts(requireNonNull(connection), asList(requireNonNull(initScripts)));
	}
}
