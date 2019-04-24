/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class EntityBeanMapperTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Entity department = DOMAIN.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, deptNo);
    department.put(TestDomain.DEPARTMENT_NAME, deptName);
    department.put(TestDomain.DEPARTMENT_LOCATION, deptLocation);
    department.put(TestDomain.DEPARTMENT_ACTIVE, deptActive);

    final List<Object> deptBeans = beanMapper.toBeans(Collections.singletonList(department));
    final DepartmentBean departmentBean = (DepartmentBean) deptBeans.get(0);
    assertEquals(deptNo, departmentBean.getDeptNo());
    assertEquals(deptName, departmentBean.getName());
    assertEquals(deptLocation, departmentBean.getLocation());
    assertEquals(deptActive, departmentBean.getActive());

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer manager = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = DOMAIN.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, id);
    employee.put(TestDomain.EMP_COMMISSION, commission);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_HIREDATE, hiredate);
    employee.put(TestDomain.EMP_JOB, job);
    employee.put(TestDomain.EMP_MGR, manager);
    employee.put(TestDomain.EMP_NAME, name);
    employee.put(TestDomain.EMP_SALARY, salary);

    final List<Object> empBeans = beanMapper.toBeans(Collections.singletonList(employee));
    final EmployeeBean employeeBean = (EmployeeBean) empBeans.get(0);
    assertEquals(id, employeeBean.getId());
    assertEquals(commission, employeeBean.getCommission());
    assertEquals(deptNo, employeeBean.getDeptno());
    assertEquals(hiredate, employeeBean.getHiredate());
    assertEquals(job, employeeBean.getJob());
    assertEquals(manager, employeeBean.getMgr());
    assertEquals(name, employeeBean.getName());
    assertEquals(salary, employeeBean.getSalary());

    final List<Object> empty = beanMapper.toBeans(null);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void toEntities() throws InvocationTargetException, NoSuchMethodException,
          IllegalAccessException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final DepartmentBean departmentBean = new DepartmentBean();
    departmentBean.setDeptNo(deptNo);
    departmentBean.setLocation(deptLocation);
    departmentBean.setName(deptName);
    departmentBean.setActive(deptActive);

    final List<Entity> departments = beanMapper.toEntities(Collections.singletonList(departmentBean));
    final Entity department = departments.get(0);
    assertEquals(deptNo, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals(deptName, department.get(TestDomain.DEPARTMENT_NAME));
    assertEquals(deptLocation, department.get(TestDomain.DEPARTMENT_LOCATION));
    assertEquals(deptActive, department.get(TestDomain.DEPARTMENT_ACTIVE));

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer manager = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final EmployeeBean employeeBean = new EmployeeBean();
    employeeBean.setId(id);
    employeeBean.setCommission(commission);
    employeeBean.setDeptno(deptNo);
    employeeBean.setHiredate(hiredate);
    employeeBean.setJob(job);
    employeeBean.setMgr(manager);
    employeeBean.setName(name);
    employeeBean.setSalary(salary);

    final List<Entity> employees = beanMapper.toEntities(Collections.singletonList(employeeBean));
    final Entity employee = employees.get(0);
    assertEquals(id, employee.get(TestDomain.EMP_ID));
    assertEquals(commission, employee.get(TestDomain.EMP_COMMISSION));
    assertEquals(deptNo, employee.get(TestDomain.EMP_DEPARTMENT));
    assertEquals(hiredate, employee.get(TestDomain.EMP_HIREDATE));
    assertEquals(job, employee.get(TestDomain.EMP_JOB));
    assertEquals(manager, employee.get(TestDomain.EMP_MGR));
    assertEquals(name, employee.get(TestDomain.EMP_NAME));
    assertEquals(salary, employee.get(TestDomain.EMP_SALARY));

    final List<Entity> empty = beanMapper.toEntities(null);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void testNullEntity() throws NoSuchMethodException, IllegalAccessException, InstantiationException,
          InvocationTargetException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertThrows(NullPointerException.class, () -> beanMapper.toBean(null));
  }

  @Test
  public void testNullBean() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertThrows(NullPointerException.class, () -> beanMapper.toEntity(null));
  }

  @Test
  public void getBeanClassUndefinedEntity() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(IllegalArgumentException.class, () -> beanMapper.getBeanClass("entityId"));
  }

  @Test
  public void setEntityIDNullBeanClass() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.setEntityId(null, "entityId"));
  }

  @Test
  public void setEntityIDNullEntityId() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.setEntityId(EmployeeBean.class, null));
  }

  @Test
  public void getEntityIDNullBeanClass() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.getEntityId(null));
  }

  @Test
  public void getBeanClassNullEntityId() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.getBeanClass(null));
  }

  @Test
  public void setPropertyNullBeanClass() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.setProperty(null, "", ""));
  }

  @Test
  public void setPropertyNullPropertyId() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.setProperty(EmployeeBean.class, null, ""));
  }

  @Test
  public void setPropertyNullPropertyName() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(DOMAIN);
    assertThrows(NullPointerException.class, () -> beanMapper.setProperty(EmployeeBean.class, "", null));
  }

  private EntityBeanMapper createEmpDeptBeanMapper() throws NoSuchMethodException {
    final EntityBeanMapper beanMap = new EntityBeanMapper(DOMAIN);
    beanMap.setEntityId(DepartmentBean.class, TestDomain.T_DEPARTMENT);
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_ID, "deptNo");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_NAME, "name");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_LOCATION, "location");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_ACTIVE, "active");

    beanMap.setEntityId(EmployeeBean.class, TestDomain.T_EMP);
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_ID, "id");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_COMMISSION, "commission");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_DEPARTMENT, "deptno");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_HIREDATE, "hiredate");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_JOB, "job");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_MGR, "mgr");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_NAME, "name");
    beanMap.setProperty(EmployeeBean.class, TestDomain.EMP_SALARY, "salary");

    return beanMap;
  }

  static class DepartmentBean {
    private Integer deptNo;
    private String name;
    private String location;
    private Boolean active;

    public DepartmentBean() {}

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

  static class EmployeeBean {

    private Integer id;
    private String name;
    private String job;
    private Integer mgr;
    private LocalDateTime hiredate;
    private Double salary;
    private Double commission;
    private Integer deptno;

    public EmployeeBean() {}

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
}
