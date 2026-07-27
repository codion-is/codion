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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.cli;

import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.framework.domain.db.SchemaDomain;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.tools.generator.domain.DomainSource;

import org.jspecify.annotations.Nullable;

import java.io.PrintStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;

/**
 * A command line interface for the domain source code generator, generating the domain source code
 * for a database schema, either to standard output or to source files.
 * <p>
 * The dbms module and the JDBC driver for the database in question must be available at runtime.
 * <pre>{@code
 * java -m is.codion.tools.generator.cli \
 *     --url jdbc:h2:mem:h2db \
 *     --init-scripts src/main/sql/create_schema.sql \
 *     --user sa \
 *     --schema PETCLINIC \
 *     --package is.codion.demos.petclinic.domain
 * }</pre>
 * Run with --help for the available options.
 */
public final class DomainGeneratorCli {

	private static final int SUCCESS = 0;
	private static final int FAILURE = 1;
	private static final int USAGE_ERROR = 2;

	private static final String USAGE = """
					Usage: java -m is.codion.tools.generator.cli [options]
					
					Generates the domain source code for a database schema.
					
					Options:
					  --url <url>              the database url, falls back to codion.db.url
					  --init-scripts <paths>   database init scripts, comma separated, falls back to codion.db.initScripts
					  --user <user>            the database user, on the form username or username:password
					  --schema <schema>        the schema to generate a domain model for (required)
					  --package <package>      the domain package (required)
					  --output-dir <dir>       the source directory to write the domain source to,
					                           the package directories are created below it,
					                           when not specified the source is printed to standard output
					  --resource-dir <dir>     the resource directory to write the i18n properties to, required by --i18n
					  --test-dir <dir>         the test source directory to write the domain unit test to, required by --test
					  --split-api-impl         write the domain api and implementation to separate source files
					  --i18n                   generate i18n resource bundles for the entity and attribute captions
					  --test                   generate a domain unit test
					  --dtos                   generate a record based dto for each entity
					  --overwrite              overwrite existing source files
					  --help                   print this message
					
					The dbms module and the JDBC driver for the database in question must be available at runtime.
					Exit codes: 0 success, 1 failure, 2 usage error.""";

	private DomainGeneratorCli() {}

	/**
	 * Generates the domain source code for the schema specified by the given arguments,
	 * exiting with 0 on success, 1 on failure and 2 in case of a usage error.
	 * @param arguments the command line arguments, run with --help for the available options
	 */
	public static void main(String[] arguments) {
		System.exit(run(arguments, System.out, System.err));
	}

	static int run(String[] arguments, PrintStream out, PrintStream err) {
		Arguments parsed;
		try {
			parsed = Arguments.parse(arguments);
		}
		catch (IllegalArgumentException e) {
			err.println(e.getMessage());
			err.println();
			err.println(USAGE);

			return USAGE_ERROR;
		}
		if (parsed.help) {
			out.println(USAGE);

			return SUCCESS;
		}
		try {
			return generate(parsed, out, err);
		}
		catch (Exception e) {
			err.println(e.getMessage() == null ? e.toString() : e.getMessage());

			return FAILURE;
		}
	}

	private static int generate(Arguments arguments, PrintStream out, PrintStream err) throws Exception {
		if (arguments.url != null) {
			Database.URL.set(arguments.url);
		}
		if (arguments.initScripts != null) {
			Database.INIT_SCRIPTS.set(arguments.initScripts);
		}
		SchemaDomain domain = schemaDomain(arguments);
		DomainSource domainSource = DomainSource.builder()
						.domain(domain)
						.domainPackage(arguments.domainPackage)
						.dtos(arguments.dtos ? entityTypes(domain) : emptySet())
						.i18n(arguments.i18n)
						.test(arguments.test)
						.build();
		if (arguments.outputDir == null) {
			out.println(domainSource.combined());
		}
		else if (!write(domainSource, arguments, err)) {
			return FAILURE;
		}
		err.println("Generated " + domain.entities().definitions().size() + " entities from schema " + arguments.schema);

		return SUCCESS;
	}

	private static SchemaDomain schemaDomain(Arguments arguments) throws SQLException {
		Database database = Database.instance();
		try (Connection connection = arguments.user == null ?
						database.createConnection() :
						database.createConnection(arguments.user)) {
			SchemaDomain domain = SchemaDomain.schemaDomain(connection.getMetaData(), arguments.schema);
			if (domain.entities().definitions().isEmpty()) {
				throw new IllegalArgumentException("No tables found in schema: " + arguments.schema);
			}

			return domain;
		}
	}

