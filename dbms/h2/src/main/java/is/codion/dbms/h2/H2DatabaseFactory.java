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
package is.codion.dbms.h2;

import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.Text;
import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.common.utilities.user.User;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.SQLException;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations.
 * <p>
 * An embedded database, in memory or in a file, runs its init scripts, {@link Database#INIT_SCRIPTS}, each once in its
 * life, as sa, when an instance is created for it, once per JVM until the database is closed. They are recorded in the
 * table {@code CODION.INIT_SCRIPT}. {@link #validateInitScripts(Connection, String...)} tells whether a database would be
 * refused, without opening it, for a copy of a database, say.
 * <p>
 * H2's DDL commits, so a script that fails may leave part of its work behind, running again the next time.
 * <p>
 * A database on a server, or in a zip file, is not initialized, nor an existing one refusing sa without a password, its
 * admin having one or being another: administered, its scripts run by its administrator, and a script listed for it
 * never run.
 * <p>
 * A thread interrupted during I/O closes the file of a database opened with {@code jdbc:h2:file:}, every connection to it
 * failing until it is reopened. Where its threads may be interrupted, open it with {@code jdbc:h2:async:} instead, the file
 * being the same. Every URL to a database in a JVM must then use the same prefix.
 */
public final class H2DatabaseFactory implements DatabaseFactory {

	private static final String DRIVER_PACKAGE = "org.h2";
	private static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
	private static final String SYSADMIN_USERNAME = "sa";

	@Override
	public boolean supports(String driverClassName) {
		return requireNonNull(driverClassName).startsWith(DRIVER_PACKAGE);
	}

	@Override
	public Database create(String url) {
		return new H2Database(url, Text.parseCSV(Database.INIT_SCRIPTS.get()));
	}

	/**
	 * Creates an H2 Database instance
	 * @param url the jdbc url
	 * @param initScripts the init scripts, each run once in the database's life, see {@link Database#INIT_SCRIPTS}
	 * @return an H2 Database instance
	 */
	public static Database database(String url, String... initScripts) {
		return new H2Database(url, initScripts == null ? emptyList() : asList(initScripts));
	}

	/**
	 * Validates a database against the init scripts it would be opened with, refusing it as opening it would, see
	 * {@link Database#INIT_SCRIPTS}, changing nothing, so the connection may be read only. A database without the record of its
	 * scripts is never refused, nor one opened with none listed.
	 * @param connection a connection to the database
	 * @param initScripts the init scripts it would be opened with
	 * @throws DatabaseException in case the database would be refused, or a script listed can not be read
	 * @throws SQLException in case the record of the scripts run can not be read
	 */
	public static void validateInitScripts(Connection connection, String... initScripts) throws SQLException {
		H2Database.validateInitScripts(requireNonNull(connection), asList(requireNonNull(initScripts)));
	}

	/**
	 * Creates a new ScriptRunner instance, using by default the sysadmin username (sa) and the default system charset.
	 * @param jdbcUrl the jdbc URL
	 * @return a new {@link ScriptRunner} instance
	 */
	public static ScriptRunner scriptRunner(String jdbcUrl) {
		return new DefaultScriptRunner(jdbcUrl);
	}

	/**
	 * Runs scripts using the H2 RunScript tool.
	 */
	public interface ScriptRunner {

		/**
		 * @param user the user credentials to use
		 * @return this {@link ScriptRunner}
		 */
		ScriptRunner user(User user);

		/**
		 * @param charset the script charset
		 * @return this {@link ScriptRunner}
		 */
		ScriptRunner charset(Charset charset);

		/**
		 * @param scriptPath the path to the script to run
		 * @return this {@link ScriptRunner}
		 */
		ScriptRunner run(String scriptPath);
	}

	private static final class DefaultScriptRunner implements ScriptRunner {

		private final String jdbcUrl;

		private User user = User.user(SYSADMIN_USERNAME);
		private Charset charset = defaultCharset();

		private DefaultScriptRunner(String jdbcUrl) {
			this.jdbcUrl = requireNonNull(jdbcUrl);
		}

		@Override
		public ScriptRunner user(User user) {
			this.user = requireNonNull(user);
			return this;
		}

		@Override
		public ScriptRunner charset(Charset charset) {
			this.charset = requireNonNull(charset);
			return this;
		}

		@Override
		public ScriptRunner run(String scriptPath) {
			runScript(jdbcUrl, scriptPath, user.username(), String.valueOf(user.password()), charset);
			return this;
		}

		private static void runScript(String jdbcUrl, String scriptPath, String username, String password, Charset scriptCharset) {
			try {
				Class<?> runScriptToolClass = Class.forName(RUN_TOOL_CLASS_NAME);
				runScriptToolClass.getMethod("execute", String.class, String.class, String.class, String.class, Charset.class, boolean.class)
								.invoke(runScriptToolClass.getDeclaredConstructor().newInstance(),
												jdbcUrl, username, password, scriptPath, scriptCharset, false);
			}
			catch (ClassNotFoundException cle) {
				throw new RuntimeException(RUN_TOOL_CLASS_NAME + " must be on classpath for running scripts", cle);
			}
			catch (InvocationTargetException ite) {
				if (ite.getCause() instanceof SQLException) {
					throw new DatabaseException((SQLException) ite.getCause());
				}
				throw new RuntimeException(ite.getCause());
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}
}
