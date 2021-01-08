/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

import java.io.IOException;
import java.io.Serializable;
import java.util.Objects;
import java.util.Properties;

final class DefaultVersion implements Version, Serializable {

  private static final long serialVersionUID = 1;

  private static final int MAGIC_NUMBER = 23;

  static final Version VERSION;

  static {
    try {
      final Properties properties = new Properties();
      properties.load(DefaultVersion.class.getResourceAsStream("version.properties"));
      VERSION = Version.parse(properties.getProperty("version"));
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private final int major;
  private final int minor;
  private final int patch;
  private final String metadata;

  /**
   * Instantiates a new version [major].[minor].[patch]-[metadata]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @param metadata the metadata, fx. build information
   */
  public DefaultVersion(final int major, final int minor, final int patch, final String metadata) {
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
  public int getMajor() {
    return major;
  }

  /**
   * @return the minor part of this version
   */
  @Override
  public int getMinor() {
    return minor;
  }

  /**
   * @return the patch part of this version
   */
  @Override
  public int getPatch() {
    return patch;
  }

  /**
   * @return the metadata part of this version
   */
  @Override
  public String getMetadata() {
    return metadata;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch + (metadata == null ? "" : "-" + metadata);
  }

  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Version && compareTo((Version) obj) == 0 && Objects.equals(metadata, ((Version) obj).getMetadata());
  }

  @Override
  public int hashCode() {
    return MAGIC_NUMBER * major + MAGIC_NUMBER * minor + MAGIC_NUMBER * patch;
  }

  @Override
  public int compareTo(final Version version) {
    int result = major - version.getMajor();
    if (result == 0) {
      result = minor - version.getMinor();
      if (result == 0) {
        result = patch - version.getPatch();
      }
    }

    return result;
  }
}
