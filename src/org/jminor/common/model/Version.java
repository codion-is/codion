/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.io.Serializable;

/**
* User: Bjorn Darri
* Date: 4.4.2010
* Time: 14:32:05
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
    return new StringBuilder().append("@").append(getDomain()).append("-").append(getVersion()).toString();
  }
}
