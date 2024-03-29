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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.version;

import java.io.Serializable;
import java.util.Objects;

final class DefaultVersion implements Version, Serializable {

	private static final long serialVersionUID = 1;

	static final Version VERSION = Version.parsePropertiesFile(DefaultVersion.class, "version.properties");

	private final int major;
	private final int minor;
	private final int patch;
	private final String metadata;

	/**
	 * Creates a new version [major].[minor].[patch]-[metadata]
	 * @param major the major version
	 * @param minor the minor version
	 * @param patch the patch version
	 * @param metadata the metadata, fx. build information
	 */
	DefaultVersion(int major, int minor, int patch, String metadata) {
		if (major < 0 || minor < 0 || patch < 0) {
			throw new IllegalArgumentException("Major, minor and patch must be non-negative integers");
		}
		this.major = major;
		this.minor = minor;
		this.patch = patch;
		this.metadata = metadata;
	}

	/**
	 * @return the major part of this version
	 */
	@Override
	public int major() {
		return major;
	}

	/**
	 * @return the minor part of this version
	 */
	@Override
	public int minor() {
		return minor;
	}

	/**
	 * @return the patch part of this version
	 */
	@Override
	public int patch() {
		return patch;
	}

	/**
	 * @return the metadata part of this version
	 */
	@Override
	public String metadata() {
		return metadata;
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch + (metadata == null ? "" : "-" + metadata);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		DefaultVersion that = (DefaultVersion) obj;

		return major == that.major && minor == that.minor && patch == that.patch && Objects.equals(metadata, that.metadata);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, metadata);
	}

	@Override
	public int compareTo(Version version) {
		int result = Integer.compare(major, version.major());
		if (result == 0) {
			result = Integer.compare(minor, version.minor());
			if (result == 0) {
				result = Integer.compare(patch, version.patch());
			}
			if (result == 0) {
				result = compareMetadata(metadata, version.metadata());
			}
		}

		return result;
	}

	private static int compareMetadata(String metadata, String toCompare) {
		if (metadata != null && toCompare != null) {
			return metadata.compareToIgnoreCase(toCompare);
		}
		if (metadata != null && toCompare == null) {
			return -1;
		}
		if (metadata == null && toCompare != null) {
			return 1;
		}

		return 0;
	}
}
