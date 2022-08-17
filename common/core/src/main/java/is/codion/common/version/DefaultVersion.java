/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;

final class DefaultVersion implements Version, Serializable {

  private static final long serialVersionUID = 1;

  static final Version VERSION;

  static {
    try {
      Properties properties = new Properties();
      properties.load(DefaultVersion.class.getResourceAsStream("version.properties"));
      VERSION = Version.parse(properties.getProperty("version"));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

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
  public DefaultVersion(int major, int minor, int patch, String metadata) {
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
