/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * A factory class for Version
 */
public final class Versions {

  private static final int MAJOR_INDEX = 0;
  private static final int MINOR_INDEX = 1;
  private static final int PATCH_INDEX = 2;

  public static Version version() {
    return version(0);
  }

  /**
   * Instantiates a new version [major].0.0
   * @param major the major version
   */
  public static Version version(final int major) {
    return version(major, 0);
  }

  /**
   * Instantiates a new version [major].[minor].0
   *
   * @param major the major version
   * @param minor the minor version
   */
  public static Version version(final int major, final int minor) {
    return version(major, minor, 0);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]
   *
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   */
  public static Version version(final int major, final int minor, final int patch) {
    return version(major, minor, patch, null);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]-[metadata]
   * @param major    the major version
   * @param minor    the minor version
   * @param patch    the patch version
   * @param metadata the metadata, fx. build information
   */
  public static Version version(final int major, final int minor, final int patch, final String metadata) {
    return new DefaultVersion(major, minor, patch, metadata);
  }

  /**
   * Parses a string on the form x.y.z-metadata
   * @param versionString the version string
   * @return a Version based on the given string
   */
  public static Version version(final String versionString) {
    if (versionString == null || versionString.isEmpty()) {
      throw new IllegalArgumentException("Invalid version string: " + versionString);
    }
    final String[] metadataSplit = versionString.trim().split("-");
    final String[] versionSplit = metadataSplit[0].split("\\.");

    return version(getIntValue(versionSplit, MAJOR_INDEX), getIntValue(versionSplit, MINOR_INDEX),
            getIntValue(versionSplit, PATCH_INDEX), metadataSplit.length > 1 ? metadataSplit[1] : null);
  }

  private static int getIntValue(final String[] splits, final int index) {
    if (splits.length > index) {
      return Integer.parseInt(splits[index]);
    }

    return 0;
  }

  private static final class DefaultVersion implements Version, Comparable<Version>, Serializable {

    private static final long serialVersionUID = 1;

    private static final int MAGIC_NUMBER = 23;

    private final int major;
    private final int minor;
    private final int patch;
    private final String metadata;

    private DefaultVersion(final int major, final int minor, final int patch, final String metadata) {
      if (major < 0 || minor < 0 || patch < 0) {
        throw new IllegalArgumentException("Major, minor and patch must be non-negative integers");
      }
      this.major = major;
      this.minor = minor;
      this.patch = patch;
      this.metadata = metadata;
    }

    @Override
    public int getMajor() {
      return major;
    }

    @Override
    public int getMinor() {
      return minor;
    }

    @Override
    public int getPatch() {
      return patch;
    }

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
      return obj instanceof Version && compareTo((Version) obj) == 0;
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
}
