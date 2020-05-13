/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.domain;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

public final class Employee implements Serializable {

  private static final long serialVersionUID = 1;

  private Integer id;
  private String name;
  private String job;
  private Employee manager;
  private LocalDate hiredate;
  private BigDecimal salary;
  private Double commission;
  private Department department;

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

  public String getJob() {
    return job;
  }

  public void setJob(final String job) {
    this.job = job;
  }

  public Employee getManager() {
    return manager;
  }

  public void setManager(final Employee manager) {
    this.manager = manager;
  }

  public LocalDate getHiredate() {
    return hiredate;
  }

  public void setHiredate(final LocalDate hiredate) {
    this.hiredate = hiredate;
  }

  public BigDecimal getSalary() {
    return salary;
  }

  public void setSalary(final BigDecimal salary) {
    this.salary = salary;
  }

  public Double getCommission() {
    return commission;
  }

  public void setCommission(final Double commission) {
    this.commission = commission;
  }

  public Department getDepartment() {
    return department;
  }

  public void setDepartment(final Department department) {
    this.department = department;
  }
}
