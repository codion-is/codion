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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.common.utilities.version;

import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a version and serves as a factory for {@link Version.Builder} instances.
 */
public interface Version extends Comparable<Version> {

	/**
	 * The key for a version property in a properties file.
	 * @see #parse(Class, String)
	 */
	String VERSION_PROPERTY_KEY = "version";

	/**
	 * @return the major part of this version
	 */
	int major();

	/**
	 * @return the minor part of this version
	 */
	int minor();

	/**
	 * @return the patch part of this version
	 */
	int patch();

	/**
	 * @return the metadata part of this version or an empty Optional in case of no metadata
	 */
	Optional<String> metadata();

	/**
	 * @return the build information part of this version or an empty Optional in case of no build information
	 */
	Optional<String> build();

	/**
	 * Builds a Version.
	 */
	interface Builder {

		/**
		 * @param major the major version component
		 * @return this builder instance
		 */
		Builder major(int major);

		/**
		 * @param minor the minor version component
		 * @return this builder instance
		 */
		Builder minor(int minor);

		/**
		 * @param patch the patch version component
		 * @return this builder instance
		 */
		Builder patch(int patch);

		/**
		 * @param metadata the metadata version component
		 * @return this builder instance
		 */
		Builder metadata(@Nullable String metadata);

		/**
		 * @param build the build information version component
		 * @return this builder instance
		 */
		Builder build(@Nullable String build);

		/**
		 * @return a new {@link Version} instance based on this builder
		 */
		Version build();
	}

	/**
	 * @return a new {@link Version.Builder} instance.
	 */
	static Builder builder() {
		return new DefaultVersion.DefaultBuilder();
	}

	/**
	 * @return a string containing the framework version number, without any version metadata (fx. build no.)
	 */
	static String versionString() {
		String versionString = versionAndMetadataString();
		if (versionString.toLowerCase().contains("-")) {
			return versionString.substring(0, versionString.toLowerCase().indexOf('-'));
		}

		return versionString;
	}

	/**
	 * @return a string containing the framework version and version metadata
	 */
	static String versionAndMetadataString() {
		return DefaultVersion.VERSION.toString();
	}

	/**
	 * @return the framework Version
	 */
	static Version version() {
		return DefaultVersion.VERSION;
	}

	/**
	 * Parses a string on the form x.y.z-metadata+build
	 * @param versionString the version string
	 * @return a Version based on the given string
	 * @throws NullPointerException in case {@code versionString} is null
	 * @throws IllegalArgumentException in case {@code versionString} is empty
	 */
	static Version parse(String versionString) {
		if (requireNonNull(versionString).isEmpty()) {
			throw new IllegalArgumentException("Empty version string: " + versionString);
		}
		String version;
		String metadata;
		String build;
		int dashIndex = versionString.indexOf('-');
		int plusIndex = versionString.indexOf('+');
		if (dashIndex != -1 && plusIndex != -1) {
			// Both metadata and build info
			version = versionString.substring(0, dashIndex);
			metadata = versionString.substring(dashIndex + 1, plusIndex);
			build = versionString.substring(plusIndex + 1);
		}
		else if (dashIndex != -1) {
			// Only metadata
			version = versionString.substring(0, dashIndex);
			metadata = versionString.substring(dashIndex + 1);
			build = null;
		}
		else if (plusIndex != -1) {
			// Only build info
			version = versionString.substring(0, plusIndex);
			build = versionString.substring(plusIndex + 1);
			metadata = null;
		}
		else {
			// Neither metadata nor build info
			version = versionString;
			metadata = null;
			build = null;
		}
		String[] versionSplit = version.split("\\.");

		int major = versionSplit.length > 0 ? Integer.parseInt(versionSplit[0]) : 0;
		int minor = versionSplit.length > 1 ? Integer.parseInt(versionSplit[1]) : 0;
		int patch = versionSplit.length > 2 ? Integer.parseInt(versionSplit[2]) : 0;

		return builder()
						.major(major)
						.minor(minor)
						.patch(patch)
						.metadata(metadata)
						.build(build)
						.build();
	}

	/**
	 * Reads a properties file from the classpath and parses the value associated with the 'version' key.
	 * @param resourceOwner the resource owning class
	 * @param versionFileResourcePath the resource path to the file containing the version, prefix with '/' for classpath root
	 * @return a {@link Version} instance parsed from the value associated with the 'version' property key found in the given resource
	 * @throws IllegalArgumentException in case the properties resource is not found or if no 'version' property key is found
	 */
	static Version parse(Class<?> resourceOwner, String versionFileResourcePath) {
		try (InputStream resourceStream = requireNonNull(resourceOwner).getResourceAsStream(requireNonNull(versionFileResourcePath))) {
			if (resourceStream == null) {
				throw new IllegalArgumentException("Version resource not found: " + resourceOwner + ", " + versionFileResourcePath);
			}
			Properties properties = new Properties();
			properties.load(resourceStream);
			String version = properties.getProperty(VERSION_PROPERTY_KEY);
			if (version == null) {
				throw new IllegalArgumentException("No '" + VERSION_PROPERTY_KEY + "' property found: " + resourceOwner + ", " + versionFileResourcePath);
			}

			return Version.parse(version);
		}
		catch (IOException e) {
			throw new UncheckedIOException("Unable to parse version information", e);
		}
	}
}
