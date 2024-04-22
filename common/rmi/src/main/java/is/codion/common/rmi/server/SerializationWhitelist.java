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
 * Copyright (c) 2018 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * Implements a serialization whitelist
 */
final class SerializationWhitelist {

	private static final Logger LOG = LoggerFactory.getLogger(SerializationWhitelist.class);

	private static final String CLASSPATH_PREFIX = "classpath:";

	private SerializationWhitelist() {}

	/**
	 * Creates a serialization filter based on a whitelist.
	 * Supports 'classpath:' prefix for a whitelist in the classpath root.
	 * @param whitelistFile the path to the file containing the whitelisted class names
	 */
	static WhitelistFilter whitelistFilter(String whitelistFile) {
		return new WhitelistFilter(whitelistFile);
	}

	/**
	 * Creates a serialization filter based on a whitelist.
	 * @param classnames the whitelisted class names
	 */
	static WhitelistFilter whitelistFilter(Collection<String> classnames) {
		return new WhitelistFilter(classnames);
	}

	/**
	 * Creates a serialization filter for a whitelist dry run.
	 * @param whitelistFile the file to write the dry-run results to
	 * @throws IllegalArgumentException in case of a classpath dry run file
	 */
	static DryRun whitelistDryRun(String whitelistFile) {
		return new DryRun(whitelistFile);
	}

	static void handleDryRun() {
		ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
		if (serialFilter instanceof DryRun) {
			((DryRun) serialFilter).writeToFile();
		}
	}

	static final class DryRun implements ObjectInputFilter {

		private final String whitelistFile;
		private final Set<Class<?>> deserializedClasses = new HashSet<>();

		private DryRun(String whitelistFile) {
			if (requireNonNull(whitelistFile).toLowerCase().startsWith(CLASSPATH_PREFIX)) {
				throw new IllegalArgumentException("Filter dry run can not be performed with a classpath whitelist: " + whitelistFile);
			}
			this.whitelistFile = whitelistFile;
		}

		@Override
		public Status checkInput(FilterInfo filterInfo) {
			Class<?> clazz = filterInfo.serialClass();
			if (clazz != null) {
				while (clazz.isArray()) {
					clazz = clazz.getComponentType();
				}
				deserializedClasses.add(clazz);
			}

			return Status.ALLOWED;
		}

		/**
		 * Writes all classnames found during the dry-run to the whitelist file.
		 */
		synchronized void writeToFile() {
			try {
				Files.write(Paths.get(whitelistFile), deserializedClasses.stream()
								.map(Class::getName)
								.sorted()
								.collect(toList()), StandardOpenOption.CREATE);
				LOG.info("Serialization whitelist written: {}", whitelistFile);
			}
			catch (Exception e) {
				LOG.error("Error while writing serialization filter dry run results: {}", whitelistFile, e);
			}
		}
	}

	static final class WhitelistFilter implements ObjectInputFilter {

		private static final String COMMENT = "#";
		private static final String WILDCARD = "*";

		private final Set<String> allowedClassnames = new HashSet<>();
		private final List<String> allowedWildcardClassnames = new ArrayList<>();

		private WhitelistFilter(String whitelistFile) {
			this(readWhitelistItems(whitelistFile));
		}

		private WhitelistFilter(Collection<String> whitelistItems) {
			addWhitelistItems(whitelistItems);
		}

		@Override
		public Status checkInput(FilterInfo filterInfo) {
			Class<?> clazz = filterInfo.serialClass();
			if (clazz == null) {
				return Status.ALLOWED;
			}
			if (clazz.isArray()) {
				return checkArrayInput(clazz);
			}

			return checkInput(clazz.getName());
		}

		Status checkArrayInput(Class<?> arrayClass) {
			Class<?> componentType = arrayClass.getComponentType();
			while (componentType.isArray()) {
				componentType = componentType.getComponentType();
			}
			if (componentType.isPrimitive()) {
				return Status.ALLOWED;
			}

			return checkInput(componentType.getName());
		}

		Status checkInput(String classname) {
			if (allowedClassnames.contains(classname) || allowWildcard(classname)) {
				return Status.ALLOWED;
			}
			LOG.error("Serialization rejected: {}", classname);

			return Status.REJECTED;
		}

		private void addWhitelistItems(Collection<String> whitelistItems) {
			requireNonNull(whitelistItems).forEach(whitelistItem -> {
				if (!whitelistItem.startsWith(COMMENT)) {
					if (whitelistItem.endsWith(WILDCARD)) {
						allowedWildcardClassnames.add(whitelistItem.substring(0, whitelistItem.length() - 1));
					}
					else {
						allowedClassnames.add(whitelistItem);
					}
				}
			});
		}

		private boolean allowWildcard(String classname) {
			if (allowedWildcardClassnames.isEmpty()) {
				return false;
			}

			for (int i = 0; i < allowedWildcardClassnames.size(); i++) {
				if (classname.startsWith(allowedWildcardClassnames.get(i))) {
					return true;
				}
			}

			return false;
		}

		private static Collection<String> readWhitelistItems(String whitelistFile) {
			if (requireNonNull(whitelistFile).startsWith(CLASSPATH_PREFIX)) {
				return readClasspathWhitelistItems(whitelistFile);
			}

			return readFileWhitelistItems(whitelistFile);
		}

		private static Collection<String> readClasspathWhitelistItems(String whitelistFile) {
			String path = classpathFilepath(whitelistFile);
			try (InputStream whitelistFileStream = SerializationWhitelist.class.getClassLoader().getResourceAsStream(path)) {
				if (whitelistFileStream == null) {
					throw new RuntimeException("Whitelist file not found on classpath: " + path);
				}
				return new BufferedReader(new InputStreamReader(whitelistFileStream, StandardCharsets.UTF_8))
								.lines()
								.collect(toSet());
			}
			catch (IOException e) {
				throw new RuntimeException("Unable to load whitelist from classpath: " + whitelistFile, e);
			}
		}

		private static Collection<String> readFileWhitelistItems(String whitelistFile) {
			try {
				return new HashSet<>(Files.readAllLines(Paths.get(whitelistFile)));
			}
			catch (IOException e) {
				LOG.error("Unable to read serialization whitelist: {}", whitelistFile);
				throw new RuntimeException(e);
			}
		}

		private static String classpathFilepath(String whitelistFile) {
			String path = whitelistFile.substring(CLASSPATH_PREFIX.length());
			if (path.startsWith("/")) {
				path = path.substring(1);
			}
			if (path.contains("/")) {
				throw new IllegalArgumentException("Whitelist file must be in the classpath root");
			}

			return path;
		}
	}
}
