/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;

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

  static {
    TestDomain.init();
  }

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";

    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, deptNo);
    department.setValue(TestDomain.DEPARTMENT_NAME, deptName);
    department.setValue(TestDomain.DEPARTMENT_LOCATION, deptLocation);

    final List<Object> deptBeans = beanMapper.toBeans(Collections.singletonList(department));
    final DepartmentBean departmentBean = (DepartmentBean) deptBeans.get(0);
    assertEquals(deptNo, departmentBean.getDeptNo());
    assertEquals(deptName, departmentBean.getName());
    assertEquals(deptLocation, departmentBean.getLocation());

    final Integer id = 42;
    final Double commission = 42.2;
    final Date hiredate = new Date();
    final String job = "job";
    final Integer manager = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = Entities.entity(TestDomain.T_EMP);
    employee.setValue(TestDomain.EMP_ID, id);
    employee.setValue(TestDomain.EMP_COMMISSION, commission);
    employee.setValue(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.setValue(TestDomain.EMP_HIREDATE, hiredate);
    employee.setValue(TestDomain.EMP_JOB, job);
    employee.setValue(TestDomain.EMP_MGR, manager);
    employee.setValue(TestDomain.EMP_NAME, name);
    employee.setValue(TestDomain.EMP_SALARY, salary);

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

    assertNull(beanMapper.toBean(null));

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
    assertEquals(deptNo, department.getValue(TestDomain.DEPARTMENT_ID));
    assertEquals(deptName, department.getValue(TestDomain.DEPARTMENT_NAME));
    assertEquals(deptLocation, department.getValue(TestDomain.DEPARTMENT_LOCATION));

    final Integer id = 42;
    final Double commission = 42.2;
    final Date hiredate = new Date();
    final String job = "job";
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
    assertEquals(id, employee.getValue(TestDomain.EMP_ID));
    assertEquals(commission, employee.getValue(TestDomain.EMP_COMMISSION));
    assertEquals(deptNo, employee.getValue(TestDomain.EMP_DEPARTMENT));
    assertEquals(hiredate, employee.getValue(TestDomain.EMP_HIREDATE));
    assertEquals(job, employee.getValue(TestDomain.EMP_JOB));
    assertEquals(manager, employee.getValue(TestDomain.EMP_MGR));
    assertEquals(name, employee.getValue(TestDomain.EMP_NAME));
    assertEquals(salary, employee.getValue(TestDomain.EMP_SALARY));

    assertNull(beanMapper.toEntity(null));

    final List<Entity> empty = beanMapper.toEntities(null);
    assertTrue(empty.isEmpty());
  }

  @Test(expected = IllegalArgumentException.class)
  public void getBeanClassUndefinedEntity() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.getBeanClass("entityID");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEntityIDNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setEntityID(null, "entityID");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setEntityIDNullEntityID() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setEntityID(EmployeeBean.class, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getEntityIDNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.getEntityID(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getBeanClassNullEntityID() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.getBeanClass(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPropertyMapNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.getPropertyMap(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setPropertyNullBeanClass() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(null, "", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setPropertyNullPropertyID() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(EmployeeBean.class, null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setPropertyNullPropertyName() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(EmployeeBean.class, "", null);
  }

  @Test
  public void getPropertyValues() {
    TestDomain.init();
    final List<Entity> entities = new ArrayList<>();
    final List<Object> values = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
      entity.setValue(TestDomain.DEPARTMENT_ID, i);
      values.add(i);
      entities.add(entity);
    }
    final Property property = Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    Collection<Integer> propertyValues = EntityUtil.getPropertyValues(TestDomain.DEPARTMENT_ID, entities);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = EntityUtil.getPropertyValues(property, entities);
    assertTrue(propertyValues.containsAll(values));
    assertTrue(EntityUtil.getPropertyValues(TestDomain.DEPARTMENT_ID, null).isEmpty());
    assertTrue(EntityUtil.getPropertyValues(TestDomain.DEPARTMENT_ID, Collections.<Entity>emptyList()).isEmpty());
  }

  @Test
  public void isPrimaryKeyModified() {
    assertFalse(EntityUtil.isPrimaryKeyModified(null));
    assertFalse(EntityUtil.isPrimaryKeyModified(Collections.<Entity>emptyList()));

    final Entity department = Entities.entity(TestDomain.T_DEPARTMENT);
    department.setValue(TestDomain.DEPARTMENT_ID, 1);
    department.setValue(TestDomain.DEPARTMENT_NAME, "name");
    department.setValue(TestDomain.DEPARTMENT_LOCATION, "loc");
    assertFalse(EntityUtil.isPrimaryKeyModified(Collections.singletonList(department)));

    department.setValue(TestDomain.DEPARTMENT_NAME, "new name");
    assertFalse(EntityUtil.isPrimaryKeyModified(Collections.singletonList(department)));

    department.setValue(TestDomain.DEPARTMENT_ID, 2);
    assertTrue(EntityUtil.isPrimaryKeyModified(Collections.singletonList(department)));

    department.revertValue(TestDomain.DEPARTMENT_ID);
    assertFalse(EntityUtil.isPrimaryKeyModified(Collections.singletonList(department)));
  }

  @Test
  public void getDistinctPropertyValues() {
    TestDomain.init();
    final List<Entity> entities = new ArrayList<>();
    final List<Object> values = new ArrayList<>();

    Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, null);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 2);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 4);
    entities.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Integer> propertyValues = EntityUtil.getDistinctPropertyValues(TestDomain.DEPARTMENT_ID, entities);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = EntityUtil.getDistinctPropertyValues(TestDomain.DEPARTMENT_ID, entities, true);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));

    assertEquals(0, EntityUtil.getDistinctPropertyValues(TestDomain.DEPARTMENT_ID, null, true).size());
    assertEquals(0, EntityUtil.getDistinctPropertyValues(TestDomain.DEPARTMENT_ID, new ArrayList<Entity>(), true).size());
  }

  @Test
  public void getSortedProperties() {
    TestDomain.init();
    final List<Property> properties = EntityUtil.getSortedProperties(TestDomain.T_EMP,
            Arrays.asList(TestDomain.EMP_HIREDATE, TestDomain.EMP_COMMISSION,
                    TestDomain.EMP_SALARY, TestDomain.EMP_JOB));
    assertEquals(TestDomain.EMP_COMMISSION, properties.get(0).getPropertyID());
    assertEquals(TestDomain.EMP_HIREDATE, properties.get(1).getPropertyID());
    assertEquals(TestDomain.EMP_JOB, properties.get(2).getPropertyID());
    assertEquals(TestDomain.EMP_SALARY, properties.get(3).getPropertyID());
  }

  @Test
  public void copyEntities() {
    TestDomain.init();
    final Entity dept1 = Entities.entity(TestDomain.T_DEPARTMENT);
    dept1.setValue(TestDomain.DEPARTMENT_ID, 1);
    dept1.setValue(TestDomain.DEPARTMENT_LOCATION, "location");
    dept1.setValue(TestDomain.DEPARTMENT_NAME, "name");
    final Entity dept2 = Entities.entity(TestDomain.T_DEPARTMENT);
    dept2.setValue(TestDomain.DEPARTMENT_ID, 2);
    dept2.setValue(TestDomain.DEPARTMENT_LOCATION, "location2");
    dept2.setValue(TestDomain.DEPARTMENT_NAME, "name2");

    final List<Entity> copies = EntityUtil.copyEntities(Arrays.asList(dept1, dept2));
    assertFalse(copies.get(0) == dept1);
    assertTrue(copies.get(0).propertyValuesEqual(dept1));
    assertFalse(copies.get(1) == dept2);
    assertTrue(copies.get(1).propertyValuesEqual(dept2));
  }

  @Test
  public void testSetPropertyValue() {
    TestDomain.init();
    final Collection<Entity> entities = new ArrayList<>();
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    entities.add(Entities.entity(TestDomain.T_DEPARTMENT));
    EntityUtil.setPropertyValue(TestDomain.DEPARTMENT_ID, 1, entities);
    for (final Entity entity : entities) {
      assertEquals(Integer.valueOf(1), entity.getIntValue(TestDomain.DEPARTMENT_ID));
    }
    EntityUtil.setPropertyValue(TestDomain.DEPARTMENT_ID, null, entities);
    for (final Entity entity : entities) {
      assertTrue(entity.isValueNull(TestDomain.DEPARTMENT_ID));
    }
  }

  @Test
  public void mapToPropertyValue() {
    TestDomain.init();
    final List<Entity> entities = new ArrayList<>();

    final Entity entityOne = Entities.entity(TestDomain.T_DEPARTMENT);
    entityOne.setValue(TestDomain.DEPARTMENT_ID, 1);
    entities.add(entityOne);

    final Entity entityTwo = Entities.entity(TestDomain.T_DEPARTMENT);
    entityTwo.setValue(TestDomain.DEPARTMENT_ID, 1);
    entities.add(entityTwo);

    final Entity entityThree = Entities.entity(TestDomain.T_DEPARTMENT);
    entityThree.setValue(TestDomain.DEPARTMENT_ID, 2);
    entities.add(entityThree);

    final Entity entityFour = Entities.entity(TestDomain.T_DEPARTMENT);
    entityFour.setValue(TestDomain.DEPARTMENT_ID, 3);
    entities.add(entityFour);

    final Entity entityFive = Entities.entity(TestDomain.T_DEPARTMENT);
    entityFive.setValue(TestDomain.DEPARTMENT_ID, 3);
    entities.add(entityFive);

    final Map<Integer, Collection<Entity>> map = EntityUtil.mapToPropertyValue(TestDomain.DEPARTMENT_ID, entities);
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
    TestDomain.init();
    final Entity one = Entities.entity(TestDomain.T_EMP);
    final Entity two = Entities.entity(TestDomain.T_DEPARTMENT);
    final Entity three = Entities.entity(TestDomain.T_DETAIL);
    final Entity four = Entities.entity(TestDomain.T_EMP);

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
  public void getProperties() {
    TestDomain.init();
    final List<String> propertyIDs = new ArrayList<>();
    propertyIDs.add(TestDomain.DEPARTMENT_ID);
    propertyIDs.add(TestDomain.DEPARTMENT_NAME);

    final Collection<Property> properties = EntityUtil.getProperties(TestDomain.T_DEPARTMENT, propertyIDs);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID)));
    assertTrue(properties.contains(Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));

    final Collection<Property> noProperties = EntityUtil.getProperties(TestDomain.T_DEPARTMENT, null);
    assertEquals(0, noProperties.size());
  }

  @Test(expected = RuntimeException.class)
  public void getEntitySerializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, null);
    EntityUtil.getEntitySerializer();
  }

  @Test
  public void setNull() {
    final Entity dept = Entities.entity(TestDomain.T_DEPARTMENT);
    for (final Property property : Entities.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertFalse(dept.containsValue(property));
      assertTrue(dept.isValueNull(property));
    }
    EntityUtil.setNull(dept);
    assertFalse(dept.isModified());
    for (final Property property : Entities.getProperties(TestDomain.T_DEPARTMENT, true)) {
      assertTrue(dept.containsValue(property));
      assertTrue(dept.isValueNull(property));
    }
  }

  @Test
  public void getModifiedProperty() {
    final Entity entity = Entities.entity(TestDomain.T_DEPARTMENT);
    entity.setValue(TestDomain.DEPARTMENT_ID, 1);
    entity.setValue(TestDomain.DEPARTMENT_LOCATION, "Location");
    entity.setValue(TestDomain.DEPARTMENT_NAME, "Name");

    final Entity current = Entities.entity(TestDomain.T_DEPARTMENT);
    current.setValue(TestDomain.DEPARTMENT_ID, 1);
    current.setValue(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.setValue(TestDomain.DEPARTMENT_NAME, "Name");

    assertFalse(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertFalse(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertFalse(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_NAME));

    current.setValue(TestDomain.DEPARTMENT_ID, 2);
    current.saveAll();
    assertTrue(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertEquals(EntityUtil.getModifiedProperty(current, entity).getPropertyID(), TestDomain.DEPARTMENT_ID);
    current.removeValue(TestDomain.DEPARTMENT_ID);
    current.saveAll();
    assertTrue(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertEquals(EntityUtil.getModifiedProperty(current, entity).getPropertyID(), TestDomain.DEPARTMENT_ID);
    current.setValue(TestDomain.DEPARTMENT_ID, 1);
    current.saveAll();
    assertFalse(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_ID));
    assertNull(EntityUtil.getModifiedProperty(current, entity));

    current.setValue(TestDomain.DEPARTMENT_LOCATION, "New location");
    current.saveAll();
    assertTrue(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(EntityUtil.getModifiedProperty(current, entity).getPropertyID(), TestDomain.DEPARTMENT_LOCATION);
    current.removeValue(TestDomain.DEPARTMENT_LOCATION);
    current.saveAll();
    assertTrue(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertEquals(EntityUtil.getModifiedProperty(current, entity).getPropertyID(), TestDomain.DEPARTMENT_LOCATION);
    current.setValue(TestDomain.DEPARTMENT_LOCATION, "Location");
    current.saveAll();
    assertFalse(EntityUtil.isValueMissingOrModified(current, entity, TestDomain.DEPARTMENT_LOCATION));
    assertNull(EntityUtil.getModifiedProperty(current, entity));
  }

  private EntityUtil.EntityBeanMapper createEmpDeptBeanMapper() throws NoSuchMethodException {
    final EntityUtil.EntityBeanMapper beanMap = new EntityUtil.EntityBeanMapper();
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
