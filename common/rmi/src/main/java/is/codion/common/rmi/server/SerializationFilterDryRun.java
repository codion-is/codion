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

import is.codion.common.utilities.scheduler.TaskScheduler;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ObjectInputFilter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static java.lang.Runtime.getRuntime;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.util.stream.Collectors.toList;

final class SerializationFilterDryRun implements ObjectInputFilter {

	private static final Logger LOG = LoggerFactory.getLogger(SerializationFilterDryRun.class);

	private static final String CLASSPATH_PREFIX = "classpath:";

	private final String patternFile;
	private final Set<Class<?>> deserializedClasses = newKeySet();
	private final @Nullable TaskScheduler flushScheduler;

	SerializationFilterDryRun(String patternFile, boolean writeOnShutdown) {
		this(patternFile, writeOnShutdown, 0);
	}

	SerializationFilterDryRun(String patternFile, boolean writeOnShutdown, int flushInterval) {
		if (requireNonNull(patternFile).toLowerCase().startsWith(CLASSPATH_PREFIX)) {
			throw new IllegalArgumentException("Filter dry run can not be performed with a classpath result file: " + patternFile);
		}
		if (flushInterval < 0) {
			throw new IllegalArgumentException("Flush interval must be non-negative: " + flushInterval);
		}
		this.patternFile = patternFile;
		this.flushScheduler = createFlushScheduler(flushInterval);
		if (writeOnShutdown) {
			getRuntime().addShutdownHook(new Thread(this::onShutdown));
		}
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
	 * Writes all classnames found during the dry-run to the output file.
	 */
	void writeToFile() {
		try {
			Files.write(Paths.get(patternFile), deserializedClasses.stream()
							.map(Class::getName)
							.sorted()
							.collect(toList()), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
			LOG.info("Serialization dryrun result written to file: {} ({} classes)", patternFile, deserializedClasses.size());
		}
		catch (Exception e) {
			LOG.error("Error while writing dryrun results to file: {}", patternFile, e);
		}
	}

	private void flushToFile() {
		if (!deserializedClasses.isEmpty()) {
			LOG.debug("Flushing serialization dryrun results");
			writeToFile();
		}
	}

	private void onShutdown() {
		if (flushScheduler != null) {
			flushScheduler.stop();
		}
		writeToFile();
	}

	private @Nullable TaskScheduler createFlushScheduler(int flushInterval) {
		if (flushInterval > 0) {
			LOG.info("Serialization dryrun flush scheduled every {} seconds", flushInterval);
			return TaskScheduler.builder()
							.task(this::flushToFile)
							.interval(flushInterval, TimeUnit.SECONDS)
							.initialDelay(flushInterval)
							.name("SerializationFilterDryRun-flush")
							.start();
		}

		return null;
	}
}
