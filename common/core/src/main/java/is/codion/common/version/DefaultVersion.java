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

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;

final class DefaultVersion implements Version, Serializable {

	@Serial
	private static final long serialVersionUID = 1;

	static final Version VERSION = Version.parse(DefaultVersion.class, "version.properties");

	private final int major;
	private final int minor;
	private final int patch;
	private final String metadata;
	private final String build;

	private DefaultVersion(DefaulBuilder builder) {
		this.major = builder.major;
		this.minor = builder.minor;
		this.patch = builder.patch;
		this.metadata = builder.metadata;
		this.build = builder.build;
	}

	@Override
	public int major() {
		return major;
	}

	@Override
	public int minor() {
		return minor;
	}

	@Override
	public int patch() {
		return patch;
	}

	@Override
	public Optional<String> metadata() {
		return Optional.ofNullable(metadata);
	}

	@Override
	public Optional<String> build() {
		return Optional.ofNullable(build);
	}

	@Override
	public String toString() {
		return major + "." + minor + "." + patch + (metadata == null ? "" : "-" + metadata) + (build == null ? "" : "+" + build);
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

		return major == that.major && minor == that.minor && patch == that.patch &&
						Objects.equals(metadata, that.metadata) && Objects.equals(build, that.build);
	}

	@Override
	public int hashCode() {
		return Objects.hash(major, minor, patch, metadata, build);
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
				result = compareMetadata(metadata, version.metadata().orElse(null));
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

	static final class DefaulBuilder implements Builder {

		private int major = 0;
		private int minor = 0;
		private int patch = 0;
		private String metadata;
		private String build;

		@Override
		public Builder major(int major) {
			if (major < 0) {
				throw new IllegalArgumentException("Major must be a non-negative integer");
			}
			this.major = major;
			return this;
		}

		@Override
		public Builder minor(int minor) {
			if (minor < 0) {
				throw new IllegalArgumentException("Minor must be a non-negative integer");
			}
			this.minor = minor;
			return this;
		}

		@Override
		public Builder patch(int patch) {
			if (patch < 0) {
				throw new IllegalArgumentException("Patch must be a non-negative integer");
			}
			this.patch = patch;
			return this;
		}

		@Override
		public Builder metadata(String metadata) {
			this.metadata = metadata;
			return this;
		}

		@Override
		public Builder build(String build) {
			this.build = build;
			return this;
		}

		@Override
		public Version build() {
			return new DefaultVersion(this);
		}
	}
}
