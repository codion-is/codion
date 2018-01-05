/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;

public class EntityBeanMapperTest {

  private static final Entities entities = new TestDomain();

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";

    final Entity department = entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, deptNo);
    department.put(TestDomain.DEPARTMENT_NAME, deptName);
    department.put(TestDomain.DEPARTMENT_LOCATION, deptLocation);

    final List<Object> deptBeans = beanMapper.toBeans(Collections.singletonList(department));
    final DepartmentBean departmentBean = (DepartmentBean) deptBeans.get(0);
    assertEquals(deptNo, departmentBean.getDeptNo());
    assertEquals(deptName, departmentBean.getName());
    assertEquals(deptLocation, departmentBean.getLocation());

    final Integer id = 42;
    final Double commission = 42.2;
    final Date hiredate = new Date();
    final String job = "CLERK";
    final Integer manager = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = entities.entity(TestDomain.T_EMP);
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

    final DepartmentBean departmentBean = new DepartmentBean();
    departmentBean.setDeptNo(deptNo);
    departmentBean.setLocation(deptLocation);
    departmentBean.setName(deptName);

    final List<Entity> departments = beanMapper.toEntities(Collections.singletonList(departmentBean));
    final Entity department = departments.get(0);
    assertEquals(deptNo, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals(deptName, department.get(TestDomain.DEPARTMENT_NAME));
    assertEquals(deptLocation, department.get(TestDomain.DEPARTMENT_LOCATION));

    final Integer id = 42;
    final Double commission = 42.2;
    final Date hiredate = new Date();
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

  @Test(expected = NullPointerException.class)
  public void testNullEntity() throws NoSuchMethodException, IllegalAccessException, InstantiationException,
          InvocationTargetException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertNull(beanMapper.toBean(null));
  }

  @Test(expected = NullPointerException.class)
  public void testNullBean() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    final EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertNull(beanMapper.toEntity(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getBeanClassUndefinedEntity() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.getBeanClass("entityId");
  }

  @Test(expected = NullPointerException.class)
  public void setEntityIDNullBeanClass() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.setEntityId(null, "entityId");
  }

  @Test(expected = NullPointerException.class)
  public void setEntityIDNullEntityId() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.setEntityId(EmployeeBean.class, null);
  }

  @Test(expected = NullPointerException.class)
  public void getEntityIDNullBeanClass() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.getEntityId(null);
  }

  @Test(expected = NullPointerException.class)
  public void getBeanClassNullEntityId() {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.getBeanClass(null);
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullBeanClass() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.setProperty(null, "", "");
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullPropertyId() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.setProperty(EmployeeBean.class, null, "");
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullPropertyName() throws NoSuchMethodException {
    final EntityBeanMapper beanMapper = new EntityBeanMapper(entities);
    beanMapper.setProperty(EmployeeBean.class, "", null);
  }

  private EntityBeanMapper createEmpDeptBeanMapper() throws NoSuchMethodException {
    final EntityBeanMapper beanMap = new EntityBeanMapper(entities);
    beanMap.setEntityId(DepartmentBean.class, TestDomain.T_DEPARTMENT);
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_ID, "deptNo");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_NAME, "name");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_LOCATION, "location");

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
  }

  static class EmployeeBean {

    private Integer id;
    private String name;
    private String job;
    private Integer mgr;
    private Date hiredate;
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

    public Date getHiredate() {
      return hiredate;
    }

    public void setHiredate(final Date hiredate) {
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
