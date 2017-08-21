/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityUtilTest {

  private static final Entities entities = new TestDomain();

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

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
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

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
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertNull(beanMapper.toBean(null));
  }

  @Test(expected = NullPointerException.class)
  public void testNullBean() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();
    assertNull(beanMapper.toEntity(null));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getBeanClassUndefinedEntity() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.getBeanClass("entityID");
  }

  @Test(expected = NullPointerException.class)
  public void setEntityIDNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.setEntityID(null, "entityID");
  }

  @Test(expected = NullPointerException.class)
  public void setEntityIDNullEntityID() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.setEntityID(EmployeeBean.class, null);
  }

  @Test(expected = NullPointerException.class)
  public void getEntityIDNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.getEntityID(null);
  }

  @Test(expected = NullPointerException.class)
  public void getBeanClassNullEntityID() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.getBeanClass(null);
  }

  @Test(expected = NullPointerException.class)
  public void getPropertyMapNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.getPropertyMap(null);
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullBeanClass() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.setProperty(null, "", "");
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullPropertyID() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.setProperty(EmployeeBean.class, null, "");
  }

  @Test(expected = NullPointerException.class)
  public void setPropertyNullPropertyName() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper(entities);
    beanMapper.setProperty(EmployeeBean.class, "", null);
  }

  @Test
  public void getPropertyValues() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = entities.entity(TestDomain.T_DEPARTMENT);
      entity.put(TestDomain.DEPARTMENT_ID, i);
      values.add(i);
      entityList.add(entity);
    }
    final Property property = entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    Collection<Integer> propertyValues = EntityUtil.getValues(TestDomain.DEPARTMENT_ID, entityList);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = EntityUtil.getValues(property.getPropertyID(), entityList);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(EntityUtil.getValues(TestDomain.DEPARTMENT_ID, null).isEmpty());
    assertTrue(EntityUtil.getValues(TestDomain.DEPARTMENT_ID, Collections.<Entity>emptyList()).isEmpty());
  }

  @Test
  public void getDistinctPropertyValues() {
    final List<Entity> entityList = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    Entity entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, null);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 2);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entity);

    entity = entities.entity(TestDomain.T_DEPARTMENT);
    entity.put(TestDomain.DEPARTMENT_ID, 4);
    entityList.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> propertyValues = EntityUtil.getDistinctValues(TestDomain.DEPARTMENT_ID, entityList);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = EntityUtil.getDistinctValues(TestDomain.DEPARTMENT_ID, entityList, true);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, EntityUtil.getDistinctValues(TestDomain.DEPARTMENT_ID, null, true).size());
    assertEquals(0, EntityUtil.getDistinctValues(TestDomain.DEPARTMENT_ID, new ArrayList<>(), true).size());
  }

  @Test
  public void getStringValueArray() {
    final Entity dept1 = entities.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    dept1.put(TestDomain.DEPARTMENT_NAME, "name1");
    dept1.put(TestDomain.DEPARTMENT_LOCATION, "loc1");
    final Entity dept2 = entities.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);
    dept2.put(TestDomain.DEPARTMENT_NAME, "name2");
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "loc2");

    final String[][] strings = EntityUtil.getStringValueArray(entities.getColumnProperties(TestDomain.T_DEPARTMENT), Arrays.asList(dept1, dept2));
    assertEquals("1", strings[0][0]);
    assertEquals("name1", strings[0][1]);
    assertEquals("loc1", strings[0][2]);
    assertEquals("2", strings[1][0]);
    assertEquals("name2", strings[1][1]);
    assertEquals("loc2", strings[1][2]);
  }

  @Test
  public void copyEntities() {
    final Entity dept1 = entities.entity(TestDomain.T_DEPARTMENT);
    dept1.put(TestDomain.DEPARTMENT_ID, 1);
    dept1.put(TestDomain.DEPARTMENT_LOCATION, "location");
    dept1.put(TestDomain.DEPARTMENT_NAME, "name");
    final Entity dept2 = entities.entity(TestDomain.T_DEPARTMENT);
    dept2.put(TestDomain.DEPARTMENT_ID, 2);
    dept2.put(TestDomain.DEPARTMENT_LOCATION, "location2");
    dept2.put(TestDomain.DEPARTMENT_NAME, "name2");

    final List<Entity> copies = EntityUtil.copyEntities(Arrays.asList(dept1, dept2));
    assertFalse(copies.get(0) == dept1);
    assertTrue(copies.get(0).valuesEqual(dept1));
    assertFalse(copies.get(1) == dept2);
    assertTrue(copies.get(1).valuesEqual(dept2));
  }

  @Test
  public void testSetPropertyValue() {
    final Collection<Entity> collection = new ArrayList<>();
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    collection.add(entities.entity(TestDomain.T_DEPARTMENT));
    EntityUtil.put(TestDomain.DEPARTMENT_ID, 1, collection);
    for (final Entity entity : collection) {
      assertEquals(Integer.valueOf(1), entity.getInteger(TestDomain.DEPARTMENT_ID));
    }
    EntityUtil.put(TestDomain.DEPARTMENT_ID, null, collection);
    for (final Entity entity : collection) {
      assertTrue(entity.isValueNull(TestDomain.DEPARTMENT_ID));
    }
  }

  @Test
  public void mapToPropertyValue() {
    final List<Entity> entityList = new ArrayList<>();

    final Entity entityOne = entities.entity(TestDomain.T_DEPARTMENT);
    entityOne.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entityOne);

    final Entity entityTwo = entities.entity(TestDomain.T_DEPARTMENT);
    entityTwo.put(TestDomain.DEPARTMENT_ID, 1);
    entityList.add(entityTwo);

    final Entity entityThree = entities.entity(TestDomain.T_DEPARTMENT);
    entityThree.put(TestDomain.DEPARTMENT_ID, 2);
    entityList.add(entityThree);

    final Entity entityFour = entities.entity(TestDomain.T_DEPARTMENT);
    entityFour.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entityFour);

    final Entity entityFive = entities.entity(TestDomain.T_DEPARTMENT);
    entityFive.put(TestDomain.DEPARTMENT_ID, 3);
    entityList.add(entityFive);

    final Map<Integer, Collection<Entity>> map = EntityUtil.mapToValue(TestDomain.DEPARTMENT_ID, entityList);
    final Collection<Entity> ones = map.get(1);
    assertTrue(ones.contains(entityOne));
    assertTrue(ones.contains(entityTwo));

    final Collection<Entity> twos = map.get(2);
    assertTrue(twos.contains(entityThree));

    final Collection<Entity> threes = map.get(3);
    assertTrue(threes.contains(entityFour));
    assertTrue(threes.contains(entityFive));
  }

  @Test
  public void mapToEntitID() {
    final Entity one = entities.entity(TestDomain.T_EMP);
    final Entity two = entities.entity(TestDomain.T_DEPARTMENT);
    final Entity three = entities.entity(TestDomain.T_DETAIL);
    final Entity four = entities.entity(TestDomain.T_EMP);

    final Collection<Entity> entities = Arrays.asList(one, two, three, four);
    final Map<String, Collection<Entity>> map = EntityUtil.mapToEntityID(entities);

    Collection<Entity> mapped = map.get(TestDomain.T_EMP);
    assertTrue(mapped.contains(one));
    assertTrue(mapped.contains(four));

    mapped = map.get(TestDomain.T_DEPARTMENT);
    assertTrue(mapped.contains(two));

    mapped = map.get(TestDomain.T_DETAIL);
    assertTrue(mapped.contains(three));
  }

  @Test
  public void putNull() {
    final Entity dept = entities.entity(TestDomain.T_DEPARTMENT);
    for (final Property property : entities.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertFalse(dept.containsKey(property));
      assertTrue(dept.isValueNull(property));
    }
    for (final Property property : entities.getProperties(TestDomain.T_DEPARTMENT, true)) {
      dept.put(property, null);
    }
    //putting nulls should not have an effect
    assertFalse(dept.isModified());
    for (final Property property : entities.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertTrue(dept.containsKey(property));
      assertTrue(dept.isValueNull(property));
    }
  }

  private EntityUtil.EntityBeanMapper createEmpDeptBeanMapper() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMap = new EntityUtil.EntityBeanMapper(entities);
    beanMap.setEntityID(DepartmentBean.class, TestDomain.T_DEPARTMENT);
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_ID, "deptNo");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_NAME, "name");
    beanMap.setProperty(DepartmentBean.class, TestDomain.DEPARTMENT_LOCATION, "location");

    beanMap.setEntityID(EmployeeBean.class, TestDomain.T_EMP);
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
