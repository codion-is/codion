/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.domain;

import java.io.Serializable;

public final class Department implements Serializable {

  private static final long serialVersionUID = 1;

  private Integer id;
  private String name;
  private String location;

  public Integer getId() {
    return id;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }
}
