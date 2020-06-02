/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.DateFormats;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.AbstractDatabaseProcedure;
import is.codion.common.db.operation.DatabaseOperation;
import is.codion.common.event.EventListener;
import is.codion.framework.domain.attribute.Attribute;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Department;
import is.codion.framework.domain.entity.Employee;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.DerivedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.entity.Entities.type;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.getEntities();

  @Test
  public void defineTypes() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);

    //assert types
    assertEquals(definition.getProperty(TestDomain.DETAIL_ID).getType(), Types.BIGINT);
    assertEquals(definition.getProperty(TestDomain.DETAIL_INT).getType(), Types.INTEGER);
    assertEquals(definition.getProperty(TestDomain.DETAIL_DOUBLE).getType(), Types.DOUBLE);
    assertEquals(definition.getProperty(TestDomain.DETAIL_STRING).getType(), Types.VARCHAR);
    assertEquals(definition.getProperty(TestDomain.DETAIL_DATE).getType(), Types.DATE);
    assertEquals(definition.getProperty(TestDomain.DETAIL_TIMESTAMP).getType(), Types.TIMESTAMP);
    assertEquals(definition.getProperty(TestDomain.DETAIL_BOOLEAN).getType(), Types.BOOLEAN);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_FK).getType(), Types.JAVA_OBJECT);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_ID).getType(), Types.BIGINT);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_NAME).getType(), Types.VARCHAR);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_CODE).getType(), Types.INTEGER);

    //assert column names
    assertEquals(definition.getProperty(TestDomain.DETAIL_ID).getAttribute(), TestDomain.DETAIL_ID);
    assertEquals(definition.getProperty(TestDomain.DETAIL_INT).getAttribute(), TestDomain.DETAIL_INT);
    assertEquals(definition.getProperty(TestDomain.DETAIL_DOUBLE).getAttribute(), TestDomain.DETAIL_DOUBLE);
    assertEquals(definition.getProperty(TestDomain.DETAIL_STRING).getAttribute(), TestDomain.DETAIL_STRING);
    assertEquals(definition.getProperty(TestDomain.DETAIL_DATE).getAttribute(), TestDomain.DETAIL_DATE);
    assertEquals(definition.getProperty(TestDomain.DETAIL_TIMESTAMP).getAttribute(), TestDomain.DETAIL_TIMESTAMP);
    assertEquals(definition.getProperty(TestDomain.DETAIL_BOOLEAN).getAttribute(), TestDomain.DETAIL_BOOLEAN);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_ID).getAttribute(), TestDomain.DETAIL_MASTER_ID);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_NAME).getAttribute(), TestDomain.DETAIL_MASTER_NAME);
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_CODE).getAttribute(), TestDomain.DETAIL_MASTER_CODE);

    //assert captions
    assertNotNull(definition.getProperty(TestDomain.DETAIL_ID).getCaption());
    assertEquals(definition.getProperty(TestDomain.DETAIL_INT).getCaption(), TestDomain.DETAIL_INT.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_DOUBLE).getCaption(), TestDomain.DETAIL_DOUBLE.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_STRING).getCaption(), "Detail string");
    assertEquals(definition.getProperty(TestDomain.DETAIL_DATE).getCaption(), TestDomain.DETAIL_DATE.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_TIMESTAMP).getCaption(), TestDomain.DETAIL_TIMESTAMP.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_BOOLEAN).getCaption(), TestDomain.DETAIL_BOOLEAN.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_FK).getCaption(), TestDomain.DETAIL_MASTER_FK.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_NAME).getCaption(), TestDomain.DETAIL_MASTER_NAME.getName());
    assertEquals(definition.getProperty(TestDomain.DETAIL_MASTER_CODE).getCaption(), TestDomain.DETAIL_MASTER_CODE.getName());

    //assert hidden status
    assertTrue(definition.getProperty(TestDomain.DETAIL_ID).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_INT).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_DOUBLE).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_STRING).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_DATE).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_TIMESTAMP).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_BOOLEAN).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_MASTER_FK).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_MASTER_NAME).isHidden());
    assertFalse(definition.getProperty(TestDomain.DETAIL_MASTER_CODE).isHidden());
  }

  @Test
  public void getPropertyWrongEntityType() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> definition.getProperty(TestDomain.MASTER_CODE));
  }

  @Test
  public void getWritableColumnProperties() {
    final EntityDefinition deptDef = domain.getDefinition(TestDomain.T_DEPARTMENT);
    List<ColumnProperty<?>> writable = deptDef
            .getWritableColumnProperties(true, true);
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ID)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_NAME)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ACTIVE)));

    writable = deptDef.getWritableColumnProperties(false, true);
    assertFalse(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ID)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_NAME)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ACTIVE)));

    writable = deptDef.getWritableColumnProperties(false, false);
    assertFalse(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ID)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_NAME)));
    assertTrue(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(TestDomain.DEPARTMENT_ACTIVE)));

    final EntityDefinition empDef = domain.getDefinition(TestDomain.T_EMP);
    writable = empDef.getWritableColumnProperties(true, true);
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_ID)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_NAME)));
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_NAME_DEPARTMENT)));

    writable = empDef.getWritableColumnProperties(false, true);
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_ID)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_NAME)));
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_NAME_DEPARTMENT)));

    writable = empDef.getWritableColumnProperties(false, false);
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_ID)));
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_NAME)));
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_NAME_DEPARTMENT)));

    writable = empDef.getWritableColumnProperties(true, false);
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_ID)));//overridden by includeNonUpdatable
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(TestDomain.EMP_NAME)));
    assertFalse(writable.contains(empDef.getProperty(TestDomain.EMP_NAME_DEPARTMENT)));
  }

  @Test
  public void sortProperties() {
    final List<Property<?>> properties = Properties.sort(domain.getDefinition(TestDomain.T_EMP).getProperties(
            asList(TestDomain.EMP_HIREDATE, TestDomain.EMP_COMMISSION,
                    TestDomain.EMP_SALARY, TestDomain.EMP_JOB)));
    assertEquals(TestDomain.EMP_COMMISSION, properties.get(0).getAttribute());
    assertEquals(TestDomain.EMP_HIREDATE, properties.get(1).getAttribute());
    assertEquals(TestDomain.EMP_JOB, properties.get(2).getAttribute());
    assertEquals(TestDomain.EMP_SALARY, properties.get(3).getAttribute());
  }

  @Test
  public void getUpdatableProperties() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    final List<Property<?>> properties = definition.getUpdatableProperties();
    assertEquals(9, properties.size());
    assertFalse(properties.contains(definition.getProperty(TestDomain.DETAIL_MASTER_NAME)));
    assertFalse(properties.contains(definition.getProperty(TestDomain.DETAIL_MASTER_CODE)));
    assertFalse(properties.contains(definition.getProperty(TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void getSelectedProperties() {
    final List<Attribute<?>> attributes = new ArrayList<>();
    attributes.add(TestDomain.DEPARTMENT_ID);
    attributes.add(TestDomain.DEPARTMENT_NAME);

    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DEPARTMENT);
    final List<Property<?>> properties = definition.getProperties(attributes);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(definition.getProperty(TestDomain.DEPARTMENT_ID)));
    assertTrue(properties.contains(definition.getProperty(TestDomain.DEPARTMENT_NAME)));

    final List<Property<?>> noProperties = definition.getProperties(emptyList());
    assertEquals(0, noProperties.size());
  }

  @Test
  public void key() {
    final EntityType entityType = type("DomainTest.key");
    final Attribute<Integer> attribute1 = entityType.attribute("id1", Integer.class);
    final Attribute<Integer> attribute2 = entityType.attribute("id2", Integer.class);
    final Attribute<Integer> attribute3 = entityType.attribute("id3", Integer.class);
    domain.define(entityType,
            Properties.primaryKeyProperty(attribute1),
            Properties.primaryKeyProperty(attribute2).primaryKeyIndex(1),
            Properties.primaryKeyProperty(attribute3).primaryKeyIndex(2).nullable(true));

    final Entity.Key key = entities.key(entityType);
    assertEquals(0, key.hashCode());
    assertTrue(key.isCompositeKey());
    assertTrue(key.isNull());

    key.put(attribute1, 1);
    key.put(attribute2, 2);
    key.put(attribute3, 3);
    assertTrue(key.isNotNull());
    assertEquals(6, key.hashCode());

    key.put(attribute2, 3);
    assertEquals(7, key.hashCode());

    key.put(attribute3, null);
    assertTrue(key.isNotNull());
    assertEquals(4, key.hashCode());
    key.put(attribute2, null);
    assertTrue(key.isNull());
    assertEquals(0, key.hashCode());
    key.put(attribute2, 4);
    assertTrue(key.isNotNull());
    assertEquals(5, key.hashCode());

    key.put(attribute2, 42);
    assertTrue(key.isNotNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> entities.key(null));

    final Entity.Key noPk = entities.key(TestDomain.T_NO_PK);
    assertThrows(IllegalArgumentException.class, () -> noPk.put(TestDomain.NO_PK_COL1, 1));
    assertThrows(IllegalArgumentException.class, () -> noPk.get(TestDomain.NO_PK_COL1));
  }

   @Test
   public void keys() {
    final List<Entity.Key> intKeys = entities.keys(TestDomain.T_EMP, 1, 2, 3, 4);
    assertEquals(4, intKeys.size());
    assertEquals(3, intKeys.get(2).getFirstValue());
    final List<Entity.Key> longKeys = entities.keys(TestDomain.T_DETAIL, 1L, 2L, 3L, 4L);
    assertEquals(4, longKeys.size());
    assertEquals(3L, longKeys.get(2).getFirstValue());
   }

  @Test
  public void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = type("keyWithSameIndex");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)).primaryKeyIndex(0),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)).primaryKeyIndex(1),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)).primaryKeyIndex(1));
    });
  }

  @Test
  public void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = type("keyWithSameIndex2");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)));
    });
  }

  @Test
  public void entity() {
    final Entity.Key key = entities.key(TestDomain.T_MASTER, 10L);

    final Entity master = entities.entity(key);
    assertEquals(TestDomain.T_MASTER, master.getEntityType());
    assertTrue(master.containsKey(TestDomain.MASTER_ID));
    assertEquals(10L, master.get(TestDomain.MASTER_ID));

    assertThrows(NullPointerException.class, () -> entities.entity((EntityType) null));
  }

  @Test
  public void getProperties() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DEPARTMENT);
    final Property<Integer> id = definition.getProperty(TestDomain.DEPARTMENT_ID);
    final Property<String> location = definition.getProperty(TestDomain.DEPARTMENT_LOCATION);
    final Property<String> name = definition.getProperty(TestDomain.DEPARTMENT_NAME);
    final Property<Boolean> active = definition.getProperty(TestDomain.DEPARTMENT_ACTIVE);
    List<Property<?>> properties = definition.getProperties(asList(TestDomain.DEPARTMENT_LOCATION, TestDomain.DEPARTMENT_NAME));
    assertEquals(2, properties.size());
    assertFalse(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));

    properties = definition.getVisibleProperties();
    assertTrue(properties.contains(id));
    assertTrue(properties.contains(location));
    assertTrue(properties.contains(name));
    assertFalse(properties.contains(active));

    final List<Property<?>> allProperties = definition.getProperties();
    assertTrue(allProperties.contains(id));
    assertTrue(allProperties.contains(location));
    assertTrue(allProperties.contains(name));
    assertTrue(allProperties.contains(active));
  }

  @Test
  public void getPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getDefinition(TestDomain.T_MASTER)
            .getProperty(TestDomain.T_MASTER.attribute("unknown property", Integer.class)));
  }

  @Test
  public void getForeignKeyReferences() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    List<ForeignKeyProperty> foreignKeyProperties = definition.getForeignKeyReferences(TestDomain.T_EMP);
    assertEquals(0, foreignKeyProperties.size());
    foreignKeyProperties = definition.getForeignKeyReferences(TestDomain.T_MASTER);
    assertEquals(1, foreignKeyProperties.size());
    assertTrue(foreignKeyProperties.contains(definition.getProperty(TestDomain.DETAIL_MASTER_FK)));
  }

  @Test
  public void getForeignKeyProperty() {
    assertNotNull(domain.getDefinition(TestDomain.T_DETAIL).getForeignKeyProperty(TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getForeignKeyPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> domain.getDefinition(TestDomain.T_DETAIL).getForeignKeyProperty(TestDomain.T_DETAIL.entityAttribute("bla bla")));
  }

  @Test
  public void hasDerivedProperties() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    assertFalse(definition.hasDerivedProperties(TestDomain.DETAIL_BOOLEAN));
    assertTrue(definition.hasDerivedProperties(TestDomain.DETAIL_INT));
  }

  @Test
  public void getDerivedProperties() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_DETAIL);
    Collection<DerivedProperty<?>> derivedProperties = definition.getDerivedProperties(TestDomain.DETAIL_BOOLEAN);
    assertTrue(derivedProperties.isEmpty());
    derivedProperties = definition.getDerivedProperties(TestDomain.DETAIL_INT);
    assertEquals(1, derivedProperties.size());
    assertTrue(derivedProperties.contains(definition.getProperty(TestDomain.DETAIL_INT_DERIVED)));
  }

  @Test
  public void hasDenormalizedProperties() {
    assertTrue(domain.getDefinition(TestDomain.T_DETAIL).hasDenormalizedProperties());
    assertTrue(domain.getDefinition(TestDomain.T_DETAIL).hasDenormalizedProperties(TestDomain.DETAIL_MASTER_FK));
  }

  @Test
  public void getDenormalizedProperties() {
    final List<DenormalizedProperty<?>> denormalized =
            domain.getDefinition(TestDomain.T_DETAIL).getDenormalizedProperties(TestDomain.DETAIL_MASTER_FK);
    assertFalse(denormalized.isEmpty());
    assertEquals(TestDomain.DETAIL_MASTER_CODE_DENORM, denormalized.get(0).getAttribute());
  }

  @Test
  public void isSmallDataset() {
    assertTrue(domain.getDefinition(TestDomain.T_DETAIL).isSmallDataset());
  }

  @Test
  public void getStringProvider() {
    assertNotNull(domain.getDefinition(TestDomain.T_DEPARTMENT).getStringProvider());
  }

  @Test
  public void redefine() {
    final EntityType entityType = type("entityType");
    final Attribute<Integer> attribute = entityType.attribute("attribute", Integer.class);
    domain.define(entityType, Properties.primaryKeyProperty(attribute));
    assertThrows(IllegalArgumentException.class, () -> domain.define(entityType, Properties.primaryKeyProperty(attribute)));
  }

  @Test
  public void nullValidation() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_EMP);
    final Entity emp = entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200.0);

    final DefaultEntityValidator validator = new DefaultEntityValidator();
    try {
      validator.validate(emp, definition);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_DEPARTMENT_FK, e.getAttribute());
    }
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    try {
      validator.validate(emp, definition);
    }
    catch (final ValidationException e) {
      fail();
    }
    emp.put(TestDomain.EMP_SALARY, null);
    try {
      validator.validate(emp, definition);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(TestDomain.EMP_SALARY, e.getAttribute());
    }
  }

  @Test
  public void maxLengthValidation() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_EMP);
    final Entity emp = entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200.0);
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(TestDomain.EMP_NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp, definition));
  }

  @Test
  public void rangeValidation() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_EMP);
    final Entity emp = entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_DEPARTMENT, 1);
    emp.put(TestDomain.EMP_NAME, "Name");
    emp.put(TestDomain.EMP_HIREDATE, LocalDateTime.now());
    emp.put(TestDomain.EMP_SALARY, 1200d);
    emp.put(TestDomain.EMP_COMMISSION, 300d);
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(TestDomain.EMP_COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp, definition));
    emp.put(TestDomain.EMP_COMMISSION, 2100d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp, definition));
  }

  @Test
  public void revalidate() {
    final AtomicInteger counter = new AtomicInteger();
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    final EventListener listener = counter::incrementAndGet;
    validator.addRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
    validator.removeRevalidationListener(listener);
    validator.revalidate();
    assertEquals(1, counter.get());
  }

  @Test
  public void getSearchProperties() {
    final EntityDefinition definition = domain.getDefinition(TestDomain.T_EMP);
    Collection<ColumnProperty<?>> searchProperties = definition.getSearchProperties();
    assertTrue(searchProperties.contains(definition.getColumnProperty(TestDomain.EMP_JOB)));
    assertTrue(searchProperties.contains(definition.getColumnProperty(TestDomain.EMP_NAME)));

    searchProperties = domain.getDefinition(TestDomain.T_DEPARTMENT).getSearchProperties();
    //should contain all string based properties
    assertTrue(searchProperties.contains(domain.getDefinition(TestDomain.T_DEPARTMENT)
            .getColumnProperty(TestDomain.DEPARTMENT_NAME)));
  }

  @Test
  public void selectableProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            domain.getDefinition(TestDomain.T_DETAIL).getSelectableColumnProperties(singletonList(TestDomain.DETAIL_STRING)));
  }

  @Test
  public void stringProvider() {
    final Entity department = entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, -10);
    department.put(TestDomain.DEPARTMENT_LOCATION, "Reykjavik");
    department.put(TestDomain.DEPARTMENT_NAME, "Sales");

    final Entity employee = entities.entity(TestDomain.T_EMP);
    final LocalDateTime hiredate = LocalDateTime.now();
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_NAME, "Darri");
    employee.put(TestDomain.EMP_HIREDATE, hiredate);

    final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern(DateFormats.SHORT_TIMESTAMP);

    StringProvider employeeToString = new StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK, TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals("Darri (department: Sales, location: Reykjavik, hiredate: " + dateFormat.format(hiredate) + ")", employeeToString.apply(employee));

    department.put(TestDomain.DEPARTMENT_LOCATION, null);
    department.put(TestDomain.DEPARTMENT_NAME, null);

    employee.put(TestDomain.EMP_DEPARTMENT_FK, null);
    employee.put(TestDomain.EMP_NAME, null);
    employee.put(TestDomain.EMP_HIREDATE, null);

    employeeToString = new StringProvider(TestDomain.EMP_NAME)
            .addText(" (department: ").addValue(TestDomain.EMP_DEPARTMENT_FK).addText(", location: ")
            .addForeignKeyValue(TestDomain.EMP_DEPARTMENT_FK, TestDomain.DEPARTMENT_LOCATION).addText(", hiredate: ")
            .addFormattedValue(TestDomain.EMP_HIREDATE, dateFormat.toFormat()).addText(")");

    assertEquals(" (department: , location: , hiredate: )", employeeToString.apply(employee));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = type("test.entity");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
              Properties.foreignKeyProperty(entityType.entityAttribute("fk_id_fk"), "caption", type("test.referenced_entity"),
                      Properties.columnProperty(entityType.attribute("fk_id", Integer.class))));
    });
  }

  @Test
  public void foreignKeyReferencingUndefinedEntityNonStrict() {
    domain.setStrictForeignKeys(false);
    final EntityType entityType = type("test.entity");
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
            Properties.foreignKeyProperty(entityType.entityAttribute("fk_id_fk"), "caption", type("test.referenced_entity"),
                    Properties.columnProperty(entityType.attribute("fk_id", Integer.class))));
    domain.setStrictForeignKeys(true);
  }

  @Test
  public void hasSingleIntegerPrimaryKey() {
    EntityType entityType = type("hasSingleIntegerPrimaryKey");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", Integer.class))
                    .primaryKeyIndex(0));
    assertTrue(domain.getDefinition(entityType).hasSingleIntegerPrimaryKey());
    entityType = type("hasSingleIntegerPrimaryKey2");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", Integer.class))
                    .primaryKeyIndex(0),
            Properties.columnProperty(entityType.attribute("test2", Integer.class))
                    .primaryKeyIndex(1));
    assertFalse(domain.getDefinition(entityType).hasSingleIntegerPrimaryKey());
    entityType = type("hasSingleIntegerPrimaryKey3");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", String.class))
                    .primaryKeyIndex(0));
    assertFalse(domain.getDefinition(entityType).hasSingleIntegerPrimaryKey());
  }

  @Test
  public void havingClause() {
    final String havingClause = "p1 > 1";
    final EntityType entityType = type("entityType3");
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("p0", Integer.class))).havingClause(havingClause);
    assertEquals(havingClause, domain.getDefinition(entityType).getHavingClause());
  }

  @Test
  public void validateTypeEntity() {
    final Entity entity = entities.entity(TestDomain.T_DETAIL);
    final Entity entity1 = entities.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_MASTER_FK, entity1));
  }

  @Test
  public void setValueDerived() {
    final Entity entity = entities.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_DERIVED, 10));
  }

  @Test
  public void setValueValueList() {
    final Entity entity = entities.entity(TestDomain.T_DETAIL);
    assertThrows(IllegalArgumentException.class, () -> entity.put(TestDomain.DETAIL_INT_VALUE_LIST, -10));
  }

  @Test
  public void addOperationExisting() {
    final DatabaseOperation operation = new AbstractDatabaseProcedure<DatabaseConnection>("operationId", "test") {
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
  public void conditionProvider() {
    final EntityType nullConditionProvider1 = type("nullConditionProvider1");
    assertThrows(IllegalArgumentException.class, () -> domain.define(nullConditionProvider1,
            Properties.primaryKeyProperty(nullConditionProvider1.integerAttribute("id"))).conditionProvider(null, (attributes, values) -> null));
    final EntityType nullConditionProvider2 = type("nullConditionProvider2");
    assertThrows(NullPointerException.class, () -> domain.define(nullConditionProvider2,
            Properties.primaryKeyProperty(nullConditionProvider2.integerAttribute("id"))).conditionProvider("id", null));
    final EntityType nullConditionProvider3 = type("nullConditionProvider3");
    assertThrows(IllegalStateException.class, () -> domain.define(nullConditionProvider3,
            Properties.primaryKeyProperty(nullConditionProvider3.integerAttribute("id")))
            .conditionProvider("id", (attributes, values) -> null)
            .conditionProvider("id", (attributes, values) -> null));
  }

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {

    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Entity department = entities.entity(TestDomain.T_DEPARTMENT);
    department.put(TestDomain.DEPARTMENT_ID, deptNo);
    department.put(TestDomain.DEPARTMENT_NAME, deptName);
    department.put(TestDomain.DEPARTMENT_LOCATION, deptLocation);
    department.put(TestDomain.DEPARTMENT_ACTIVE, deptActive);

    final List<Department> deptBeans = entities.toBeans(singletonList(department));
    final Department departmentBean = deptBeans.get(0);
    assertEquals(deptNo, departmentBean.getDeptNo());
    assertEquals(deptName, departmentBean.getName());
    assertEquals(deptLocation, departmentBean.getLocation());
    assertEquals(deptActive, departmentBean.getActive());

    final Entity manager = entities.entity(TestDomain.T_EMP);
    manager.put(TestDomain.EMP_ID, 12);

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = entities.entity(TestDomain.T_EMP);
    employee.put(TestDomain.EMP_ID, id);
    employee.put(TestDomain.EMP_COMMISSION, commission);
    employee.put(TestDomain.EMP_DEPARTMENT_FK, department);
    employee.put(TestDomain.EMP_HIREDATE, hiredate);
    employee.put(TestDomain.EMP_JOB, job);
    employee.put(TestDomain.EMP_MGR_FK, manager);
    employee.put(TestDomain.EMP_NAME, name);
    employee.put(TestDomain.EMP_SALARY, salary);

    final List<Employee> empBeans = entities.toBeans(singletonList(employee));
    final Employee employeeBean = empBeans.get(0);
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

    final List<Object> empty = entities.toBeans(null);
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

    final List<Entity> departments = entities.fromBeans(singletonList(departmentBean));
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

    final List<Entity> employees = entities.fromBeans(singletonList(employeeBean));
    final Entity employee = employees.get(0);
    assertEquals(id, employee.get(TestDomain.EMP_ID));
    assertEquals(commission, employee.get(TestDomain.EMP_COMMISSION));
    assertEquals(deptNo, employee.get(TestDomain.EMP_DEPARTMENT));
    assertEquals(deptNo, employee.getForeignKey(TestDomain.EMP_DEPARTMENT_FK)
            .get(TestDomain.DEPARTMENT_ID));
    assertEquals(hiredate, employee.get(TestDomain.EMP_HIREDATE));
    assertEquals(job, employee.get(TestDomain.EMP_JOB));
    assertEquals(mgr, employee.get(TestDomain.EMP_MGR));
    assertEquals(12, employee.getForeignKey(TestDomain.EMP_MGR_FK).get(TestDomain.EMP_ID));
    assertEquals(name, employee.get(TestDomain.EMP_NAME));
    assertEquals(salary, employee.get(TestDomain.EMP_SALARY));

    final List<Entity> empty = entities.fromBeans(null);
    assertTrue(empty.isEmpty());
  }

  @Test
  public void testNullEntity() throws NoSuchMethodException, IllegalAccessException, InstantiationException,
          InvocationTargetException {
    assertThrows(NullPointerException.class, () -> entities.toBean(null));
  }

  @Test
  public void testNullBean() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    assertThrows(NullPointerException.class, () -> entities.fromBean(null));
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

    final List<Entity> copies = entities.deepCopyEntities(asList(dept1, dept2));
    assertNotSame(copies.get(0), dept1);
    assertTrue(copies.get(0).valuesEqual(dept1));
    assertNotSame(copies.get(1), dept2);
    assertTrue(copies.get(1).valuesEqual(dept2));

    final Entity emp1 = entities.entity(TestDomain.T_EMP);
    emp1.put(TestDomain.EMP_DEPARTMENT_FK, dept1);
    emp1.put(TestDomain.EMP_NAME, "name");
    emp1.put(TestDomain.EMP_COMMISSION, 130.5);

    Entity copy = entities.copyEntity(emp1);
    assertTrue(emp1.valuesEqual(copy));
    assertSame(emp1.get(TestDomain.EMP_DEPARTMENT_FK), copy.get(TestDomain.EMP_DEPARTMENT_FK));

    copy = entities.deepCopyEntity(emp1);
    assertTrue(emp1.valuesEqual(copy));
    assertNotSame(emp1.get(TestDomain.EMP_DEPARTMENT_FK), copy.get(TestDomain.EMP_DEPARTMENT_FK));
  }
}
