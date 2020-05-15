/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

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
