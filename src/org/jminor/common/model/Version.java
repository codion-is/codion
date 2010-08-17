/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * A class specifying a version.
 */
public final class Version implements Serializable {

  private static final long serialVersionUID = 1;

  private final String domain;
  private final String version;

  /**
   * Instantiates a new Version object.
   * @param domain the domain this version object should represent
   * @param version the version string
   */
  public Version(final String domain, final String version) {
    this.domain = domain;
    this.version = version;
  }

  /**
   * @return the domain this version represents
   */
  public String getDomain() {
    return domain;
  }

  /**
   * @return the version string
   */
  public String getVersion() {
    return version;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return new StringBuilder().append("@").append(domain).append("-").append(version).toString();
  }
}
