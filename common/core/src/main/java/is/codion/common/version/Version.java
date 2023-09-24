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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.version;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import static java.util.Objects.requireNonNull;

/**
 * Specifies a version and serves as a factory class for {@link Version} instances.
 */
public interface Version extends Comparable<Version> {

  /**
   * The key for a version property in a properties file.
   * @see #parsePropertiesFile(Class, String)
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
   * @return the metadata part of this version
   */
  String metadata();

  /**
   * Creates a new version [major].0.0
   * @param major the major version
   * @return a Version
   */
  static Version version(int major) {
    return version(major, 0);
  }

  /**
   * Creates a new version [major].[minor].0
   * @param major the major version
   * @param minor the minor version
   * @return a Version
   */
  static Version version(int major, int minor) {
    return version(major, minor, 0);
  }

  /**
   * Creates a new version [major].[minor].[patch]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @return a Version
   */
  static Version version(int major, int minor, int patch) {
    return version(major, minor, patch, null);
  }

  /**
   * Creates a new version [major].[minor].[patch]-[metadata]
   * @param major the major version
   * @param minor the minor version
   * @param patch the patch version
   * @param metadata the metadata, fx. build information
   * @return a Version
   */
  static Version version(int major, int minor, int patch, String metadata) {
    return new DefaultVersion(major, minor, patch, metadata);
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
   * Parses a string on the form x.y.z-metadata
   * @param versionString the version string
   * @return a Version based on the given string
   */
  static Version parse(String versionString) {
    if (versionString == null || versionString.isEmpty()) {
      throw new IllegalArgumentException("Invalid version string: " + versionString);
    }
    String version;
    String metadata;
    int dashIndex = versionString.indexOf('-');
    if (dashIndex > 0) {
      version = versionString.substring(0, dashIndex);
      metadata = versionString.substring(dashIndex + 1);
    }
    else {
      int spaceIndex = versionString.indexOf(' ');
      if (spaceIndex > 0) {
        version = versionString.substring(0, spaceIndex);
        metadata = versionString.substring(spaceIndex + 1);
      }
      else {
        version = versionString;
        metadata = null;
      }
    }
    String[] versionSplit = version.split("\\.");

    int major = versionSplit.length > 0 ? Integer.parseInt(versionSplit[0]) : 0;
    int minor = versionSplit.length > 1 ? Integer.parseInt(versionSplit[1]) : 0;
    int patch = versionSplit.length > 2 ? Integer.parseInt(versionSplit[2]) : 0;

    return new DefaultVersion(major, minor, patch, metadata);
  }

  /**
   * Reads a properties file from the classpath and parses the value associated with the 'version' key.
   * @param resourceOwner the resource owning class
   * @param resourcePath the resource path, prefix with '/' for classpath root
   * @return a {@link Version} instance parsed from the value associated with the 'version' property key found in the given resource
   * @throws IllegalArgumentException in case the properties resource is not found or if no 'version' property key is found
   */
  static Version parsePropertiesFile(Class<?> resourceOwner, String resourcePath) {
    try (InputStream resourceStream = requireNonNull(resourceOwner).getResourceAsStream(requireNonNull(resourcePath))) {
      if (resourceStream == null) {
        throw new IllegalArgumentException("Version resource not found: " + resourceOwner + ", " + resourcePath);
      }
      Properties properties = new Properties();
      properties.load(resourceStream);
      String version = properties.getProperty(VERSION_PROPERTY_KEY);
      if (version == null) {
        throw new IllegalArgumentException("No '" + VERSION_PROPERTY_KEY + "' property found: " + resourceOwner + ", " + resourcePath);
      }

      return Version.parse(version);
    }
    catch (IOException e) {
      throw new RuntimeException("Unable to parse version information", e);
    }
  }
}
