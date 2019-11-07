/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain;

import org.jminor.common.DateFormats;
import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.DatabaseConnection;
import org.jminor.common.db.valuemap.exception.LengthValidationException;
import org.jminor.common.db.valuemap.exception.NullValidationException;
import org.jminor.common.db.valuemap.exception.RangeValidationException;
import org.jminor.common.db.valuemap.exception.ValidationException;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

  private final TestDomain domain = new TestDomain();

  @Test
  public void getWritableColumnProperties() {
    List<String> writable = domain.getWritableColumnProperties(TestDomain.T_DEPARTMENT, true, true)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertTrue(writable.contains(TestDomain.DEPARTMENT_ID));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_NAME));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_LOCATION));
    assertFalse(writable.contains(TestDomain.DEPARTMENT_ACTIVE));

    writable = domain.getWritableColumnProperties(TestDomain.T_DEPARTMENT, false, true)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertFalse(writable.contains(TestDomain.DEPARTMENT_ID));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_NAME));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_LOCATION));
    assertFalse(writable.contains(TestDomain.DEPARTMENT_ACTIVE));

    writable = domain.getWritableColumnProperties(TestDomain.T_DEPARTMENT, false, false)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertFalse(writable.contains(TestDomain.DEPARTMENT_ID));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_NAME));
    assertTrue(writable.contains(TestDomain.DEPARTMENT_LOCATION));
    assertFalse(writable.contains(TestDomain.DEPARTMENT_ACTIVE));

    writable = domain.getWritableColumnProperties(TestDomain.T_EMP, true, true)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertTrue(writable.contains(TestDomain.EMP_ID));
    assertTrue(writable.contains(TestDomain.EMP_HIREDATE));
    assertTrue(writable.contains(TestDomain.EMP_NAME));
    assertFalse(writable.contains(TestDomain.EMP_NAME_DEPARTMENT));

    writable = domain.getWritableColumnProperties(TestDomain.T_EMP, false, true)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertFalse(writable.contains(TestDomain.EMP_ID));
    assertTrue(writable.contains(TestDomain.EMP_HIREDATE));
    assertTrue(writable.contains(TestDomain.EMP_NAME));
    assertFalse(writable.contains(TestDomain.EMP_NAME_DEPARTMENT));

    writable = domain.getWritableColumnProperties(TestDomain.T_EMP, false, false)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertFalse(writable.contains(TestDomain.EMP_ID));
    assertFalse(writable.contains(TestDomain.EMP_HIREDATE));
    assertTrue(writable.contains(TestDomain.EMP_NAME));
    assertFalse(writable.contains(TestDomain.EMP_NAME_DEPARTMENT));

    writable = domain.getWritableColumnProperties(TestDomain.T_EMP, true, false)
            .stream().map(Property::getPropertyId).collect(Collectors.toList());
    assertFalse(writable.contains(TestDomain.EMP_ID));//overridden by includeNonUpdatable
    assertFalse(writable.contains(TestDomain.EMP_HIREDATE));
    assertTrue(writable.contains(TestDomain.EMP_NAME));
    assertFalse(writable.contains(TestDomain.EMP_NAME_DEPARTMENT));
  }

  @Test
  public void sortProperties() {
    final List<Property> properties = Properties.sort(domain.getProperties(TestDomain.T_EMP,
            asList(TestDomain.EMP_HIREDATE, TestDomain.EMP_COMMISSION,
                    TestDomain.EMP_SALARY, TestDomain.EMP_JOB)));
    assertEquals(TestDomain.EMP_COMMISSION, properties.get(0).getPropertyId());
    assertEquals(TestDomain.EMP_HIREDATE, properties.get(1).getPropertyId());
    assertEquals(TestDomain.EMP_JOB, properties.get(2).getPropertyId());
    assertEquals(TestDomain.EMP_SALARY, properties.get(3).getPropertyId());
  }

  @Test
  public void getUpdatableProperties() {
    final List<Property> properties = domain.getUpdatableProperties(TestDomain.T_DETAIL);
    assertEquals(9, properties.size());
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_NAME)));
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_CODE)));
    assertFalse(properties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void getSelectedProperties() {
    final List<String> propertyIds = new ArrayList<>();
    propertyIds.add(TestDomain.DEPARTMENT_ID);
    propertyIds.add(TestDomain.DEPARTMENT_NAME);

    final Collection<Property> properties = domain.getProperties(TestDomain.T_DEPARTMENT, propertyIds);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID)));
    assertTrue(properties.contains(domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));

    final Collection<Property> noProperties = domain.getProperties(TestDomain.T_DEPARTMENT, emptyList());
    assertEquals(0, noProperties.size());
  }

  @Test
  public void getEntitySerializerUnconfigured() {
    domain.ENTITY_SERIALIZER_CLASS.set(null);
    assertThrows(RuntimeException.class, domain::getEntitySerializer);
  }

  @Test
  public void key() {
    final String entityId = "DomainTest.key";
    final String propertyId1 = "id1";
    final String propertyId2 = "id2";
    final String propertyId3 = "id3";
    domain.define(entityId,
            Properties.primaryKeyProperty(propertyId1),
            Properties.primaryKeyProperty(propertyId2).setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty(propertyId3).setPrimaryKeyIndex(2).setNullable(true));

    final Entity.Key key = domain.key(entityId);
    assertEquals(0, key.hashCode());
    assertTrue(key.isCompositeKey());
    assertTrue(key.isNull());

    key.put(propertyId1, 1);
    key.put(propertyId2, 2);
    key.put(propertyId3, 3);
    assertFalse(key.isNull());
    assertEquals(6, key.hashCode());

    key.put(propertyId2, 3);
    assertEquals(7, key.hashCode());

    key.put(propertyId3, null);
    assertFalse(key.isNull());
    assertEquals(4, key.hashCode());
    key.put(propertyId2, null);
    assertTrue(key.isNull());
    assertEquals(0, key.hashCode());
    key.put(propertyId2, 4);
    assertFalse(key.isNull());
    assertEquals(5, key.hashCode());

    key.put(propertyId2, 42);
    assertFalse(key.isNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> domain.key((String) null));
  }

  @Test
  public void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("keyWithSameIndex",
            Properties.primaryKeyProperty("1").setPrimaryKeyIndex(0),
            Properties.primaryKeyProperty("2").setPrimaryKeyIndex(1),
            Properties.primaryKeyProperty("3").setPrimaryKeyIndex(1)));
  }

  @Test
  public void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("keyWithSameIndex2",
            Properties.primaryKeyProperty("1"),
            Properties.primaryKeyProperty("2"),
            Properties.primaryKeyProperty("3")));
  }

  @Test
  public void entity() {
    final Entity.Key key = domain.key(TestDomain.T_MASTER);
    key.put(TestDomain.MASTER_ID, 10L);

    final Entity master = domain.entity(key);
    assertEquals(TestDomain.T_MASTER, master.getEntityId());
    assertTrue(master.containsKey(TestDomain.MASTER_ID));
    assertEquals(10L, master.get(TestDomain.MASTER_ID));

    assertThrows(NullPointerException.class, () -> domain.entity((String) null));
  }

  @Test
  public void getProperties() {
    final Property id = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_ID);
    final Property location = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_LOCATION);
    final Property name = domain.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final List<Property> properties = domain.getProperties(TestDomain.T_DEPARTMENT, asList(TestDomain.DEPARTMENT_LOCATION, TestDomain.DEPARTMENT_NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    final Collection<Property> visibleProperties = domain.getProperties(TestDomain.T_DEPARTMENT, false);
    assertEquals(3, visibleProperties.size());
    assertTrue(visibleProperties.contains(id));
    assertTrue(visibleProperties.contains(location));
    assertTrue(visibleProperties.contains(name));

    final Collection<Property> allProperties = domain.getProperties(TestDomain.T_DEPARTMENT, true);
    assertTrue(allProperties.containsAll(visibleProperties));
    assertEquals(allProperties.size(), visibleProperties.size() + 1);
  }

  @Test
  public void getPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getProperty(TestDomain.T_MASTER, "unknown property"));
  }

  @Test
  public void getForeignKeyProperties() {
    List<Property.ForeignKeyProperty> foreignKeyProperties = domain.getForeignKeyProperties(TestDomain.T_DETAIL, TestDomain.T_EMP);
    assertEquals(0, foreignKeyProperties.size());
    foreignKeyProperties = domain.getForeignKeyProperties(TestDomain.T_DETAIL, TestDomain.T_MASTER);
    assertEquals(1, foreignKeyProperties.size());
    assertTrue(foreignKeyProperties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK)));
  }

  @Test
  public void getForeignKeyProperty() {
    assertNotNull(domain.getForeignKeyProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getForeignKeyPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getForeignKeyProperty(TestDomain.T_DETAIL, "bla bla"));
  }

  @Test
  public void getDomainEntityIds() {
    final Domain domain = Domain.getDomain(new TestDomain().getDomainId());
    assertNotNull(domain.getDefinition(TestDomain.T_DEPARTMENT));
    assertNotNull(domain.getDefinition(TestDomain.T_EMP));
  }

  @Test
  public void hasDerivedProperties() {
    assertFalse(domain.hasDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN));
    assertTrue(domain.hasDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_INT));
  }

  @Test
  public void getDerivedProperties() {
    Collection<Property.DerivedProperty> derivedProperties = domain.getDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_BOOLEAN);
    assertTrue(derivedProperties.isEmpty());
    derivedProperties = domain.getDerivedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_INT);
    assertEquals(1, derivedProperties.size());
    assertTrue(derivedProperties.contains(domain.getProperty(TestDomain.T_DETAIL, TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void hasDenormalizedProperties() {
    assertFalse(domain.hasDenormalizedProperties(TestDomain.T_DEPARTMENT));
    assertTrue(domain.hasDenormalizedProperties(TestDomain.T_DETAIL));
    assertTrue(domain.hasDenormalizedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getDenormalizedProperties() {
    final List<Property.DenormalizedProperty> denormalized = domain.getDenormalizedProperties(TestDomain.T_DETAIL, TestDomain.DETAIL_MASTER_FK);
    assertFalse(denormalized.isEmpty());
    assertEquals(TestDomain.DETAIL_MASTER_CODE_DENORM, denormalized.get(0).getPropertyId());
  }

  @Test
  public void isSmallDataset() {
    assertTrue(domain.isSmallDataset(TestDomain.T_DETAIL));
  }

  @Test
  public void getStringProvider() {
    assertNotNull(domain.getStringProvider(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void redefine() {
    final String entityId = "entityId";
    domain.define(entityId, Properties.primaryKeyProperty("propertyId"));
    assertThrows(IllegalArgumentException.class, () -> domain.define(entityId, Properties.primaryKeyProperty("propertyId")));
  }

  @Test
  public void redefineAllowed() {
    final String entityId = "entityId2";
    domain.define(entityId, Properties.primaryKeyProperty("id"));
    assertEquals("id", domain.getPrimaryKeyProperties(entityId).get(0).getPropertyId());
    domain.ALLOW_REDEFINE_ENTITY.set(true);
    domain.define(entityId, Properties.primaryKeyProperty("id2"));
    assertEquals("id2", domain.getPrimaryKeyProperties(entityId).get(0).getPropertyId());
    domain.ALLOW_REDEFINE_ENTITY.set(false);
  }

  @Test
  public void nullValidation() {
    final Entity emp = domain.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200.0);

    final Domain.Validator validator = new Domain.Validator();
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_DEPARTMENT_FK, e.getKey());
    }
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    try {
      validator.validate(emp);
    }
    catch (final ValidationException e) {
      fail();
    }
    emp.put(TestDomain.EMP_SALARY, null);
    try {
      validator.validate(emp);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_SALARY, e.getKey());
    }
  }

  @Test
  public void maxLengthValidation() {
    final Entity emp = domain.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200.0);
    final Domain.Validator validator = new Domain.Validator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp)));
    emp.put(TestDomain.EMP_NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp));
  }

  @Test
  public void rangeValidation() {
    final Entity emp = domain.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200d);
    emp.put(TestDomain.EMP_COMMISSION, 300d);
    final Domain.Validator validator = new Domain.Validator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp)));
    emp.put(TestDomain.EMP_COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
    emp.put(TestDomain.EMP_COMMISSION, 2100d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
  }

  @Test
  public void getSearchProperties() {
    Collection<Property.ColumnProperty> searchProperties = domain.getSearchProperties(TestDomain.T_EMP);
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_JOB)));
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_EMP, TestDomain.EMP_NAME)));

    searchProperties = domain.getSearchProperties(TestDomain.T_DEPARTMENT);
    //should contain all string based properties
    assertTrue(searchProperties.contains(domain.getColumnProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  public void selectableProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            domain.getSelectableColumnProperties(TestDomain.T_DETAIL, singletonList(TestDomain.DETAIL_STRING)));
  }

  @Test
  public void stringProvider() {
    final Entity department = domain.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    department.put(TestDomain.DEPARTMENT_LOCATION, "Reykjavik");
    department.put(TestDomain.DEPARTMENT_NAME, "Sales");

    final Entity employee = domain.entity(TestDomain.T_EMP);
    final LocalDateTime hiredate = LocalDateTime.now();
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_NAME, "Darri");
    employee.put(TestDomain.EMP_HIREDATE, hiredate);

    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DateFormats.SHORT_TIMESTAMP);

    Domain.StringProvider employeeToString = new Domain.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(domain.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK),
                    TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.toString(employee));

    department.put(TestDomain.DEPARTMENT_LOCATION, null);
    department.put(TestDomain.DEPARTMENT_NAME, null);

    employee.put(TestDomain.EMP_DEPARTMENT_FK, null);
    employee.put(TestDomain.EMP_NAME, null);
    employee.put(TestDomain.EMP_HIREDATE, null);

    employeeToString = new Domain.StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(domain.getForeignKeyProperty(TestDomain.T_EMP, TestDomain.EMP_DEPARTMENT_FK),
                    TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals(" (department: , location: , hiredate: )", employeeToString.toString(employee));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id"))));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntityNonStrict() {
    Entity.Definition.STRICT_FOREIGN_KEYS.set(false);
    domain.define("test.entity",
            Properties.primaryKeyProperty("id"),
            Properties.foreignKeyProperty("fk_id_fk", "caption", "test.referenced_entity",
                    Properties.columnProperty("fk_id")));
    Entity.Definition.STRICT_FOREIGN_KEYS.set(true);
  }

  @Test
  public void setSearchPropertyIdsInvalidProperty() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("spids",
            Properties.primaryKeyProperty("1"),
            Properties.columnProperty("test"))
            .setSearchPropertyIds("invalid"));
  }

  @Test
  public void hasSingleIntegerPrimaryKey() {
    String entityId = "hasSingleIntegerPrimaryKey";
    domain.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0));
    assertTrue(domain.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey2";
    domain.define(entityId,
            Properties.columnProperty("test")
                    .setPrimaryKeyIndex(0),
            Properties.columnProperty("test2")
                    .setPrimaryKeyIndex(1));
    assertFalse(domain.hasSingleIntegerPrimaryKey(entityId));
    entityId = "hasSingleIntegerPrimaryKey3";
    domain.define(entityId,
            Properties.columnProperty("test", Types.VARCHAR)
                    .setPrimaryKeyIndex(0));
    assertFalse(domain.hasSingleIntegerPrimaryKey(entityId));
  }

  @Test
  public void havingClause() {
    final String havingClause = "p1 > 1";
    domain.define("entityId3",
            Properties.primaryKeyProperty("p0")).setHavingClause(havingClause);
    assertEquals(havingClause, domain.getHavingClause("entityId3"));
  }

  @Test
  public void validateTypeEntity() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    final Entity entity1 = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_MASTER_FK, entity1));
  }

  @Test
  public void setValueDerived() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_DERIVED, 10));
  }

  @Test
  public void setValueValueList() {
    final Entity entity = domain.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_VALUE_LIST, -10));
  }

  @Test
  public void addOperationExisting() {
    final DatabaseConnection.Operation operation = new AbstractProcedure<DatabaseConnection>("operationId", "test") {
      @Override
      public void execute(final DatabaseConnection databaseConnection, final Object... arguments) {}
    };
    domain.addOperation(operation);
    assertThrows(IllegalArgumentException.class, () -> domain.addOperation(operation));
  }

  @Test
  public void getFunctionNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> domain.getFunction("nonexistingfunctionid"));
  }

  @Test
  public void getProcedureNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> domain.getProcedure("nonexistingprocedureid"));
  }

  @Test
  public void defaultEntity() {
    final Entity detail = domain.defaultEntity(TestDomain.T_DETAIL, key -> null);
    assertFalse(detail.containsKey(TestDomain.DETAIL_DOUBLE));//columnHasDefaultValue
    assertFalse(detail.containsKey(TestDomain.DETAIL_DATE));//columnHasDefaultValue
    assertTrue(detail.containsKey(TestDomain.DETAIL_BOOLEAN_NULLABLE));//columnHasDefaultValue && property.hasDefaultValue
  }

  @Test
  public void conditionProvider() {
    assertThrows(IllegalArgumentException.class, () -> domain.define("nullConditionProvider1",
            Properties.primaryKeyProperty("id")).addConditionProvider(null, values -> null));
    assertThrows(NullPointerException.class, () -> domain.define("nullConditionProvider2",
            Properties.primaryKeyProperty("id")).addConditionProvider("id", null));
    assertThrows(IllegalStateException.class, () -> domain.define("nullConditionProvider3",
            Properties.primaryKeyProperty("id"))
            .addConditionProvider("id", values -> null)
            .addConditionProvider("id", values -> null));
  }

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Entity department = domain.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, deptNo);
    department.put(TestDomain.DEPARTMENT_NAME, deptName);
    department.put(TestDomain.DEPARTMENT_LOCATION, deptLocation);
    department.put(TestDomain.DEPARTMENT_ACTIVE, deptActive);

    final List<Object> deptBeans = domain.toBeans(singletonList(department));
    final Department departmentBean = (Department) deptBeans.get(0);
    assertEquals(deptNo, departmentBean.getDeptNo());
    assertEquals(deptName, departmentBean.getName());
    assertEquals(deptLocation, departmentBean.getLocation());
    assertEquals(deptActive, departmentBean.getActive());

    final Entity manager = domain.entity(TestDomain.T_EMP);
    manager.put(TestDomain.EMP_ID, 12);

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = domain.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, id);
    employee.put(TestDomain.EMP_COMMISSION, commission);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_HIREDATE, hiredate);
    employee.put(TestDomain.EMP_JOB, job);
    employee.put(TestDomain.EMP_MGR_FK, manager);
    employee.put(TestDomain.EMP_NAME, name);
    employee.put(TestDomain.EMP_SALARY, salary);

    final List<Object> empBeans = domain.toBeans(singletonList(employee));
    final Employee employeeBean = (Employee) empBeans.get(0);
    assertEquals(id, employeeBean.getId());
    assertEquals(commission, employeeBean.getCommission());
    assertEquals(deptNo, employeeBean.getDeptno());
    assertEquals(deptNo, employeeBean.getDepartment().getDeptNo());
    assertEquals(hiredate.truncatedTo(ChronoUnit.DAYS), employeeBean.getHiredate());
    assertEquals(job, employeeBean.getJob());
    assertEquals(mgr, employeeBean.getMgr());
    assertEquals(12, employeeBean.getManager().getId());
    assertEquals(name, employeeBean.getName());
    assertEquals(salary, employeeBean.getSalary());

    final List<Object> empty = domain.toBeans(null);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void fromBeans() throws InvocationTargetException, NoSuchMethodException,
          IllegalAccessException {

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Department departmentBean = new Department();
    departmentBean.setDeptNo(deptNo);
    departmentBean.setLocation(deptLocation);
    departmentBean.setName(deptName);
    departmentBean.setActive(deptActive);

    final List<Entity> departments = domain.fromBeans(singletonList(departmentBean));
    final Entity department = departments.get(0);
    assertEquals(deptNo, department.get(TestDomain.DEPARTMENT_ID));
    assertEquals(deptName, department.get(TestDomain.DEPARTMENT_NAME));
    assertEquals(deptLocation, department.get(TestDomain.DEPARTMENT_LOCATION));
    assertEquals(deptActive, department.get(TestDomain.DEPARTMENT_ACTIVE));

    final Employee manager = new Employee();
    manager.setId(12);

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Employee employeeBean = new Employee();
    employeeBean.setId(id);
    employeeBean.setCommission(commission);
    employeeBean.setDeptno(deptNo);
    employeeBean.setDepartment(departmentBean);
    employeeBean.setHiredate(hiredate);
    employeeBean.setJob(job);
    employeeBean.setMgr(mgr);
    employeeBean.setManager(manager);
    employeeBean.setName(name);
    employeeBean.setSalary(salary);

    final List<Entity> employees = domain.fromBeans(singletonList(employeeBean));
    final Entity employee = employees.get(0);
    assertEquals(id, employee.get(TestDomain.EMP_ID));
    assertEquals(commission, employee.get(TestDomain.EMP_COMMISSION));
    assertEquals(deptNo, employee.get(TestDomain.EMP_DEPARTMENT));
    assertEquals(deptNo, employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK)
            .getInteger(TestDomain.DEPARTMENT_ID));
    assertEquals(hiredate, employee.get(TestDomain.EMP_HIREDATE));
    assertEquals(job, employee.get(TestDomain.EMP_JOB));
    assertEquals(mgr, employee.get(TestDomain.EMP_MGR));
    assertEquals(12, employee.getForeignKey(TestDomain.EMP_MGR_FK).getInteger(TestDomain.EMP_ID));
    assertEquals(name, employee.get(TestDomain.EMP_NAME));
    assertEquals(salary, employee.get(TestDomain.EMP_SALARY));

    final List<Entity> empty = domain.fromBeans(null);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void testNullEntity() throws NoSuchMethodException, IllegalAccessException, InstantiationException,
          InvocationTargetException {
    assertThrows(NullPointerException.class, () -> domain.toBean(null));
  }

  @Test
  public void testNullBean() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    assertThrows(NullPointerException.class, () -> domain.fromBean(null));
  }
}
