/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * A simple version class for use semantic versioning (http://semver.org)
 */
public final class Version implements Comparable<Version>, Serializable {

  private static final long serialVersionUID = 1;

  private static final int MAGIC_NUMBER = 23;

  private static final int MAJOR_INDEX = 0;
  private static final int MINOR_INDEX = 1;
  private static final int PATCH_INDEX = 2;

  private final int major;
  private final int minor;
  private final int patch;
  private final String metadata;

  /**
   * Instantiates a new version [major].0.0
   * @param major the major version
   */
  public Version(final int major) {
    this(major, 0);
  }

  /**
   * Instantiates a new version [major].[minor].0
   * @param major the major version
   * @param minor the minor version
   */
  public Version(final int major, final int minor) {
    this(major, minor, 0);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   */
  public Version(final int major, final int minor, final int patch) {
    this(major, minor, patch, null);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]-[metadata]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @param metadata the metadata, fx. build information
   */
  public Version(final int major, final int minor, final int patch, final String metadata) {
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
  public int getMajor() {
    return major;
  }

  /**
   * @return the minor part of this version
   */
  public int getMinor() {
    return minor;
  }

  /**
   * @return the patch part of this version
   */
  public int getPatch() {
    return patch;
  }

  /**
   * @return the metadata part of this version
   */
  public String getMetadata() {
    return metadata;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return major + "." + minor + "." + patch + (metadata == null ? "" : "-" + metadata);
  }

  /** {@inheritDoc} */
  @Override
  public boolean equals(final Object obj) {
    return obj instanceof Version && compareTo((Version) obj) == 0;
  }

  /** {@inheritDoc} */
  @Override
  public int hashCode() {
    return MAGIC_NUMBER * major + MAGIC_NUMBER * minor + MAGIC_NUMBER * patch;
  }

  /** {@inheritDoc} */
  @Override
  public int compareTo(final Version version) {
    int result = major - version.major;
    if (result == 0) {
      result = minor - version.minor;
      if (result == 0) {
        result = patch - version.patch;
      }
    }

    return result;
  }

  /**
   * Parses a string on the form x.y.z-metadata
   * @param versionString the version string
   * @return a Version based on the given string
   */
  public static Version parse(final String versionString) {
    if (Util.nullOrEmpty(versionString)) {
      throw new IllegalArgumentException("Invalid version string: " + versionString);
    }
    final String[] metadataSplit = versionString.trim().split("-");
    final String[] versionSplit = metadataSplit[0].split("\\.");

    return new Version(getIntValue(versionSplit, MAJOR_INDEX), getIntValue(versionSplit, MINOR_INDEX),
            getIntValue(versionSplit, PATCH_INDEX), metadataSplit.length > 1 ? metadataSplit[1] : null);
  }

  private static int getIntValue(final String[] splits, final int index) {
    if (splits.length > index) {
      return Integer.parseInt(splits[index]);
    }

    return 0;
  }
}
