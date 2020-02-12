/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

/**
 * A simple version class
 */
public final class Version implements Comparable<Version>, Serializable {

  private static final long serialVersionUID = 1;

  private static final int MAGIC_NUMBER = 23;

  private static final int MAJOR_INDEX = 0;
  private static final int MINOR_INDEX = 1;
  private static final int PATCH_INDEX = 2;

  private static final Version VERSION;

  static {
    try {
      final Properties properties = new Properties();
      properties.load(Version.class.getResourceAsStream("version.properties"));
      VERSION = parse(properties.getProperty("version"));
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
   * @return a string containing the framework version number, without any version metadata (fx. build no.)
   */
  public static String getVersionString() {
    final String versionString = getVersionAndBuildNumberString();
    if (versionString.toLowerCase().contains("-")) {
      return versionString.substring(0, versionString.toLowerCase().indexOf('-'));
    }

    return versionString;
  }

  /**
   * @return a string containing the framework version and version metadata
   */
  public static String getVersionAndBuildNumberString() {
    return VERSION.toString();
  }

  /**
   * @return the framework Version
   */
  public static Version getVersion() {
    return VERSION;
  }

  /**
   * Parses a string on the form x.y.z-metadata
   * @param versionString the version string
   * @return a Version based on the given string
   */
  public static Version parse(final String versionString) {
    if (versionString == null || versionString.isEmpty()) {
      throw new IllegalArgumentException("Invalid version string: " + versionString);
    }
    final String version;
    final String metadata;
    final int dashIndex = versionString.indexOf('-');
    if (dashIndex > 0) {
      version = versionString.substring(0, dashIndex);
      metadata = versionString.substring(dashIndex + 1);
    }
    else {
      final int spaceIndex = versionString.indexOf(' ');
      if (spaceIndex > 0) {
        version = versionString.substring(0, spaceIndex);
        metadata = versionString.substring(spaceIndex + 1);
      }
      else {
        version = versionString;
        metadata = null;
      }
    }
    final String[] versionSplit = version.split("\\.");

    return new Version(getIntValue(versionSplit, MAJOR_INDEX), getIntValue(versionSplit, MINOR_INDEX),
            getIntValue(versionSplit, PATCH_INDEX), metadata);
  }

  private static int getIntValue(final String[] splits, final int index) {
    if (splits.length > index) {
      return Integer.parseInt(splits[index]);
    }

    return 0;
  }
}
