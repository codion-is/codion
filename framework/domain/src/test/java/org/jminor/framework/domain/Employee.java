/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import java.time.LocalDateTime;

public class Employee {

  private Integer id;
  private String name;
  private String job;
  private Integer mgr;
  private LocalDateTime hiredate;
  private Double salary;
  private Double commission;
  private Integer deptno;

  public Employee() {}

  public Double getCommission() {
    return commission;
  }

  public void setCommission(final Double commission) {
    this.commission = commission;
  }

  public Integer getDeptno() {
    return deptno;
  }

  public void setDeptno(final Integer deptno) {
    this.deptno = deptno;
  }

  public LocalDateTime getHiredate() {
    return hiredate;
  }

  public void setHiredate(final LocalDateTime hiredate) {
    this.hiredate = hiredate;
  }

  public Integer getId() {
    return id;
  }

  public void setId(final Integer id) {
    this.id = id;
  }

  public String getJob() {
    return job;
  }

  public void setJob(final String job) {
    this.job = job;
  }

  public Integer getMgr() {
    return mgr;
  }

  public void setMgr(final Integer mgr) {
    this.mgr = mgr;
  }

  public String getName() {
    return name;
  }

  public void setName(final String name) {
    this.name = name;
  }

  public Double getSalary() {
    return salary;
  }

  public void setSalary(final Double salary) {
    this.salary = salary;
  }
}
