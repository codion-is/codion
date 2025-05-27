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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.dbms.h2;

import is.codion.common.Text;
import is.codion.common.db.database.Database;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.Charset;
import java.sql.SQLException;
import java.util.Arrays;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Provides h2 database implementations
 */
public final class H2DatabaseFactory implements DatabaseFactory {

	private static final String DRIVER_PACKAGE = "org.h2";
	private static final String RUN_TOOL_CLASS_NAME = "org.h2.tools.RunScript";
	private static final String SYSADMIN_USERNAME = "sa";

	@Override
	public boolean driverCompatible(String driverClassName) {
		return requireNonNull(driverClassName).startsWith(DRIVER_PACKAGE);
	}

	@Override
	public Database create(String url) {
		return new H2Database(url, Text.parseCSV(Database.DATABASE_INIT_SCRIPTS.get()),
						Database.SELECT_FOR_UPDATE_NOWAIT.getOrThrow());
	}

	/**
	 * Creates an H2 Database instance
	 * @param url the jdbc url
	 * @param initScripts initialization scripts to run on database creation
	 * @return an H2 Database instance
	 */
	public static Database createDatabase(String url, String... initScripts) {
		return new H2Database(url, initScripts == null ? emptyList() : Arrays.asList(initScripts),
						Database.SELECT_FOR_UPDATE_NOWAIT.getOrThrow());
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
		private Charset charset = Charset.defaultCharset();

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
				throw new RuntimeException(e);
			}
		}
	}
}
