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
 * Copyright (c) 2018 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.toList;

final class SerializationFilterDryRun {

	private static final Logger LOG = LoggerFactory.getLogger(SerializationFilterDryRun.class);

	private static final String CLASSPATH_PREFIX = "classpath:";

	private SerializationFilterDryRun() {}

	/**
	 * Creates a serialization filter for a whitelist dry run.
	 * @param patternFile the file to write the dry-run results to
	 * @throws IllegalArgumentException in case of a classpath dry run file
	 */
	static DryRun whitelistDryRun(String patternFile) {
		return new DryRun(patternFile);
	}

	static void handleDryRun() {
		ObjectInputFilter serialFilter = ObjectInputFilter.Config.getSerialFilter();
		if (serialFilter instanceof DryRun) {
			((DryRun) serialFilter).writeToFile();
		}
	}

	static final class DryRun implements ObjectInputFilter {

		private final String patternFile;
		private final Set<Class<?>> deserializedClasses = newKeySet();

		private DryRun(String patternFile) {
			if (requireNonNull(patternFile).toLowerCase().startsWith(CLASSPATH_PREFIX)) {
				throw new IllegalArgumentException("Filter dry run can not be performed with a classpath result file: " + patternFile);
			}
			this.patternFile = patternFile;
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
		void writeToFile() {
			try {
				Files.write(Paths.get(patternFile), deserializedClasses.stream()
								.map(Class::getName)
								.sorted()
								.collect(toList()), StandardOpenOption.CREATE);
				LOG.info("Serialization dryrun result written to file: {}", patternFile);
			}
			catch (Exception e) {
				LOG.error("Error while writing dryrun results to file: {}", patternFile, e);
			}
		}
	}
}
