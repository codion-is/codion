/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

public class Department {

  private Integer deptNo;
  private String name;
  private String location;
  private Boolean active;

  public Department() {}

  public Integer getDeptNo() {
    return deptNo;
  }

  public void setDeptNo(final Integer deptNo) {
    this.deptNo = deptNo;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(final String location) {
    this.location = location;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Boolean getActive() {
    return active;
  }

  public void setActive(final Boolean active) {
    this.active = active;
  }
}