	private static boolean write(DomainSource domainSource, Arguments arguments, PrintStream err) throws Exception {
		Path sourcePath = packagePath(arguments.outputDir, arguments.domainPackage);
		// The api/impl are written to the same source directory, the api below an 'api' package,
		// the resource and test paths default to the source path, in which case they are unused,
		// since --i18n and --test require --resource-dir and --test-dir respectively
		Path resourcePath = arguments.resourceDir == null ? sourcePath : packagePath(arguments.resourceDir, arguments.domainPackage);
		Path testPath = arguments.testDir == null ? sourcePath : packagePath(arguments.testDir, arguments.domainPackage);
		if (arguments.splitApiImpl ?
						domainSource.writeApiImpl(sourcePath, sourcePath, resourcePath, testPath, () -> arguments.overwrite) :
						domainSource.writeCombined(sourcePath, resourcePath, testPath, () -> arguments.overwrite)) {
			err.println("Wrote the domain source to " + sourcePath);

			return true;
		}
		err.println("The domain source already exists in " + sourcePath + ", use --overwrite to replace it");

		return false;
	}

	private static Path packagePath(Path root, String domainPackage) {
		Path path = root;
		for (String name : domainPackage.split("\\.")) {
			path = path.resolve(name);
		}

		return path;
	}

	private static Set<EntityType> entityTypes(SchemaDomain domain) {
		return domain.entities().definitions().stream()
						.map(EntityDefinition::type)
						.collect(toSet());
	}

	private static final class Arguments {

		private @Nullable String url;
		private @Nullable String initScripts;
		private @Nullable User user;
		private @Nullable String schema;
		private @Nullable String domainPackage;
		private @Nullable Path outputDir;
		private @Nullable Path resourceDir;
		private @Nullable Path testDir;
		private boolean splitApiImpl;
		private boolean i18n;
		private boolean test;
		private boolean dtos;
		private boolean overwrite;
		private boolean help;

		private static Arguments parse(String[] arguments) {
			Arguments parsed = new Arguments();
			for (int i = 0; i < arguments.length; i++) {
				String argument = arguments[i];
				switch (argument) {
					case "--url" -> parsed.url = value(arguments, ++i);
					case "--init-scripts" -> parsed.initScripts = value(arguments, ++i);
					case "--user" -> parsed.user = User.parse(value(arguments, ++i));
					case "--schema" -> parsed.schema = value(arguments, ++i);
					case "--package" -> parsed.domainPackage = value(arguments, ++i);
					case "--output-dir" -> parsed.outputDir = Path.of(value(arguments, ++i));
					case "--resource-dir" -> parsed.resourceDir = Path.of(value(arguments, ++i));
					case "--test-dir" -> parsed.testDir = Path.of(value(arguments, ++i));
					case "--split-api-impl" -> parsed.splitApiImpl = true;
					case "--i18n" -> parsed.i18n = true;
					case "--test" -> parsed.test = true;
					case "--dtos" -> parsed.dtos = true;
					case "--overwrite" -> parsed.overwrite = true;
					case "--help", "-h" -> parsed.help = true;
					default -> throw new IllegalArgumentException("Unknown option: " + argument);
				}
			}
			if (!parsed.help) {
				parsed.validate();
			}

			return parsed;
		}

		private void validate() {
			if (schema == null) {
				throw new IllegalArgumentException("The schema must be specified: --schema");
			}
			if (domainPackage == null) {
				throw new IllegalArgumentException("The domain package must be specified: --package");
			}
			if (url == null && Database.URL.isNull()) {
				throw new IllegalArgumentException("The database url must be specified: --url or " + Database.URL.name());
			}
			// The i18n properties and the unit test are additional files, standard output can not represent them
			if (i18n && outputDir == null) {
				throw new IllegalArgumentException("The --i18n option requires --output-dir");
			}
			if (test && outputDir == null) {
				throw new IllegalArgumentException("The --test option requires --output-dir");
			}
			if (i18n && resourceDir == null) {
				throw new IllegalArgumentException("The --i18n option requires --resource-dir");
			}
			if (test && testDir == null) {
				throw new IllegalArgumentException("The --test option requires --test-dir");
			}
		}

		private static String value(String[] arguments, int index) {
			if (index == arguments.length) {
				throw new IllegalArgumentException("Missing value for option: " + arguments[index - 1]);
			}

			return arguments[index];
		}
	}
}
