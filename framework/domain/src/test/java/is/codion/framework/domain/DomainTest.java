/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.Serializer;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.common.event.EventListener;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.Master;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
import is.codion.framework.domain.property.ForeignKeyProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.framework.domain.TestDomain.DOMAIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.getEntities();

  @Test
  public void defineTypes() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);

    //assert types
    assertEquals(definition.getProperty(Detail.ID).getAttribute().getTypeClass(), Long.class);
    assertEquals(definition.getProperty(Detail.INT).getAttribute().getTypeClass(), Integer.class);
    assertEquals(definition.getProperty(Detail.DOUBLE).getAttribute().getTypeClass(), Double.class);
    assertEquals(definition.getProperty(Detail.STRING).getAttribute().getTypeClass(), String.class);
    assertEquals(definition.getProperty(Detail.DATE).getAttribute().getTypeClass(), LocalDate.class);
    assertEquals(definition.getProperty(Detail.TIMESTAMP).getAttribute().getTypeClass(), LocalDateTime.class);
    assertEquals(definition.getProperty(Detail.BOOLEAN).getAttribute().getTypeClass(), Boolean.class);
    assertEquals(definition.getProperty(Detail.MASTER_FK).getAttribute().getTypeClass(), Entity.class);
    assertEquals(definition.getProperty(Detail.MASTER_ID).getAttribute().getTypeClass(), Long.class);
    assertEquals(definition.getProperty(Detail.MASTER_NAME).getAttribute().getTypeClass(), String.class);
    assertEquals(definition.getProperty(Detail.MASTER_CODE).getAttribute().getTypeClass(), Integer.class);

    //assert column names
    assertEquals(definition.getProperty(Detail.ID).getAttribute(), Detail.ID);
    assertEquals(definition.getProperty(Detail.INT).getAttribute(), Detail.INT);
    assertEquals(definition.getProperty(Detail.DOUBLE).getAttribute(), Detail.DOUBLE);
    assertEquals(definition.getProperty(Detail.STRING).getAttribute(), Detail.STRING);
    assertEquals(definition.getProperty(Detail.DATE).getAttribute(), Detail.DATE);
    assertEquals(definition.getProperty(Detail.TIMESTAMP).getAttribute(), Detail.TIMESTAMP);
    assertEquals(definition.getProperty(Detail.BOOLEAN).getAttribute(), Detail.BOOLEAN);
    assertEquals(definition.getProperty(Detail.MASTER_ID).getAttribute(), Detail.MASTER_ID);
    assertEquals(definition.getProperty(Detail.MASTER_NAME).getAttribute(), Detail.MASTER_NAME);
    assertEquals(definition.getProperty(Detail.MASTER_CODE).getAttribute(), Detail.MASTER_CODE);

    //assert captions
    assertNotNull(definition.getProperty(Detail.ID).getCaption());
    assertEquals(definition.getProperty(Detail.INT).getCaption(), Detail.INT.getName());
    assertEquals(definition.getProperty(Detail.DOUBLE).getCaption(), Detail.DOUBLE.getName());
    assertEquals(definition.getProperty(Detail.STRING).getCaption(), "Detail string");
    assertEquals(definition.getProperty(Detail.DATE).getCaption(), Detail.DATE.getName());
    assertEquals(definition.getProperty(Detail.TIMESTAMP).getCaption(), Detail.TIMESTAMP.getName());
    assertEquals(definition.getProperty(Detail.BOOLEAN).getCaption(), Detail.BOOLEAN.getName());
    assertEquals(definition.getProperty(Detail.MASTER_FK).getCaption(), Detail.MASTER_FK.getName());
    assertEquals(definition.getProperty(Detail.MASTER_NAME).getCaption(), Detail.MASTER_NAME.getName());
    assertEquals(definition.getProperty(Detail.MASTER_CODE).getCaption(), Detail.MASTER_CODE.getName());

    //assert hidden status
    assertTrue(definition.getProperty(Detail.ID).isHidden());
    assertFalse(definition.getProperty(Detail.INT).isHidden());
    assertFalse(definition.getProperty(Detail.DOUBLE).isHidden());
    assertFalse(definition.getProperty(Detail.STRING).isHidden());
    assertFalse(definition.getProperty(Detail.DATE).isHidden());
    assertFalse(definition.getProperty(Detail.TIMESTAMP).isHidden());
    assertFalse(definition.getProperty(Detail.BOOLEAN).isHidden());
    assertFalse(definition.getProperty(Detail.MASTER_FK).isHidden());
    assertFalse(definition.getProperty(Detail.MASTER_NAME).isHidden());
    assertFalse(definition.getProperty(Detail.MASTER_CODE).isHidden());
  }

  @Test
  public void getPropertyWrongEntityType() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> definition.getProperty(Master.CODE));
  }

  @Test
  public void getWritableColumnProperties() {
    final EntityDefinition deptDef = entities.getDefinition(Department.TYPE);
    List<ColumnProperty<?>> writable = deptDef
            .getWritableColumnProperties(true, true);
    assertTrue(writable.contains(deptDef.getProperty(Department.NO)));
    assertTrue(writable.contains(deptDef.getProperty(Department.NAME)));
    assertTrue(writable.contains(deptDef.getProperty(Department.LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(Department.ACTIVE)));

    writable = deptDef.getWritableColumnProperties(false, true);
    assertFalse(writable.contains(deptDef.getProperty(Department.NO)));
    assertTrue(writable.contains(deptDef.getProperty(Department.NAME)));
    assertTrue(writable.contains(deptDef.getProperty(Department.LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(Department.ACTIVE)));

    writable = deptDef.getWritableColumnProperties(false, false);
    assertFalse(writable.contains(deptDef.getProperty(Department.NO)));
    assertTrue(writable.contains(deptDef.getProperty(Department.NAME)));
    assertTrue(writable.contains(deptDef.getProperty(Department.LOCATION)));
    assertFalse(writable.contains(deptDef.getProperty(Department.ACTIVE)));

    final EntityDefinition empDef = entities.getDefinition(Employee.TYPE);
    writable = empDef.getWritableColumnProperties(true, true);
    assertTrue(writable.contains(empDef.getProperty(Employee.ID)));
    assertTrue(writable.contains(empDef.getProperty(Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(Employee.NAME)));
    assertFalse(writable.contains(empDef.getProperty(Employee.DEPARTMENT_NAME)));

    writable = empDef.getWritableColumnProperties(false, true);
    assertFalse(writable.contains(empDef.getProperty(Employee.ID)));
    assertTrue(writable.contains(empDef.getProperty(Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(Employee.NAME)));
    assertFalse(writable.contains(empDef.getProperty(Employee.DEPARTMENT_NAME)));

    writable = empDef.getWritableColumnProperties(false, false);
    assertFalse(writable.contains(empDef.getProperty(Employee.ID)));
    assertFalse(writable.contains(empDef.getProperty(Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(Employee.NAME)));
    assertFalse(writable.contains(empDef.getProperty(Employee.DEPARTMENT_NAME)));

    writable = empDef.getWritableColumnProperties(true, false);
    assertFalse(writable.contains(empDef.getProperty(Employee.ID)));//overridden by includeNonUpdatable
    assertFalse(writable.contains(empDef.getProperty(Employee.HIREDATE)));
    assertTrue(writable.contains(empDef.getProperty(Employee.NAME)));
    assertFalse(writable.contains(empDef.getProperty(Employee.DEPARTMENT_NAME)));
  }

  @Test
  public void sortProperties() {
    final List<Property<?>> properties = Properties.sort(entities.getDefinition(Employee.TYPE).getProperties(
            asList(Employee.HIREDATE, Employee.COMMISSION,
                    Employee.SALARY, Employee.JOB)));
    assertEquals(Employee.COMMISSION, properties.get(0).getAttribute());
    assertEquals(Employee.HIREDATE, properties.get(1).getAttribute());
    assertEquals(Employee.JOB, properties.get(2).getAttribute());
    assertEquals(Employee.SALARY, properties.get(3).getAttribute());
  }

  @Test
  public void getUpdatableProperties() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    final List<Property<?>> properties = definition.getUpdatableProperties();
    assertEquals(10, properties.size());
    assertFalse(properties.contains(definition.getProperty(Detail.MASTER_NAME)));
    assertFalse(properties.contains(definition.getProperty(Detail.MASTER_CODE)));
    assertFalse(properties.contains(definition.getProperty(Detail.INT_DERIVED)));
  }

  @Test
  public void getSelectedProperties() {
    final List<Attribute<?>> attributes = new ArrayList<>();
    attributes.add(Department.NO);
    attributes.add(Department.NAME);

    final EntityDefinition definition = entities.getDefinition(Department.TYPE);
    final List<Property<?>> properties = definition.getProperties(attributes);
    assertEquals(2, properties.size());
    assertTrue(properties.contains(definition.getProperty(Department.NO)));
    assertTrue(properties.contains(definition.getProperty(Department.NAME)));

    final List<Property<?>> noProperties = definition.getProperties(emptyList());
    assertEquals(0, noProperties.size());
  }

  @Test
  public void key() {
    final EntityType<Entity> entityType = DOMAIN.entityType("DomainTest.key");
    final Attribute<Integer> attribute1 = entityType.attribute("id1", Integer.class);
    final Attribute<Integer> attribute2 = entityType.attribute("id2", Integer.class);
    final Attribute<Integer> attribute3 = entityType.attribute("id3", Integer.class);
    domain.define(entityType,
            Properties.primaryKeyProperty(attribute1),
            Properties.primaryKeyProperty(attribute2).primaryKeyIndex(1),
            Properties.primaryKeyProperty(attribute3).primaryKeyIndex(2).nullable(true));

    Key key = entities.primaryKey(entityType);
    assertEquals(0, key.hashCode());
    assertTrue(key.isCompositeKey());
    assertTrue(key.isNull());

    assertThrows(IllegalStateException.class, () -> entities.primaryKey(entityType).withValue(1));
    assertThrows(IllegalStateException.class, key::get);
    assertThrows(IllegalStateException.class, key::getOptional);
    assertThrows(IllegalStateException.class, key::getAttribute);

    key = key.withValue(attribute1, 1).withValue(attribute2, 2).withValue(attribute3, 3);
    assertTrue(key.isNotNull());
    assertEquals(6, key.hashCode());
    assertTrue(key.getOptional(attribute1).isPresent());

    key = key.withValue(attribute2, 3);
    assertEquals(7, key.hashCode());

    key = key.withValue(attribute3, null);
    assertTrue(key.isNotNull());
    assertEquals(4, key.hashCode());
    key = key.withValue(attribute2, null);
    assertTrue(key.isNull());
    assertFalse(key.getOptional(attribute2).isPresent());
    assertEquals(0, key.hashCode());
    key = key.withValue(attribute2, 4);
    assertTrue(key.getOptional(attribute2).isPresent());
    assertTrue(key.isNotNull());
    assertEquals(5, key.hashCode());

    key = key.withValue(attribute2, 42);
    assertTrue(key.isNotNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> entities.primaryKey(null));

    final Key noPk = entities.primaryKey(TestDomain.T_NO_PK);
    assertThrows(IllegalArgumentException.class, () -> noPk.withValue(TestDomain.NO_PK_COL1, 1));
    assertThrows(IllegalArgumentException.class, () -> noPk.get(TestDomain.NO_PK_COL1));
  }

   @Test
   public void keys() {
    final List<Key> intKeys = entities.primaryKeys(Employee.TYPE, 1, 2, 3, 4);
    assertEquals(4, intKeys.size());
    assertEquals(Integer.valueOf(3), intKeys.get(2).get());
    final List<Key> longKeys = entities.primaryKeys(Detail.TYPE, 1L, 2L, 3L, 4L);
    assertEquals(4, longKeys.size());
    assertEquals(Long.valueOf(3), longKeys.get(2).get());
   }

  @Test
  public void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType<Entity> entityType = DOMAIN.entityType("keyWithSameIndex");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)).primaryKeyIndex(0),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)).primaryKeyIndex(1),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)).primaryKeyIndex(1));
    });
  }

  @Test
  public void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType<Entity> entityType = DOMAIN.entityType("keyWithSameIndex2");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)));
    });
  }

  @Test
  public void entity() {
    final Key key = entities.primaryKey(Master.TYPE, 10L);

    final Entity master = entities.entity(key);
    assertEquals(Master.TYPE, master.getEntityType());
    assertTrue(master.containsValue(Master.ID));
    assertEquals(10L, master.get(Master.ID));

    assertThrows(NullPointerException.class, () -> entities.entity((EntityType<Entity>) null));
  }

  @Test
  public void getProperties() {
    final EntityDefinition definition = entities.getDefinition(Department.TYPE);
    final Property<Integer> id = definition.getProperty(Department.NO);
    final Property<String> location = definition.getProperty(Department.LOCATION);
    final Property<String> name = definition.getProperty(Department.NAME);
    final Property<Boolean> active = definition.getProperty(Department.ACTIVE);
    List<Property<?>> properties = definition.getProperties(asList(Department.LOCATION, Department.NAME));
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
    assertThrows(IllegalArgumentException.class, () -> entities.getDefinition(Master.TYPE)
            .getProperty(Master.TYPE.attribute("unknown property", Integer.class)));
  }

  @Test
  public void getForeignKeyReferences() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    List<ForeignKeyProperty> foreignKeyProperties = definition.getForeignKeyReferences(Employee.TYPE);
    assertEquals(0, foreignKeyProperties.size());
    foreignKeyProperties = definition.getForeignKeyReferences(Master.TYPE);
    assertEquals(2, foreignKeyProperties.size());
    assertTrue(foreignKeyProperties.contains(definition.getProperty(Detail.MASTER_FK)));
  }

  @Test
  public void getForeignKeyProperty() {
    assertNotNull(entities.getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.MASTER_FK));
  }

  @Test
  public void getForeignKeyPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> entities.getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.TYPE.entityAttribute("bla bla")));
  }

  @Test
  public void hasDerivedAttributes() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    assertFalse(definition.hasDerivedAttributes(Detail.BOOLEAN));
    assertTrue(definition.hasDerivedAttributes(Detail.INT));
  }

  @Test
  public void getDerivedAttributes() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    Collection<Attribute<?>> derivedAttributes = definition.getDerivedAttributes(Detail.BOOLEAN);
    assertTrue(derivedAttributes.isEmpty());
    derivedAttributes = definition.getDerivedAttributes(Detail.INT);
    assertEquals(1, derivedAttributes.size());
    assertTrue(derivedAttributes.contains(Detail.INT_DERIVED));
  }

  @Test
  public void hasDenormalizedProperties() {
    assertTrue(entities.getDefinition(Detail.TYPE).hasDenormalizedProperties());
    assertTrue(entities.getDefinition(Detail.TYPE).hasDenormalizedProperties(Detail.MASTER_FK));
  }

  @Test
  public void getDenormalizedProperties() {
    final List<DenormalizedProperty<?>> denormalized =
            entities.getDefinition(Detail.TYPE).getDenormalizedProperties(Detail.MASTER_FK);
    assertFalse(denormalized.isEmpty());
    assertEquals(Detail.MASTER_CODE_DENORM, denormalized.get(0).getAttribute());
  }

  @Test
  public void isSmallDataset() {
    assertTrue(entities.getDefinition(Detail.TYPE).isSmallDataset());
  }

  @Test
  public void getStringProvider() {
    assertNotNull(entities.getDefinition(Department.TYPE).getStringProvider());
  }

  @Test
  public void redefine() {
    final EntityType<Entity> entityType = DOMAIN.entityType("redefine");
    final Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    domain.define(entityType, Properties.primaryKeyProperty(attribute));
    assertThrows(IllegalArgumentException.class, () -> domain.define(entityType, Properties.primaryKeyProperty(attribute)));
  }

  @Test
  public void nullValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.entity(Employee.TYPE);
    emp.put(Employee.NAME, "Name");
    emp.put(Employee.HIREDATE, LocalDateTime.now());
    emp.put(Employee.SALARY, 1200.0);

    final DefaultEntityValidator validator = new DefaultEntityValidator();
    try {
      validator.validate(emp, definition);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(Employee.DEPARTMENT_FK, e.getAttribute());
    }
    emp.put(Employee.DEPARTMENT, 1);
    try {
      validator.validate(emp, definition);
    }
    catch (final ValidationException e) {
      fail();
    }
    emp.put(Employee.SALARY, null);
    try {
      validator.validate(emp, definition);
      fail();
    }
    catch (final ValidationException e) {
      assertTrue(e instanceof NullValidationException);
      assertEquals(Employee.SALARY, e.getAttribute());
    }
  }

  @Test
  public void maxLengthValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.entity(Employee.TYPE);
    emp.put(Employee.DEPARTMENT, 1);
    emp.put(Employee.NAME, "Name");
    emp.put(Employee.HIREDATE, LocalDateTime.now());
    emp.put(Employee.SALARY, 1200.0);
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(Employee.NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp, definition));
  }

  @Test
  public void rangeValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.entity(Employee.TYPE);
    emp.put(Employee.DEPARTMENT, 1);
    emp.put(Employee.NAME, "Name");
    emp.put(Employee.HIREDATE, LocalDateTime.now());
    emp.put(Employee.SALARY, 1200d);
    emp.put(Employee.COMMISSION, 300d);
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(Employee.COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp, definition));
    emp.put(Employee.COMMISSION, 2100d);
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
  public void getSearchAttributes() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    Collection<Attribute<String>> searchAttributes = definition.getSearchAttributes();
    assertTrue(searchAttributes.contains(Employee.JOB));
    assertTrue(searchAttributes.contains(Employee.NAME));

    searchAttributes = entities.getDefinition(Department.TYPE).getSearchAttributes();
    //should contain all string based properties
    assertTrue(searchAttributes.contains(Department.NAME));
  }

  @Test
  public void selectableProperty() {
    assertThrows(IllegalArgumentException.class, () ->
            entities.getDefinition(Detail.TYPE).getSelectableColumnProperties(singletonList(Detail.STRING)));
  }

  @Test
  public void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType<Entity> entityType = DOMAIN.entityType("test.entity");
      final Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
      final EntityType<Entity> referencedEntityType = DOMAIN.entityType("test.referenced_entity");
      final Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
              Properties.columnProperty(fkId),
              Properties.foreignKeyProperty(entityType.entityAttribute("fk_id_fk"), "caption")
                      .reference(fkId, refId));
    });
  }

  @Test
  public void foreignKeyReferencingUndefinedEntityNonStrict() {
    domain.setStrictForeignKeys(false);
    final EntityType<Entity> entityType = DOMAIN.entityType("test.entity");
    final Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
    final EntityType<Entity> referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    final Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
            Properties.columnProperty(fkId),
            Properties.foreignKeyProperty(entityType.entityAttribute("fk_id_fk"), "caption")
                    .reference(fkId, refId));
    domain.setStrictForeignKeys(true);
  }

  @Test
  public void hasSingleIntegerPrimaryKey() {
    EntityType<Entity> entityType = DOMAIN.entityType("hasSingleIntegerPrimaryKey");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", Integer.class))
                    .primaryKeyIndex(0));
    assertTrue(entities.getDefinition(entityType).hasSingleIntegerPrimaryKey());
    entityType = DOMAIN.entityType("hasSingleIntegerPrimaryKey2");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", Integer.class))
                    .primaryKeyIndex(0),
            Properties.columnProperty(entityType.attribute("test2", Integer.class))
                    .primaryKeyIndex(1));
    assertFalse(entities.getDefinition(entityType).hasSingleIntegerPrimaryKey());
    entityType = DOMAIN.entityType("hasSingleIntegerPrimaryKey3");
    domain.define(entityType,
            Properties.columnProperty(entityType.attribute("test", String.class))
                    .primaryKeyIndex(0));
    assertFalse(entities.getDefinition(entityType).hasSingleIntegerPrimaryKey());
  }

  @Test
  public void havingClause() {
    final String havingClause = "p1 > 1";
    final EntityType<Entity> entityType = DOMAIN.entityType("entityType3");
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("p0", Integer.class))).havingClause(havingClause);
    assertEquals(havingClause, entities.getDefinition(entityType).getHavingClause());
  }

  @Test
  public void validateTypeEntity() {
    final Entity entity = entities.entity(Detail.TYPE);
    final Entity entity1 = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.MASTER_FK, entity1));
  }

  @Test
  public void setValueDerived() {
    final Entity entity = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_DERIVED, 10));
  }

  @Test
  public void setValueValueList() {
    final Entity entity = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_VALUE_LIST, -10));
  }

  @Test
  public void defineProcedureExisting() {
    final ProcedureType<DatabaseConnection, Object> procedureType = ProcedureType.procedureType("operationId");
    final DatabaseProcedure<DatabaseConnection, Object> operation = (databaseConnection, arguments) -> {};
    domain.defineProcedure(procedureType, operation);
    assertThrows(IllegalArgumentException.class, () -> domain.defineProcedure(procedureType, operation));
  }

  @Test
  public void defineFunctionExisting() {
    final FunctionType<DatabaseConnection, Object, Object> functionType = FunctionType.functionType("operationId");
    final DatabaseFunction<DatabaseConnection, Object, Object> function = (databaseConnection, arguments) -> null;
    domain.defineFunction(functionType, function);
    assertThrows(IllegalArgumentException.class, () -> domain.defineFunction(functionType, function));
  }

  @Test
  public void getFunctionNonExisting() {
    final FunctionType<?, ?, ?> functionType = FunctionType.functionType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.getFunction(functionType));
  }

  @Test
  public void getProcedureNonExisting() {
    final ProcedureType<?, ?> procedureType = ProcedureType.procedureType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.getProcedure(procedureType));
  }

  @Test
  public void conditionProvider() {
    final EntityType<Entity> nullConditionProvider1 = DOMAIN.entityType("nullConditionProvider1");
    assertThrows(NullPointerException.class, () -> domain.define(nullConditionProvider1,
            Properties.primaryKeyProperty(nullConditionProvider1.integerAttribute("id"))).conditionProvider(null, (attributes, values) -> null));
    final EntityType<Entity> nullConditionProvider2 = DOMAIN.entityType("nullConditionProvider2");
    assertThrows(NullPointerException.class, () -> domain.define(nullConditionProvider2,
            Properties.primaryKeyProperty(nullConditionProvider2.integerAttribute("id"))).conditionProvider(
                    nullConditionProvider2.conditionType("id"), null));
    final EntityType<Entity> nullConditionProvider3 = DOMAIN.entityType("nullConditionProvider3");
    final ConditionType nullConditionType = nullConditionProvider3.conditionType("id");
    assertThrows(IllegalStateException.class, () -> domain.define(nullConditionProvider3,
            Properties.primaryKeyProperty(nullConditionProvider3.integerAttribute("id")))
            .conditionProvider(nullConditionType, (attributes, values) -> null)
            .conditionProvider(nullConditionType, (attributes, values) -> null));
  }

  @Test
  public void copyEntities() {
    final Entity dept1 = entities.entity(Department.TYPE);
    dept1.put(Department.NO, 1);
    dept1.put(Department.LOCATION, "location");
    dept1.put(Department.NAME, "name");
    final Entity dept2 = entities.entity(Department.TYPE);
    dept2.put(Department.NO, 2);
    dept2.put(Department.LOCATION, "location2");
    dept2.put(Department.NAME, "name2");

    final List<Entity> copies = entities.deepCopyEntities(asList(dept1, dept2));
    assertNotSame(copies.get(0), dept1);
    assertTrue(copies.get(0).columnValuesEqual(dept1));
    assertNotSame(copies.get(1), dept2);
    assertTrue(copies.get(1).columnValuesEqual(dept2));

    final Entity emp1 = entities.entity(Employee.TYPE);
    emp1.put(Employee.DEPARTMENT_FK, dept1);
    emp1.put(Employee.NAME, "name");
    emp1.put(Employee.COMMISSION, 130.5);

    Entity copy = entities.copyEntity(emp1);
    assertTrue(emp1.columnValuesEqual(copy));
    assertSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));

    copy = entities.deepCopyEntity(emp1);
    assertTrue(emp1.columnValuesEqual(copy));
    assertNotSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
  }

  @Test
  public void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Entity department = entities.entity(Department.TYPE);
    department.put(Department.NO, deptNo);
    department.put(Department.NAME, deptName);
    department.put(Department.LOCATION, deptLocation);
    department.put(Department.ACTIVE, deptActive);

    final List<Department> deptBeans = entities.castTo(Department.TYPE, singletonList(department));
    final Department departmentBean = deptBeans.get(0);
    assertEquals(deptNo, departmentBean.deptNo());
    assertEquals(deptName, departmentBean.name());
    assertEquals(deptLocation, departmentBean.location());
    assertEquals(deptActive, departmentBean.active());

    departmentBean.active(false);

    assertFalse(department.get(Department.ACTIVE));

    final Entity manager = entities.entity(Employee.TYPE);
    manager.put(Employee.ID, 12);

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = entities.entity(Employee.TYPE);
    employee.put(Employee.ID, id);
    employee.put(Employee.COMMISSION, commission);
    employee.put(Employee.DEPARTMENT_FK, department);
    employee.put(Employee.HIREDATE, hiredate);
    employee.put(Employee.JOB, job);
    employee.put(Employee.MANAGER_FK, manager);
    employee.put(Employee.NAME, name);
    employee.put(Employee.SALARY, salary);

    final List<Employee> empBeans = entities.castTo(Employee.TYPE, singletonList(employee));
    final Employee employeeBean = empBeans.get(0);
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

    assertNull(entities.castTo(Employee.TYPE, (Entity) null));
    assertTrue(entities.castTo(Employee.TYPE, emptyList()).isEmpty());
  }

  @Test
  public void toEntityType() {
    final Entity master = entities.entity(Master.TYPE);
    master.put(Master.ID, 1L);
    master.put(Master.CODE, 1);
    master.put(Master.NAME, "name");

    final Master master1 = entities.castTo(Master.TYPE, master);

    final Entity master2 = entities.entity(Master.TYPE);
    master2.put(Master.ID, 2L);
    master2.put(Master.CODE, 2);
    master2.put(Master.NAME, "name2");

    final List<Entity> masters = asList(master, master1, master2);

    final List<Master> mastersTyped = entities.castTo(Master.TYPE, masters);

    assertSame(master1, mastersTyped.get(1));

    assertEquals(1L, mastersTyped.get(0).get(Master.ID));
    assertEquals(1L, mastersTyped.get(1).get(Master.ID));
    assertEquals(2L, mastersTyped.get(2).get(Master.ID));

    assertEquals(1L, mastersTyped.get(0).getId());
    assertEquals("name", mastersTyped.get(0).getName());

    final Entity detail = entities.entity(Detail.TYPE);
    detail.put(Detail.ID, 1L);
    detail.put(Detail.DOUBLE, 1.2);
    detail.put(Detail.MASTER_FK, master);

    final Detail detailTyped = entities.castTo(Detail.TYPE, detail);
    assertEquals(detailTyped.getId().get(), 1L);
    assertEquals(detailTyped.getDouble().get(), 1.2);
    assertEquals(detailTyped.getMaster().get(), master);

    detailTyped.setId(2L);
    detailTyped.setDouble(2.1);
    detailTyped.setMaster(master1);

    assertEquals(detailTyped.getId().get(), detail.get(Detail.ID));
    assertEquals(detailTyped.getDouble().get(), detail.get(Detail.DOUBLE));
    assertSame(detailTyped.getMaster().get(), detail.get(Detail.MASTER_FK));

    detailTyped.setAll(3L, 3.2, mastersTyped.get(2));

    assertEquals(detailTyped.getId().get(), 3L);
    assertEquals(detailTyped.getDouble().get(), 3.2);
    assertSame(detailTyped.getMaster().get(), mastersTyped.get(2));

    final Entity compositeMaster = entities.entity(TestDomain.T_COMPOSITE_MASTER);
    compositeMaster.put(TestDomain.COMPOSITE_MASTER_ID, 1);
    compositeMaster.put(TestDomain.COMPOSITE_MASTER_ID_2, 2);

    assertSame(compositeMaster, entities.castTo(TestDomain.T_COMPOSITE_MASTER, compositeMaster));
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final List<Entity> entitiesToSer = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      final Entity entity = entities.entity(Master.TYPE);
      entity.put(Master.ID, (long) i);
      entity.put(Master.NAME, Integer.toString(i));
      entity.put(Master.CODE, 1);
      entitiesToSer.add(entity);
    }

    Serializer.deserialize(Serializer.serialize(entities.castTo(Master.TYPE, entitiesToSer)));
  }

  @Test
  public void extendedDomain() {
    final TestDomainExtended extended = new TestDomainExtended();
    Entities entities = extended.getEntities();

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.T_COMPOSITE_MASTER);

    final TestDomainExtended.TestDomainSecondExtenion second = new TestDomainExtended.TestDomainSecondExtenion();
    entities = second.getEntities();

    entities.entity(TestDomainExtended.TestDomainSecondExtenion.T_SECOND_EXTENDED);

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.T_COMPOSITE_MASTER);
  }
}
