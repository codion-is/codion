/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity;

import is.codion.common.Serializer;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.KeyTest;
import is.codion.framework.domain.TestDomainExtended;
import is.codion.framework.domain.entity.exception.ItemValidationException;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class EntitiesTest {

  private final Entities entities = new TestDomain().entities();

  @Test
  void defineTypes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);

    //assert types
    assertEquals(definition.property(TestDomain.Detail.ID).attribute().valueClass(), Long.class);
    assertEquals(definition.property(TestDomain.Detail.SHORT).attribute().valueClass(), Short.class);
    assertEquals(definition.property(TestDomain.Detail.INT).attribute().valueClass(), Integer.class);
    assertEquals(definition.property(TestDomain.Detail.DOUBLE).attribute().valueClass(), Double.class);
    assertEquals(definition.property(TestDomain.Detail.STRING).attribute().valueClass(), String.class);
    assertEquals(definition.property(TestDomain.Detail.DATE).attribute().valueClass(), LocalDate.class);
    assertEquals(definition.property(TestDomain.Detail.TIMESTAMP).attribute().valueClass(), LocalDateTime.class);
    assertEquals(definition.property(TestDomain.Detail.BOOLEAN).attribute().valueClass(), Boolean.class);
    assertEquals(definition.property(TestDomain.Detail.MASTER_FK).attribute().valueClass(), Entity.class);
    assertEquals(definition.property(TestDomain.Detail.MASTER_ID).attribute().valueClass(), Long.class);
    assertEquals(definition.property(TestDomain.Detail.MASTER_NAME).attribute().valueClass(), String.class);
    assertEquals(definition.property(TestDomain.Detail.MASTER_CODE).attribute().valueClass(), Integer.class);

    //assert column names
    assertEquals(definition.property(TestDomain.Detail.ID).attribute(), TestDomain.Detail.ID);
    assertEquals(definition.property(TestDomain.Detail.SHORT).attribute(), TestDomain.Detail.SHORT);
    assertEquals(definition.property(TestDomain.Detail.INT).attribute(), TestDomain.Detail.INT);
    assertEquals(definition.property(TestDomain.Detail.DOUBLE).attribute(), TestDomain.Detail.DOUBLE);
    assertEquals(definition.property(TestDomain.Detail.STRING).attribute(), TestDomain.Detail.STRING);
    assertEquals(definition.property(TestDomain.Detail.DATE).attribute(), TestDomain.Detail.DATE);
    assertEquals(definition.property(TestDomain.Detail.TIMESTAMP).attribute(), TestDomain.Detail.TIMESTAMP);
    assertEquals(definition.property(TestDomain.Detail.BOOLEAN).attribute(), TestDomain.Detail.BOOLEAN);
    assertEquals(definition.property(TestDomain.Detail.MASTER_ID).attribute(), TestDomain.Detail.MASTER_ID);
    assertEquals(definition.property(TestDomain.Detail.MASTER_NAME).attribute(), TestDomain.Detail.MASTER_NAME);
    assertEquals(definition.property(TestDomain.Detail.MASTER_CODE).attribute(), TestDomain.Detail.MASTER_CODE);

    //assert captions
    assertNotNull(definition.property(TestDomain.Detail.ID).caption());
    assertEquals(definition.property(TestDomain.Detail.SHORT).caption(), TestDomain.Detail.SHORT.name());
    assertEquals(definition.property(TestDomain.Detail.INT).caption(), TestDomain.Detail.INT.name());
    assertEquals(definition.property(TestDomain.Detail.DOUBLE).caption(), TestDomain.Detail.DOUBLE.name());
    assertEquals(definition.property(TestDomain.Detail.STRING).caption(), "Detail string");
    assertEquals(definition.property(TestDomain.Detail.DATE).caption(), TestDomain.Detail.DATE.name());
    assertEquals(definition.property(TestDomain.Detail.TIMESTAMP).caption(), TestDomain.Detail.TIMESTAMP.name());
    assertEquals(definition.property(TestDomain.Detail.BOOLEAN).caption(), TestDomain.Detail.BOOLEAN.name());
    assertEquals(definition.property(TestDomain.Detail.MASTER_FK).caption(), TestDomain.Detail.MASTER_FK.name());
    assertEquals(definition.property(TestDomain.Detail.MASTER_NAME).caption(), TestDomain.Detail.MASTER_NAME.name());
    assertEquals(definition.property(TestDomain.Detail.MASTER_CODE).caption(), TestDomain.Detail.MASTER_CODE.name());

    //assert hidden status
    assertTrue(definition.property(TestDomain.Detail.ID).isHidden());
    assertFalse(definition.property(TestDomain.Detail.SHORT).isHidden());
    assertFalse(definition.property(TestDomain.Detail.INT).isHidden());
    assertFalse(definition.property(TestDomain.Detail.DOUBLE).isHidden());
    assertFalse(definition.property(TestDomain.Detail.STRING).isHidden());
    assertFalse(definition.property(TestDomain.Detail.DATE).isHidden());
    assertFalse(definition.property(TestDomain.Detail.TIMESTAMP).isHidden());
    assertFalse(definition.property(TestDomain.Detail.BOOLEAN).isHidden());
    assertFalse(definition.property(TestDomain.Detail.MASTER_FK).isHidden());
    assertFalse(definition.property(TestDomain.Detail.MASTER_NAME).isHidden());
    assertFalse(definition.property(TestDomain.Detail.MASTER_CODE).isHidden());
  }

  @Test
  void propertyWrongEntityType() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> definition.property(TestDomain.Master.CODE));
  }

  @Test
  void writableColumnProperties() {
    EntityDefinition deptDef = entities.definition(TestDomain.Department.TYPE);
    List<ColumnProperty<?>> writable = deptDef
            .writableColumnProperties(true, true);
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.NO)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.NAME)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.LOCATION)));
    assertFalse(writable.contains(deptDef.property(TestDomain.Department.ACTIVE)));

    writable = deptDef.writableColumnProperties(false, true);
    assertFalse(writable.contains(deptDef.property(TestDomain.Department.NO)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.NAME)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.LOCATION)));
    assertFalse(writable.contains(deptDef.property(TestDomain.Department.ACTIVE)));

    writable = deptDef.writableColumnProperties(false, false);
    assertFalse(writable.contains(deptDef.property(TestDomain.Department.NO)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.NAME)));
    assertTrue(writable.contains(deptDef.property(TestDomain.Department.LOCATION)));
    assertFalse(writable.contains(deptDef.property(TestDomain.Department.ACTIVE)));

    EntityDefinition empDef = entities.definition(TestDomain.Employee.TYPE);
    writable = empDef.writableColumnProperties(true, true);
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.ID)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.NAME)));
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.DEPARTMENT_NAME)));

    writable = empDef.writableColumnProperties(false, true);
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.ID)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.NAME)));
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.DEPARTMENT_NAME)));

    writable = empDef.writableColumnProperties(false, false);
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.ID)));
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.NAME)));
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.DEPARTMENT_NAME)));

    writable = empDef.writableColumnProperties(true, false);
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.ID)));//overridden by includeNonUpdatable
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.property(TestDomain.Employee.NAME)));
    assertFalse(writable.contains(empDef.property(TestDomain.Employee.DEPARTMENT_NAME)));
  }

  @Test
  void sortProperties() {
    List<Property<?>> properties = new ArrayList<>(entities.definition(TestDomain.Employee.TYPE).properties(
            asList(TestDomain.Employee.HIREDATE, TestDomain.Employee.COMMISSION,
                    TestDomain.Employee.SALARY, TestDomain.Employee.JOB)));
    properties.sort(Property.propertyComparator());
    assertEquals(TestDomain.Employee.COMMISSION, properties.get(0).attribute());
    assertEquals(TestDomain.Employee.HIREDATE, properties.get(1).attribute());
    assertEquals(TestDomain.Employee.JOB, properties.get(2).attribute());
    assertEquals(TestDomain.Employee.SALARY, properties.get(3).attribute());
  }

  @Test
  void updatableProperties() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<Property<?>> properties = definition.updatableProperties();
    assertEquals(11, properties.size());
    assertFalse(properties.contains(definition.property(TestDomain.Detail.MASTER_NAME)));
    assertFalse(properties.contains(definition.property(TestDomain.Detail.MASTER_CODE)));
    assertFalse(properties.contains(definition.property(TestDomain.Detail.INT_DERIVED)));
  }

  @Test
  void selectedProperties() {
    List<Attribute<?>> attributes = new ArrayList<>();
    attributes.add(TestDomain.Department.NO);
    attributes.add(TestDomain.Department.NAME);

    EntityDefinition definition = entities.definition(TestDomain.Department.TYPE);
    Collection<Property<?>> properties = definition.properties(attributes);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(definition.property(TestDomain.Department.NO)));
    assertTrue(properties.contains(definition.property(TestDomain.Department.NAME)));

    Collection<Property<?>> noProperties = definition.properties(emptyList());
    assertEquals(0, noProperties.size());
  }

  @Test
  void key() {
    Key key = entities.keyBuilder(KeyTest.TYPE).build();
    assertEquals(0, key.hashCode());
    assertTrue(key.attributes().isEmpty());
    assertTrue(key.isNull());

    assertThrows(IllegalStateException.class, () -> entities.primaryKey(KeyTest.TYPE, 1));
    assertThrows(NoSuchElementException.class, key::get);
    assertThrows(NoSuchElementException.class, key::optional);
    assertThrows(NoSuchElementException.class, key::attribute);

    key = key.copyBuilder()
            .with(KeyTest.ID1, 1)
            .with(KeyTest.ID2, 2)
            .with(KeyTest.ID3, 3)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(6, key.hashCode());
    assertTrue(key.optional(KeyTest.ID1).isPresent());

    key = key.copyBuilder()
            .with(KeyTest.ID2, 3)
            .build();
    assertEquals(7, key.hashCode());

    key = key.copyBuilder()
            .with(KeyTest.ID3, null)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(4, key.hashCode());
    key = key.copyBuilder()
            .with(KeyTest.ID2, null)
            .build();
    assertTrue(key.isNull());
    assertFalse(key.optional(KeyTest.ID2).isPresent());
    assertEquals(0, key.hashCode());
    key = key.copyBuilder()
            .with(KeyTest.ID2, 4)
            .build();
    assertTrue(key.optional(KeyTest.ID2).isPresent());
    assertTrue(key.isNotNull());
    assertEquals(5, key.hashCode());

    key = key.copyBuilder()
            .with(KeyTest.ID2, 42)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> entities.keyBuilder(null));

    assertFalse(entities.keyBuilder(TestDomain.NoPk.TYPE)
            .with(TestDomain.NoPk.COL1, 1)
            .build()
            .isPrimaryKey());
    Key noPk = entities.keyBuilder(TestDomain.NoPk.TYPE).build();
    assertThrows(IllegalArgumentException.class, () -> noPk.get(TestDomain.NoPk.COL1));
  }

  @Test
  void keys() {
    List<Key> intKeys = entities.primaryKeys(TestDomain.Employee.TYPE, 1, 2, 3, 4);
    assertEquals(4, intKeys.size());
    assertEquals(Integer.valueOf(3), intKeys.get(2).get());
    List<Key> longKeys = entities.primaryKeys(TestDomain.Detail.TYPE, 1L, 2L, 3L, 4L);
    assertEquals(4, longKeys.size());
    assertEquals(Long.valueOf(3), longKeys.get(2).get());
  }

  @Test
  void entity() {
    Key key = entities.primaryKey(TestDomain.Master.TYPE, 10L);

    Entity master = Entity.entity(key);
    assertEquals(TestDomain.Master.TYPE, master.type());
    assertTrue(master.contains(TestDomain.Master.ID));
    assertEquals(10L, master.get(TestDomain.Master.ID));

    assertThrows(NullPointerException.class, () -> entities.entity(null));
  }

  @Test
  void properties() {
    EntityDefinition definition = entities.definition(TestDomain.Department.TYPE);
    Property<Integer> id = definition.property(TestDomain.Department.NO);
    Property<String> location = definition.property(TestDomain.Department.LOCATION);
    Property<String> name = definition.property(TestDomain.Department.NAME);
    Property<Boolean> active = definition.property(TestDomain.Department.ACTIVE);
    Collection<Property<?>> properties = definition.properties(asList(TestDomain.Department.LOCATION, TestDomain.Department.NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    properties = definition.visibleProperties();
    assertTrue(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));
    assertFalse(properties.contains(active));

    List<Property<?>> allProperties = definition.properties();
    assertTrue(allProperties.contains(id));
    assertTrue(allProperties.contains(location));
    assertTrue(allProperties.contains(name));
    assertTrue(allProperties.contains(active));
  }

  @Test
  void propertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> entities.definition(TestDomain.Master.TYPE)
            .property(TestDomain.Master.TYPE.attribute("unknown property", Integer.class)));
  }

  @Test
  void foreignKeys() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<ForeignKey> foreignKeys = definition.foreignKeys(TestDomain.Employee.TYPE);
    assertEquals(0, foreignKeys.size());
    foreignKeys = definition.foreignKeys(TestDomain.Master.TYPE);
    assertEquals(2, foreignKeys.size());
    assertTrue(foreignKeys.contains(TestDomain.Detail.MASTER_FK));
  }

  @Test
  void foreignKeyProperty() {
    assertNotNull(entities.definition(TestDomain.Detail.TYPE).foreignKeyProperty(TestDomain.Detail.MASTER_FK));
  }

  @Test
  void foreignKeyPropertyInvalid() {
    ForeignKey foreignKey = TestDomain.Detail.TYPE.foreignKey("bla bla", TestDomain.Detail.MASTER_ID, TestDomain.Master.ID);
    assertThrows(IllegalArgumentException.class, () -> entities.definition(TestDomain.Detail.TYPE).foreignKeyProperty(foreignKey));
  }

  @Test
  void hasDerivedAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    assertFalse(definition.hasDerivedAttributes(TestDomain.Detail.BOOLEAN));
    assertTrue(definition.hasDerivedAttributes(TestDomain.Detail.INT));
  }

  @Test
  void derivedAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Detail.TYPE);
    Collection<Attribute<?>> derivedAttributes = definition.derivedAttributes(TestDomain.Detail.BOOLEAN);
    assertTrue(derivedAttributes.isEmpty());
    derivedAttributes = definition.derivedAttributes(TestDomain.Detail.INT);
    assertEquals(1, derivedAttributes.size());
    assertTrue(derivedAttributes.contains(TestDomain.Detail.INT_DERIVED));
  }

  @Test
  void isSmallDataset() {
    assertTrue(entities.definition(TestDomain.Detail.TYPE).isSmallDataset());
  }

  @Test
  void stringFactory() {
    assertNotNull(entities.definition(TestDomain.Department.TYPE).stringFactory());
  }

  @Test
  void nullValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200.0)
            .build();

    DefaultEntityValidator validator = new DefaultEntityValidator();
    try {
      validator.validate(emp);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.Employee.DEPARTMENT_FK, e.attribute());
    }
    emp.put(TestDomain.Employee.DEPARTMENT_NO, 1);
    try {
      validator.validate(emp);
    }
    catch (ValidationException e) {
      fail();
    }
    emp.put(TestDomain.Employee.SALARY, null);
    try {
      validator.validate(emp);
      fail();
    }
    catch (ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.Employee.SALARY, e.attribute());
    }
  }

  @Test
  void maxLengthValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_NO, 1)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200.0)
            .build();
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(emp));
    emp.put(TestDomain.Employee.NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void rangeValidation() {
    Entity emp = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_NO, 1)
            .with(TestDomain.Employee.NAME, "Name")
            .with(TestDomain.Employee.HIREDATE, LocalDateTime.now())
            .with(TestDomain.Employee.SALARY, 1200d)
            .with(TestDomain.Employee.COMMISSION, 300d)
            .build();
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(emp));
    emp.put(TestDomain.Employee.COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
    emp.put(TestDomain.Employee.COMMISSION, 2100d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void itemValidation() {
    Map<Attribute<?>, Object> values = new HashMap<>();
    values.put(TestDomain.Employee.NAME, "Name");
    values.put(TestDomain.Employee.DEPARTMENT_NO, 1);
    values.put(TestDomain.Employee.JOB, "CLREK");
    Entity emp = entities.definition(TestDomain.Employee.TYPE).entity(values);
    DefaultEntityValidator validator = new DefaultEntityValidator();
    assertThrows(ItemValidationException.class, () -> validator.validate(emp));
  }

  @Test
  void searchAttributes() {
    EntityDefinition definition = entities.definition(TestDomain.Employee.TYPE);
    Collection<Attribute<String>> searchAttributes = definition.searchAttributes();
    assertTrue(searchAttributes.contains(TestDomain.Employee.JOB));
    assertTrue(searchAttributes.contains(TestDomain.Employee.NAME));

    searchAttributes = entities.definition(TestDomain.Department.TYPE).searchAttributes();
    //should contain all string based properties
    assertTrue(searchAttributes.contains(TestDomain.Department.NAME));
  }

  @Test
  void validateTypeEntity() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    Entity entity1 = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.MASTER_FK, entity1));
  }

  @Test
  void setValueDerived() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.INT_DERIVED, 10));
  }

  @Test
  void setValueItem() {
    Entity entity = entities.entity(TestDomain.Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.Detail.INT_VALUE_LIST, -10));
  }

  @Test
  void copyEntities() {
    Entity dept1 = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, 1)
            .with(TestDomain.Department.LOCATION, "location")
            .with(TestDomain.Department.NAME, "name")
            .build();
    Entity dept2 = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, 2)
            .with(TestDomain.Department.LOCATION, "location2")
            .with(TestDomain.Department.NAME, "name2")
            .build();

    Iterator<Entity> copies = Entity.deepCopy(asList(dept1, dept2)).iterator();
    Entity dept1Copy = copies.next();
    Entity dept2Copy = copies.next();
    assertNotSame(dept1Copy, dept1);
    assertTrue(dept1Copy.columnValuesEqual(dept1));
    assertNotSame(dept2Copy, dept2);
    assertTrue(dept2Copy.columnValuesEqual(dept2));

    Entity emp1 = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.DEPARTMENT_FK, dept1)
            .with(TestDomain.Employee.NAME, "name")
            .with(TestDomain.Employee.COMMISSION, 130.5)
            .build();

    Entity copy = emp1.copy();
    assertTrue(emp1.columnValuesEqual(copy));
    assertSame(emp1.get(TestDomain.Employee.DEPARTMENT_FK), copy.get(TestDomain.Employee.DEPARTMENT_FK));
    assertFalse(emp1.isModified());

    copy = emp1.deepCopy();
    assertTrue(emp1.columnValuesEqual(copy));
    assertNotSame(emp1.get(TestDomain.Employee.DEPARTMENT_FK), copy.get(TestDomain.Employee.DEPARTMENT_FK));
    assertFalse(emp1.isModified());
  }

  @Test
  void toBeans() {
    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    Entity department = entities.builder(TestDomain.Department.TYPE)
            .with(TestDomain.Department.NO, deptNo)
            .with(TestDomain.Department.NAME, deptName)
            .with(TestDomain.Department.LOCATION, deptLocation)
            .with(TestDomain.Department.ACTIVE, deptActive)
            .build();

    Collection<TestDomain.Department> deptBeans = Entity.castTo(TestDomain.Department.class, singletonList(department));
    TestDomain.Department departmentBean = deptBeans.iterator().next();
    assertEquals(deptNo, departmentBean.deptNo());
    assertEquals(deptName, departmentBean.name());
    assertEquals(deptLocation, departmentBean.location());
    assertEquals(deptActive, departmentBean.active());

    departmentBean.active(false);
    departmentBean.setDeptNo(42);

    assertFalse(department.get(TestDomain.Department.ACTIVE));
    assertEquals(42, department.get(TestDomain.Department.NO));

    department.put(TestDomain.Department.NO, null);
    assertEquals(0d, departmentBean.deptNo());

    departmentBean.setDeptNo(deptNo);

    Entity manager = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.ID, 12)
            .build();

    final Integer id = 42;
    final Double commission = 42.2;
    LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    Entity employee = entities.builder(TestDomain.Employee.TYPE)
            .with(TestDomain.Employee.ID, id)
            .with(TestDomain.Employee.COMMISSION, commission)
            .with(TestDomain.Employee.DEPARTMENT_FK, department)
            .with(TestDomain.Employee.HIREDATE, hiredate)
            .with(TestDomain.Employee.JOB, job)
            .with(TestDomain.Employee.MANAGER_FK, manager)
            .with(TestDomain.Employee.NAME, name)
            .with(TestDomain.Employee.SALARY, salary)
            .build();

    Collection<TestDomain.Employee> empBeans = Entity.castTo(TestDomain.Employee.class, singletonList(employee));
    TestDomain.Employee employeeBean = empBeans.iterator().next();
    assertEquals(id, employeeBean.getId());
    assertEquals(commission, employeeBean.getCommission());
    assertEquals(deptNo, employeeBean.getDeptno());
    assertEquals(deptNo, employeeBean.getDepartment().deptNo());
    assertEquals(hiredate, employeeBean.getHiredate());
    assertEquals(job, employeeBean.getJob());
    assertEquals(mgr, employeeBean.getMgr());
    assertEquals(12, employeeBean.getManager().getId());
    assertEquals(name, employeeBean.getName());
    assertEquals(salary, employeeBean.getSalary());

    assertTrue(Entity.castTo(TestDomain.Employee.class, emptyList()).isEmpty());
  }

  @Test
  void toEntityType() {
    Entity master = entities.builder(TestDomain.Master.TYPE)
            .with(TestDomain.Master.ID, 1L)
            .with(TestDomain.Master.CODE, 1)
            .with(TestDomain.Master.NAME, "name")
            .build();

    assertThrows(IllegalArgumentException.class, () -> master.castTo(TestDomain.Detail.class));

    TestDomain.Master master1 = master.castTo(TestDomain.Master.class);

    assertSame(master1, master1.castTo(TestDomain.Master.class));

    Entity master2 = entities.builder(TestDomain.Master.TYPE)
            .with(TestDomain.Master.ID, 2L)
            .with(TestDomain.Master.CODE, 2)
            .with(TestDomain.Master.NAME, "name2")
            .build();

    List<Entity> masters = asList(master, master1, master2);

    List<TestDomain.Master> mastersTyped = new ArrayList<>(Entity.castTo(TestDomain.Master.class, masters));

    assertSame(master1, mastersTyped.get(1));

    assertEquals(1L, mastersTyped.get(0).get(TestDomain.Master.ID));
    assertEquals(1L, mastersTyped.get(1).get(TestDomain.Master.ID));
    assertEquals(2L, mastersTyped.get(2).get(TestDomain.Master.ID));

    assertEquals(1L, mastersTyped.get(0).getId());
    assertEquals("name", mastersTyped.get(0).getName());

    Entity detail = entities.builder(TestDomain.Detail.TYPE)
            .with(TestDomain.Detail.ID, 1L)
            .with(TestDomain.Detail.DOUBLE, 1.2)
            .with(TestDomain.Detail.MASTER_FK, master)
            .build();

    TestDomain.Detail detailTyped = detail.castTo(TestDomain.Detail.class);
    assertEquals(detailTyped.getId().orElse(null), 1L);
    assertEquals(detailTyped.getDouble().orElse(null), 1.2);
    assertEquals(detailTyped.getMaster().orElse(null), master);
    assertEquals(detailTyped.master(), master);

    detailTyped.setId(2L);
    detailTyped.setDouble(2.1);
    detailTyped.setMaster(master1);

    assertEquals(detailTyped.getId().orElse(null), detail.get(TestDomain.Detail.ID));
    assertEquals(detailTyped.getDouble().orElse(null), detail.get(TestDomain.Detail.DOUBLE));
    assertSame(detailTyped.getMaster().orElse(null), detail.get(TestDomain.Detail.MASTER_FK));

    detailTyped.setAll(3L, 3.2, mastersTyped.get(2));

    assertEquals(detailTyped.getId().orElse(null), 3L);
    assertEquals(detailTyped.getDouble().orElse(null), 3.2);
    assertSame(detailTyped.getMaster().orElse(null), mastersTyped.get(2));

    Entity compositeMaster = entities.builder(TestDomain.CompositeMaster.TYPE)
            .with(TestDomain.CompositeMaster.COMPOSITE_MASTER_ID, 1)
            .with(TestDomain.CompositeMaster.COMPOSITE_MASTER_ID_2, 2)
            .build();

    assertSame(compositeMaster, compositeMaster.castTo(Entity.class));
  }

  @Test
  void serialize() throws IOException, ClassNotFoundException {
    List<Entity> entitiesToSer = IntStream.range(0, 10)
            .mapToObj(i -> entities.builder(TestDomain.Master.TYPE)
                    .with(TestDomain.Master.ID, (long) i)
                    .with(TestDomain.Master.NAME, Integer.toString(i))
                    .with(TestDomain.Master.CODE, 1)
                    .build())
            .collect(Collectors.toList());

    Serializer.deserialize(Serializer.serialize(Entity.castTo(TestDomain.Master.class, entitiesToSer)));
  }

  @Test
  void extendedDomain() {
    TestDomainExtended extended = new TestDomainExtended();
    Entities entities = extended.entities();

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.CompositeMaster.TYPE);

    TestDomainExtended.TestDomainSecondExtension second = new TestDomainExtended.TestDomainSecondExtension();
    entities = second.entities();

    entities.entity(TestDomainExtended.TestDomainSecondExtension.T_SECOND_EXTENDED);

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.CompositeMaster.TYPE);

    assertNotNull(second.procedure(TestDomainExtended.PROC_TYPE));
    assertNotNull(second.function(TestDomainExtended.FUNC_TYPE));
    assertNotNull(second.report(TestDomainExtended.REP_TYPE));

    //entity type name clash
    assertThrows(IllegalArgumentException.class, TestDomainExtended.TestDomainThirdExtension::new);
  }
}
