/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
 * A class encapsulating a version.<br>
 * User: Bjorn Darri<br>
 * Date: 4.4.2010<br>
 * Time: 14:32:05<br>
 */
public class Version implements Serializable {

  private static final long serialVersionUID = 1;

  private final String domain;
  private final String version;

  public Version(final String domain, final String version) {
    this.domain = domain;
    this.version = version;
  }

  public String getDomain() {
    return domain;
  }

  public String getVersion() {
    return version;
  }

  @Override
  public String toString() {
    return new StringBuilder().append("@").append(domain).append("-").append(version).toString();
  }
}
