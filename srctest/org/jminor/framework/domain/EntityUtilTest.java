/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.framework.Configuration;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.empdept.domain.EmpDept;

import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class EntityUtilTest {

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final EntityUtil.EntityBeanMapper beanMapper = createEmpDeptBeanMapper();

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";

    final Entity department = Entities.entity(EmpDept.T_DEPARTMENT);
    department.setValue(EmpDept.DEPARTMENT_ID, deptNo);
    department.setValue(EmpDept.DEPARTMENT_NAME, deptName);
    department.setValue(EmpDept.DEPARTMENT_LOCATION, deptLocation);

    final List<Object> deptBeans = beanMapper.toBeans(Arrays.asList(department));
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

    final Entity employee = Entities.entity(EmpDept.T_EMPLOYEE);
    employee.setValue(EmpDept.EMPLOYEE_ID, id);
    employee.setValue(EmpDept.EMPLOYEE_COMMISSION, commission);
    employee.setValue(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
    employee.setValue(EmpDept.EMPLOYEE_HIREDATE, hiredate);
    employee.setValue(EmpDept.EMPLOYEE_JOB, job);
    employee.setValue(EmpDept.EMPLOYEE_MGR, manager);
    employee.setValue(EmpDept.EMPLOYEE_NAME, name);
    employee.setValue(EmpDept.EMPLOYEE_SALARY, salary);

    final List<Object> empBeans = beanMapper.toBeans(Arrays.asList(employee));
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

    final List<Entity> departments = beanMapper.toEntities(Arrays.asList(departmentBean));
    final Entity department = departments.get(0);
    assertEquals(deptNo, department.getValue(EmpDept.DEPARTMENT_ID));
    assertEquals(deptName, department.getValue(EmpDept.DEPARTMENT_NAME));
    assertEquals(deptLocation, department.getValue(EmpDept.DEPARTMENT_LOCATION));

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

    final List<Entity> employees = beanMapper.toEntities(Arrays.asList(employeeBean));
    final Entity employee = employees.get(0);
    assertEquals(id, employee.getValue(EmpDept.EMPLOYEE_ID));
    assertEquals(commission, employee.getValue(EmpDept.EMPLOYEE_COMMISSION));
    assertEquals(deptNo, employee.getValue(EmpDept.EMPLOYEE_DEPARTMENT));
    assertEquals(hiredate, employee.getValue(EmpDept.EMPLOYEE_HIREDATE));
    assertEquals(job, employee.getValue(EmpDept.EMPLOYEE_JOB));
    assertEquals(manager, employee.getValue(EmpDept.EMPLOYEE_MGR));
    assertEquals(name, employee.getValue(EmpDept.EMPLOYEE_NAME));
    assertEquals(salary, employee.getValue(EmpDept.EMPLOYEE_SALARY));

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
  public void setPropertyNullBeanClass() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(null, "", "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setPropertyNullPropertyID() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(EmployeeBean.class, null, "");
  }

  @Test(expected = IllegalArgumentException.class)
  public void setPropertyNullPropertyName() {
    final EntityUtil.EntityBeanMapper beanMapper = new EntityUtil.EntityBeanMapper();
    beanMapper.setProperty(EmployeeBean.class, "", null);
  }

  @Test
  public void getPropertyValues() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();
    final List<Object> values = new ArrayList<Object>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
      entity.setValue(EmpDept.DEPARTMENT_ID, i);
      values.add(i);
      entities.add(entity);
    }
    final Property property = Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID);
    Collection<Object> propertyValues = EntityUtil.getPropertyValues(EmpDept.DEPARTMENT_ID, entities);
    assertTrue(propertyValues.containsAll(values));
    propertyValues = EntityUtil.getPropertyValues(property, entities);
    assertTrue(propertyValues.containsAll(values));
  }

  @Test
  public void getDistinctPropertyValues() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();
    final List<Object> values = new ArrayList<Object>();

    Entity entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, null);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 2);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entity);

    entity = Entities.entity(EmpDept.T_DEPARTMENT);
    entity.setValue(EmpDept.DEPARTMENT_ID, 4);
    entities.add(entity);

    values.add(1);
    values.add(2);
    values.add(3);
    values.add(4);

    Collection<Object> propertyValues = EntityUtil.getDistinctPropertyValues(EmpDept.DEPARTMENT_ID, entities);
    assertEquals(4, propertyValues.size());
    assertTrue(propertyValues.containsAll(values));

    propertyValues = EntityUtil.getDistinctPropertyValues(EmpDept.DEPARTMENT_ID, entities, true);
    assertEquals(5, propertyValues.size());
    values.add(null);
    assertTrue(propertyValues.containsAll(values));
  }

  @Test
  public void testSetPropertyValue() {
    EmpDept.init();
    final Collection<Entity> entities = new ArrayList<Entity>();
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    entities.add(Entities.entity(EmpDept.T_DEPARTMENT));
    EntityUtil.setPropertyValue(EmpDept.DEPARTMENT_ID, 1, entities);
    for (final Entity entity : entities) {
      assertEquals(Integer.valueOf(1), entity.getIntValue(EmpDept.DEPARTMENT_ID));
    }
    EntityUtil.setPropertyValue(EmpDept.DEPARTMENT_ID, null, entities);
    for (final Entity entity : entities) {
      assertTrue(entity.isValueNull(EmpDept.DEPARTMENT_ID));
    }
  }

  @Test
  public void hashByPropertyValue() {
    EmpDept.init();
    final List<Entity> entities = new ArrayList<Entity>();

    final Entity entityOne = Entities.entity(EmpDept.T_DEPARTMENT);
    entityOne.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entityOne);

    final Entity entityTwo = Entities.entity(EmpDept.T_DEPARTMENT);
    entityTwo.setValue(EmpDept.DEPARTMENT_ID, 1);
    entities.add(entityTwo);

    final Entity entityThree = Entities.entity(EmpDept.T_DEPARTMENT);
    entityThree.setValue(EmpDept.DEPARTMENT_ID, 2);
    entities.add(entityThree);

    final Entity entityFour = Entities.entity(EmpDept.T_DEPARTMENT);
    entityFour.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entityFour);

    final Entity entityFive = Entities.entity(EmpDept.T_DEPARTMENT);
    entityFive.setValue(EmpDept.DEPARTMENT_ID, 3);
    entities.add(entityFive);

    final Map<Object, Collection<Entity>> map = EntityUtil.hashByPropertyValue(EmpDept.DEPARTMENT_ID, entities);
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
  public void hashByEntitID() {
    Chinook.init();
    final Entity one = Entities.entity(Chinook.T_ALBUM);
    final Entity two = Entities.entity(Chinook.T_ARTIST);
    final Entity three = Entities.entity(Chinook.T_CUSTOMER);
    final Entity four = Entities.entity(Chinook.T_ALBUM);
    final Entity five = Entities.entity(Chinook.T_ARTIST);

    final Collection<Entity> entities = Arrays.asList(one, two, three, four, five);
    final Map<String, Collection<Entity>> map = EntityUtil.hashByEntityID(entities);

    Collection<Entity> hashed = map.get(Chinook.T_ALBUM);
    assertTrue(hashed.contains(one));
    assertTrue(hashed.contains(four));

    hashed = map.get(Chinook.T_ARTIST);
    assertTrue(hashed.contains(two));
    assertTrue(hashed.contains(five));

    hashed = map.get(Chinook.T_CUSTOMER);
    assertTrue(hashed.contains(three));
  }

  @Test
  public void getProperties() {
    EmpDept.init();
    final List<String> propertyIDs = new ArrayList<String>();
    propertyIDs.add(EmpDept.DEPARTMENT_ID);
    propertyIDs.add(EmpDept.DEPARTMENT_NAME);

    final Collection<Property> properties = EntityUtil.getProperties(EmpDept.T_DEPARTMENT, propertyIDs);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_ID)));
    assertTrue(properties.contains(Entities.getProperty(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME)));

    final Collection<Property> noProperties = EntityUtil.getProperties(EmpDept.T_DEPARTMENT, null);
    assertEquals(0, noProperties.size());
  }

  @Test(expected = RuntimeException.class)
  public void getEntitySerializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, null);
    EntityUtil.getEntitySerializer();
  }

  @Test(expected = RuntimeException.class)
  public void getEntityDeserializerUnconfigured() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, null);
    EntityUtil.getEntityDeserializer();
  }

  @Test
  public void getEntitySerializer() {
    Configuration.setValue(Configuration.ENTITY_SERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntitySerializer());
    Configuration.clearValue(Configuration.ENTITY_SERIALIZER_CLASS);
  }

  @Test
  public void getEntityDeserializer() {
    Configuration.setValue(Configuration.ENTITY_DESERIALIZER_CLASS, "org.jminor.framework.plugins.json.EntityJSONParser");
    assertNotNull(EntityUtil.getEntityDeserializer());
    Configuration.clearValue(Configuration.ENTITY_DESERIALIZER_CLASS);
  }

  private EntityUtil.EntityBeanMapper createEmpDeptBeanMapper() {
    final EntityUtil.EntityBeanMapper beanMap = new EntityUtil.EntityBeanMapper();
    beanMap.setEntityID(DepartmentBean.class, EmpDept.T_DEPARTMENT);
    beanMap.setProperty(DepartmentBean.class, EmpDept.DEPARTMENT_ID, "deptNo");
    beanMap.setProperty(DepartmentBean.class, EmpDept.DEPARTMENT_NAME, "name");
    beanMap.setProperty(DepartmentBean.class, EmpDept.DEPARTMENT_LOCATION, "location");

    beanMap.setEntityID(EmployeeBean.class, EmpDept.T_EMPLOYEE);
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_ID, "id");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_COMMISSION, "commission");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_DEPARTMENT, "deptno");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_HIREDATE, "hiredate");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_JOB, "job");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_MGR, "mgr");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_NAME, "name");
    beanMap.setProperty(EmployeeBean.class, EmpDept.EMPLOYEE_SALARY, "salary");

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
