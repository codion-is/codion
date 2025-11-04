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
package is.codion.tools.jul.classpath;

import java.io.IOException;
import java.io.InputStream;

import static java.util.logging.LogManager.getLogManager;

/**
 * A custom configuration class for java.util.logging that loads configuration files from the classpath.
 * <p>
 * By default, java.util.logging only supports loading configuration files from the filesystem via
 * {@code -Djava.util.logging.config.file=path/to/file.properties}. This limitation makes it difficult
 * to use JUL configuration in containerized applications, jlink images, or fat jars where configuration
 * files are typically packaged as classpath resources.
 * <p>
 * This class bridges that gap by loading the configuration file specified by {@code java.util.logging.config.file}
 * from the classpath instead of the filesystem. It uses the standard JUL configuration mechanism via
 * {@code -Djava.util.logging.config.class} to install itself as the configuration handler.
 *
 * <h2>Module Path Requirements</h2>
 * <p>
 * This class is in the {@code is.codion.tools.jul.classpath} module, which must be on the module path
 * for the JVM to load it. There are two ways to ensure this:
 *
 * <h3>Option 1: Module Dependency (Recommended)</h3>
 * <p>
 * Add a {@code requires} clause to your {@code module-info.java}:
 * <pre>{@code
 * module your.module {
 *     requires is.codion.tools.jul.classpath;
 * }
 * }</pre>
 * <p>
 * Note: The {@code is.codion.framework.server} module already requires this module, so server applications
 * using EntityServer get this functionality automatically without additional configuration.
 *
 * <h3>Option 2: Runtime Module Addition</h3>
 * <p>
 * If you cannot add a module dependency (e.g., configuration-only modules with no {@code module-info.java}),
 * explicitly add the module at runtime:
 * <pre>{@code
 * --add-modules is.codion.tools.jul.classpath
 * }</pre>
 * <p>
 * For jlink images, add the module to the jlink configuration:
 * <pre>{@code
 * jlink --add-modules is.codion.tools.jul.classpath,...
 * }</pre>
 *
 * <h2>Basic Usage</h2>
 * <p>
 * Place your logging configuration file (e.g., {@code logging.properties}) on the classpath and configure
 * the JVM with both properties:
 *
 * <pre>{@code
 * -Djava.util.logging.config.file=logging.properties
 * -Djava.util.logging.config.class=is.codion.tools.jul.classpath.ClasspathConfiguration
 * }</pre>
 * <p>
 * The configuration file will be loaded from the classpath root using the context class loader.
 *
 * <h2>Example Configuration File</h2>
 * <p>
 * A typical use case is configuring serialization filter rejection logging:
 *
 * <pre>{@code
 * # logging.properties - place on classpath
 *
 * # Global log level
 * .level=OFF
 *
 * # Enable java.io.serialization logger
 * java.io.serialization.level=FINE
 * java.io.serialization.handlers=java.util.logging.FileHandler
 * java.io.serialization.useParentHandlers=false
 *
 * # Write to file in working directory
 * java.util.logging.FileHandler.pattern=serialization.log
 * java.util.logging.FileHandler.formatter=java.util.logging.SimpleFormatter
 * java.util.logging.SimpleFormatter.format=%1$tF %1$tT %4$s: %5$s%n
 * }</pre>
 *
 * <h2>Benefits</h2>
 *
 * <ul>
 * <li>Works with jlink images where filesystem access may be restricted</li>
 * <li>Compatible with Docker containers and cloud deployments</li>
 * <li>Enables packaging JUL configuration in fat jars</li>
 * <li>Maintains standard {@code java.util.logging.config.file} property semantics</li>
 * <li>Provides clear feedback on success or failure to load configuration</li>
 * </ul>
 * @see java.util.logging.LogManager#readConfiguration(InputStream)
 */
public final class ClasspathConfiguration {

	private static final String CONFIGURATION_FILE = "java.util.logging.config.file";
	private static final String UNABLE_TO_CONFIGURE = "Unable to configure java.util.logging from classpath: ";

	/**
	 * Constructs a new ClasspathConfiguration and immediately loads the configuration file
	 * specified by {@code java.util.logging.config.file} from the classpath.
	 * <p>
	 * This constructor is invoked automatically by the JVM when
	 * {@code -Djava.util.logging.config.class=is.codion.tools.jul.classpath.ClasspathConfiguration}
	 * is specified.
	 * <p>
	 * If the configuration file is found and successfully loaded, a confirmation message is
	 * printed to stdout. If the file is not found or loading fails, an error message is
	 * printed to stderr.
	 */
	public ClasspathConfiguration() {
		String configFile = System.getProperty(CONFIGURATION_FILE);
		if (configFile == null) {
			System.err.println(UNABLE_TO_CONFIGURE + CONFIGURATION_FILE + " property not specified");
			return;
		}
		if (configFile.contains("/") || configFile.contains("\\")) {
			System.err.println(UNABLE_TO_CONFIGURE + configFile + " is not in the classpath root");
			return;
		}
		try (InputStream inputStream = ClasspathConfiguration.class.getClassLoader().getResourceAsStream(configFile)) {
			if (inputStream == null) {
				System.err.println(UNABLE_TO_CONFIGURE + configFile + " not found");
			}
			else {
				getLogManager().readConfiguration(inputStream);
				System.out.println("Loaded java.util.logging configuration from classpath: " + configFile);
			}
		}
		catch (IOException exception) {
			System.err.println(UNABLE_TO_CONFIGURE + configFile + " - " + exception.getMessage());
		}
	}
}
