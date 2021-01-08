/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.version;

/**
 * Specifies a version and serves as a factory class for {@link Version} instances.
 */
public interface Version extends Comparable<Version> {

  /**
   * @return the major part of this version
   */
  int getMajor();

  /**
   * @return the minor part of this version
   */
  int getMinor();

  /**
   * @return the patch part of this version
   */
  int getPatch();

  /**
   * @return the metadata part of this version
   */
  String getMetadata();

  /**
   * Instantiates a new version [major].0.0
   * @param major the major version
   * @return a Version
   */
  static Version version(final int major) {
    return version(major, 0);
  }

  /**
   * Instantiates a new version [major].[minor].0
   * @param major the major version
   * @param minor the minor version
   * @return a Version
   */
  static Version version(final int major, final int minor) {
    return version(major, minor, 0);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @return a Version
   */
  static Version version(final int major, final int minor, final int patch) {
    return version(major, minor, patch, null);
  }

  /**
   * Instantiates a new version [major].[minor].[patch]-[metadata]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @param metadata the metadata, fx. build information
   * @return a Version
   */
  static Version version(final int major, final int minor, final int patch, final String metadata) {
    return new DefaultVersion(major, minor, patch, metadata);
  }

  /**
   * @return a string containing the framework version number, without any version metadata (fx. build no.)
   */
  static String getVersionString() {
    final String versionString = getVersionAndBuildNumberString();
    if (versionString.toLowerCase().contains("-")) {
      return versionString.substring(0, versionString.toLowerCase().indexOf('-'));
    }

    return versionString;
  }

  /**
   * @return a string containing the framework version and version metadata
   */
  static String getVersionAndBuildNumberString() {
    return DefaultVersion.VERSION.toString();
  }

  /**
   * @return the framework Version
   */
  static Version getVersion() {
    return DefaultVersion.VERSION;
  }

  /**
   * Parses a string on the form x.y.z-metadata
   * @param versionString the version string
   * @return a Version based on the given string
   */
  static Version parse(final String versionString) {
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

    return new DefaultVersion(getIntValue(versionSplit, 0), getIntValue(versionSplit, 1),
            getIntValue(versionSplit, 2), metadata);
  }

  static int getIntValue(final String[] splits, final int index) {
    if (splits.length > index) {
      return Integer.parseInt(splits[index]);
    }

    return 0;
  }
}
