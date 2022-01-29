/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.Serializer;
import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.domain.TestDomain.Department;
import is.codion.framework.domain.TestDomain.Detail;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.TestDomain.Master;
import is.codion.framework.domain.TestDomain.NoPk;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.DefaultEntityValidator;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.LengthValidationException;
import is.codion.framework.domain.entity.exception.NullValidationException;
import is.codion.framework.domain.entity.exception.RangeValidationException;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ColumnProperty;
import is.codion.framework.domain.property.DenormalizedProperty;
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

import static is.codion.framework.domain.TestDomain.DOMAIN;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public class DomainTest {

  private final TestDomain domain = new TestDomain();
  private final Entities entities = domain.getEntities();

  @Test
  void defineTypes() {
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
  void getPropertyWrongEntityType() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> definition.getProperty(Master.CODE));
  }

  @Test
  void getWritableColumnProperties() {
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
  void sortProperties() {
    final List<Property<?>> properties = Properties.sort(entities.getDefinition(Employee.TYPE).getProperties(
            asList(Employee.HIREDATE, Employee.COMMISSION,
                    Employee.SALARY, Employee.JOB)));
    assertEquals(Employee.COMMISSION, properties.get(0).getAttribute());
    assertEquals(Employee.HIREDATE, properties.get(1).getAttribute());
    assertEquals(Employee.JOB, properties.get(2).getAttribute());
    assertEquals(Employee.SALARY, properties.get(3).getAttribute());
  }

  @Test
  void getUpdatableProperties() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    final List<Property<?>> properties = definition.getUpdatableProperties();
    assertEquals(10, properties.size());
    assertFalse(properties.contains(definition.getProperty(Detail.MASTER_NAME)));
    assertFalse(properties.contains(definition.getProperty(Detail.MASTER_CODE)));
    assertFalse(properties.contains(definition.getProperty(Detail.INT_DERIVED)));
  }

  @Test
  void getSelectedProperties() {
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
  void key() {
    final EntityType entityType = DOMAIN.entityType("DomainTest.key");
    final Attribute<Integer> attribute1 = entityType.attribute("id1", Integer.class);
    final Attribute<Integer> attribute2 = entityType.attribute("id2", Integer.class);
    final Attribute<Integer> attribute3 = entityType.attribute("id3", Integer.class);
    domain.define(entityType,
            Properties.primaryKeyProperty(attribute1),
            Properties.primaryKeyProperty(attribute2).primaryKeyIndex(1),
            Properties.primaryKeyProperty(attribute3).primaryKeyIndex(2).nullable(true));

    Key key = entities.keyBuilder(entityType).build();
    assertEquals(0, key.hashCode());
    assertTrue(key.getAttributes().isEmpty());
    assertTrue(key.isNull());

    assertThrows(IllegalStateException.class, () -> entities.primaryKey(entityType, 1));
    assertThrows(IllegalStateException.class, key::get);
    assertThrows(IllegalStateException.class, key::getOptional);
    assertThrows(IllegalStateException.class, key::getAttribute);

    key = key.copyBuilder()
            .with(attribute1, 1)
            .with(attribute2, 2)
            .with(attribute3, 3)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(6, key.hashCode());
    assertTrue(key.getOptional(attribute1).isPresent());

    key = key.copyBuilder()
            .with(attribute2, 3)
            .build();
    assertEquals(7, key.hashCode());

    key = key.copyBuilder()
            .with(attribute3, null)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(4, key.hashCode());
    key = key.copyBuilder()
            .with(attribute2, null)
            .build();
    assertTrue(key.isNull());
    assertFalse(key.getOptional(attribute2).isPresent());
    assertEquals(0, key.hashCode());
    key = key.copyBuilder()
            .with(attribute2, 4)
            .build();
    assertTrue(key.getOptional(attribute2).isPresent());
    assertTrue(key.isNotNull());
    assertEquals(5, key.hashCode());

    key = key.copyBuilder()
            .with(attribute2, 42)
            .build();
    assertTrue(key.isNotNull());
    assertEquals(43, key.hashCode());

    assertThrows(NullPointerException.class, () -> entities.keyBuilder(null));

    assertFalse(entities.keyBuilder(NoPk.TYPE)
            .with(NoPk.COL1, 1)
            .build()
            .isPrimaryKey());
    final Key noPk = entities.keyBuilder(NoPk.TYPE).build();
    assertThrows(IllegalArgumentException.class, () -> noPk.get(NoPk.COL1));
  }

  @Test
  void keys() {
    final List<Key> intKeys = entities.primaryKeys(Employee.TYPE, 1, 2, 3, 4);
    assertEquals(4, intKeys.size());
    assertEquals(Integer.valueOf(3), intKeys.get(2).get());
    final List<Key> longKeys = entities.primaryKeys(Detail.TYPE, 1L, 2L, 3L, 4L);
    assertEquals(4, longKeys.size());
    assertEquals(Long.valueOf(3), longKeys.get(2).get());
  }

  @Test
  void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = DOMAIN.entityType("keyWithSameIndex");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)).primaryKeyIndex(0),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)).primaryKeyIndex(1),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)).primaryKeyIndex(1));
    });
  }

  @Test
  void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = DOMAIN.entityType("keyWithSameIndex2");
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("1", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("2", Integer.class)),
              Properties.primaryKeyProperty(entityType.attribute("3", Integer.class)));
    });
  }

  @Test
  void entity() {
    final Key key = entities.primaryKey(Master.TYPE, 10L);

    final Entity master = entities.entity(key);
    assertEquals(Master.TYPE, master.getEntityType());
    assertTrue(master.contains(Master.ID));
    assertEquals(10L, master.get(Master.ID));

    assertThrows(NullPointerException.class, () -> entities.entity((EntityType) null));
  }

  @Test
  void getProperties() {
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
  void getPropertyInvalid() {
    assertThrows(IllegalArgumentException.class, () -> entities.getDefinition(Master.TYPE)
            .getProperty(Master.TYPE.attribute("unknown property", Integer.class)));
  }

  @Test
  void getForeignKeys() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    List<ForeignKey> foreignKeys = definition.getForeignKeys(Employee.TYPE);
    assertEquals(0, foreignKeys.size());
    foreignKeys = definition.getForeignKeys(Master.TYPE);
    assertEquals(2, foreignKeys.size());
    assertTrue(foreignKeys.contains(Detail.MASTER_FK));
  }

  @Test
  void getForeignKeyProperty() {
    assertNotNull(entities.getDefinition(Detail.TYPE).getForeignKeyProperty(Detail.MASTER_FK));
  }

  @Test
  void getForeignKeyPropertyInvalid() {
    final ForeignKey foreignKey = Detail.TYPE.foreignKey("bla bla", Detail.MASTER_ID, Master.ID);
    assertThrows(IllegalArgumentException.class, () -> entities.getDefinition(Detail.TYPE).getForeignKeyProperty(foreignKey));
  }

  @Test
  void hasDerivedAttributes() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    assertFalse(definition.hasDerivedAttributes(Detail.BOOLEAN));
    assertTrue(definition.hasDerivedAttributes(Detail.INT));
  }

  @Test
  void getDerivedAttributes() {
    final EntityDefinition definition = entities.getDefinition(Detail.TYPE);
    Collection<Attribute<?>> derivedAttributes = definition.getDerivedAttributes(Detail.BOOLEAN);
    assertTrue(derivedAttributes.isEmpty());
    derivedAttributes = definition.getDerivedAttributes(Detail.INT);
    assertEquals(1, derivedAttributes.size());
    assertTrue(derivedAttributes.contains(Detail.INT_DERIVED));
  }

  @Test
  void hasDenormalizedProperties() {
    assertTrue(entities.getDefinition(Detail.TYPE).hasDenormalizedProperties());
    assertTrue(entities.getDefinition(Detail.TYPE).hasDenormalizedProperties(Detail.MASTER_FK));
  }

  @Test
  void getDenormalizedProperties() {
    final List<DenormalizedProperty<?>> denormalized =
            entities.getDefinition(Detail.TYPE).getDenormalizedProperties(Detail.MASTER_FK);
    assertFalse(denormalized.isEmpty());
    assertEquals(Detail.MASTER_CODE_DENORM, denormalized.get(0).getAttribute());
  }

  @Test
  void isSmallDataset() {
    assertTrue(entities.getDefinition(Detail.TYPE).isSmallDataset());
  }

  @Test
  void getStringFactory() {
    assertNotNull(entities.getDefinition(Department.TYPE).getStringFactory());
  }

  @Test
  void redefine() {
    final EntityType entityType = DOMAIN.entityType("redefine");
    final Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    domain.define(entityType, Properties.primaryKeyProperty(attribute));
    assertThrows(IllegalArgumentException.class, () -> domain.define(entityType, Properties.primaryKeyProperty(attribute)));
  }

  @Test
  void nullValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.builder(Employee.TYPE)
            .with(Employee.NAME, "Name")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.SALARY, 1200.0)
            .build();

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
  void maxLengthValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT, 1)
            .with(Employee.NAME, "Name")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.SALARY, 1200.0)
            .build();
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(Employee.NAME, "LooooongName");
    assertThrows(LengthValidationException.class, () -> validator.validate(emp, definition));
  }

  @Test
  void rangeValidation() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    final Entity emp = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT, 1)
            .with(Employee.NAME, "Name")
            .with(Employee.HIREDATE, LocalDateTime.now())
            .with(Employee.SALARY, 1200d)
            .with(Employee.COMMISSION, 300d)
            .build();
    final DefaultEntityValidator validator = new DefaultEntityValidator();
    assertDoesNotThrow(() -> validator.validate(singletonList(emp), definition));
    emp.put(Employee.COMMISSION, 10d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp, definition));
    emp.put(Employee.COMMISSION, 2100d);
    assertThrows(RangeValidationException.class, () -> validator.validate(emp, definition));
  }

  @Test
  void getSearchAttributes() {
    final EntityDefinition definition = entities.getDefinition(Employee.TYPE);
    Collection<Attribute<String>> searchAttributes = definition.getSearchAttributes();
    assertTrue(searchAttributes.contains(Employee.JOB));
    assertTrue(searchAttributes.contains(Employee.NAME));

    searchAttributes = entities.getDefinition(Department.TYPE).getSearchAttributes();
    //should contain all string based properties
    assertTrue(searchAttributes.contains(Department.NAME));
  }

  @Test
  void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      final EntityType entityType = DOMAIN.entityType("test.entity");
      final Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
      final EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
      final Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
      final ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
      domain.define(entityType,
              Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
              Properties.columnProperty(fkId),
              Properties.foreignKeyProperty(foreignKey, "caption"));
    });
  }

  @Test
  void foreignKeyReferencingUndefinedEntityNonStrict() {
    domain.setStrictForeignKeys(false);
    final EntityType entityType = DOMAIN.entityType("test.entity");
    final Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
    final EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    final Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
    final ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("id", Integer.class)),
            Properties.columnProperty(fkId),
            Properties.foreignKeyProperty(foreignKey, "caption"));
    domain.setStrictForeignKeys(true);
  }

  @Test
  void hasSingleIntegerPrimaryKey() {
    EntityType entityType = DOMAIN.entityType("hasSingleIntegerPrimaryKey");
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
  void havingClause() {
    final String havingClause = "p1 > 1";
    final EntityType entityType = DOMAIN.entityType("entityType3");
    domain.define(entityType,
            Properties.primaryKeyProperty(entityType.attribute("p0", Integer.class))).havingClause(havingClause);
    assertEquals(havingClause, entities.getDefinition(entityType).getHavingClause());
  }

  @Test
  void validateTypeEntity() {
    final Entity entity = entities.entity(Detail.TYPE);
    final Entity entity1 = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.MASTER_FK, entity1));
  }

  @Test
  void setValueDerived() {
    final Entity entity = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_DERIVED, 10));
  }

  @Test
  void setValueItem() {
    final Entity entity = entities.entity(Detail.TYPE);
    assertThrows(IllegalArgumentException.class, () -> entity.put(Detail.INT_VALUE_LIST, -10));
  }

  @Test
  void defineProcedureExisting() {
    final ProcedureType<DatabaseConnection, Object> procedureType = ProcedureType.procedureType("operationId");
    final DatabaseProcedure<DatabaseConnection, Object> operation = (databaseConnection, arguments) -> {};
    domain.defineProcedure(procedureType, operation);
    assertThrows(IllegalArgumentException.class, () -> domain.defineProcedure(procedureType, operation));
  }

  @Test
  void defineFunctionExisting() {
    final FunctionType<DatabaseConnection, Object, Object> functionType = FunctionType.functionType("operationId");
    final DatabaseFunction<DatabaseConnection, Object, Object> function = (databaseConnection, arguments) -> null;
    domain.defineFunction(functionType, function);
    assertThrows(IllegalArgumentException.class, () -> domain.defineFunction(functionType, function));
  }

  @Test
  void getFunctionNonExisting() {
    final FunctionType<?, ?, ?> functionType = FunctionType.functionType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.getFunction(functionType));
  }

  @Test
  void getProcedureNonExisting() {
    final ProcedureType<?, ?> procedureType = ProcedureType.procedureType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.getProcedure(procedureType));
  }

  @Test
  void conditionProvider() {
    final EntityType nullConditionProvider1 = DOMAIN.entityType("nullConditionProvider1");
    assertThrows(NullPointerException.class, () -> domain.define(nullConditionProvider1,
            Properties.primaryKeyProperty(nullConditionProvider1.integerAttribute("id"))).conditionProvider(null, (attributes, values) -> null));
    final EntityType nullConditionProvider2 = DOMAIN.entityType("nullConditionProvider2");
    assertThrows(NullPointerException.class, () -> domain.define(nullConditionProvider2,
            Properties.primaryKeyProperty(nullConditionProvider2.integerAttribute("id"))).conditionProvider(
            nullConditionProvider2.conditionType("id"), null));
    final EntityType nullConditionProvider3 = DOMAIN.entityType("nullConditionProvider3");
    final ConditionType nullConditionType = nullConditionProvider3.conditionType("id");
    assertThrows(IllegalStateException.class, () -> domain.define(nullConditionProvider3,
                    Properties.primaryKeyProperty(nullConditionProvider3.integerAttribute("id")))
            .conditionProvider(nullConditionType, (attributes, values) -> null)
            .conditionProvider(nullConditionType, (attributes, values) -> null));
  }

  @Test
  void missingForeignKeyReferenceProperty() {
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testForeignKeys());
  }

  @Test
  void misconfiguredPrimaryKeyPropertyIndexes() {
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes1());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes2());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes3());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes4());
  }

  @Test
  void copyEntities() {
    final Entity dept1 = entities.builder(Department.TYPE)
            .with(Department.NO, 1)
            .with(Department.LOCATION, "location")
            .with(Department.NAME, "name")
            .build();
    final Entity dept2 = entities.builder(Department.TYPE)
            .with(Department.NO, 2)
            .with(Department.LOCATION, "location2")
            .with(Department.NAME, "name2")
            .build();

    final List<Entity> copies = Entity.deepCopy(asList(dept1, dept2));
    assertNotSame(copies.get(0), dept1);
    assertTrue(copies.get(0).columnValuesEqual(dept1));
    assertNotSame(copies.get(1), dept2);
    assertTrue(copies.get(1).columnValuesEqual(dept2));

    final Entity emp1 = entities.builder(Employee.TYPE)
            .with(Employee.DEPARTMENT_FK, dept1)
            .with(Employee.NAME, "name")
            .with(Employee.COMMISSION, 130.5)
            .build();

    Entity copy = emp1.copy();
    assertTrue(emp1.columnValuesEqual(copy));
    assertSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
    assertFalse(emp1.isModified());

    copy = emp1.deepCopy();
    assertTrue(emp1.columnValuesEqual(copy));
    assertNotSame(emp1.get(Employee.DEPARTMENT_FK), copy.get(Employee.DEPARTMENT_FK));
    assertFalse(emp1.isModified());
  }

  @Test
  void toBeans() throws InvocationTargetException, NoSuchMethodException,
          InstantiationException, IllegalAccessException {
    final Integer deptNo = 13;
    final String deptName = "Department";
    final String deptLocation = "Location";
    final Boolean deptActive = true;

    final Entity department = entities.builder(Department.TYPE)
            .with(Department.NO, deptNo)
            .with(Department.NAME, deptName)
            .with(Department.LOCATION, deptLocation)
            .with(Department.ACTIVE, deptActive)
            .build();

    final List<Department> deptBeans = Entity.castTo(Department.class, singletonList(department));
    final Department departmentBean = deptBeans.get(0);
    assertEquals(deptNo, departmentBean.deptNo());
    assertEquals(deptName, departmentBean.name());
    assertEquals(deptLocation, departmentBean.location());
    assertEquals(deptActive, departmentBean.active());

    departmentBean.active(false);

    assertFalse(department.get(Department.ACTIVE));

    final Entity manager = entities.builder(Employee.TYPE)
            .with(Employee.ID, 12)
            .build();

    final Integer id = 42;
    final Double commission = 42.2;
    final LocalDateTime hiredate = LocalDateTime.now();
    final String job = "CLERK";
    final Integer mgr = 12;
    final String name = "John Doe";
    final Double salary = 1234.5;

    final Entity employee = entities.builder(Employee.TYPE)
            .with(Employee.ID, id)
            .with(Employee.COMMISSION, commission)
            .with(Employee.DEPARTMENT_FK, department)
            .with(Employee.HIREDATE, hiredate)
            .with(Employee.JOB, job)
            .with(Employee.MANAGER_FK, manager)
            .with(Employee.NAME, name)
            .with(Employee.SALARY, salary)
            .build();

    final List<Employee> empBeans = Entity.castTo(Employee.class, singletonList(employee));
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

    assertTrue(Entity.castTo(Employee.class, emptyList()).isEmpty());
  }

  @Test
  void toEntityType() {
    final Entity master = entities.builder(Master.TYPE)
            .with(Master.ID, 1L)
            .with(Master.CODE, 1)
            .with(Master.NAME, "name")
            .build();

    final Master master1 = master.castTo(Master.class);

    assertSame(master1, master1.castTo(Master.class));

    final Entity master2 = entities.builder(Master.TYPE)
            .with(Master.ID, 2L)
            .with(Master.CODE, 2)
            .with(Master.NAME, "name2")
            .build();

    final List<Entity> masters = asList(master, master1, master2);

    final List<Master> mastersTyped = Entity.castTo(Master.class, masters);

    assertSame(master1, mastersTyped.get(1));

    assertEquals(1L, mastersTyped.get(0).get(Master.ID));
    assertEquals(1L, mastersTyped.get(1).get(Master.ID));
    assertEquals(2L, mastersTyped.get(2).get(Master.ID));

    assertEquals(1L, mastersTyped.get(0).getId());
    assertEquals("name", mastersTyped.get(0).getName());

    final Entity detail = entities.builder(Detail.TYPE)
            .with(Detail.ID, 1L)
            .with(Detail.DOUBLE, 1.2)
            .with(Detail.MASTER_FK, master)
            .build();

    final Detail detailTyped = detail.castTo(Detail.class);
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

    final Entity compositeMaster = entities.builder(TestDomain.T_COMPOSITE_MASTER)
            .with(TestDomain.COMPOSITE_MASTER_ID, 1)
            .with(TestDomain.COMPOSITE_MASTER_ID_2, 2)
            .build();

    assertSame(compositeMaster, compositeMaster.castTo(Entity.class));
  }

  @Test
  void serialize() throws IOException, ClassNotFoundException {
    final List<Entity> entitiesToSer = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      entitiesToSer.add(entities.builder(Master.TYPE)
              .with(Master.ID, (long) i)
              .with(Master.NAME, Integer.toString(i))
              .with(Master.CODE, 1)
              .build());
    }

    Serializer.deserialize(Serializer.serialize(Entity.castTo(Master.class, entitiesToSer)));
  }

  @Test
  void extendedDomain() {
    final TestDomainExtended extended = new TestDomainExtended();
    Entities entities = extended.getEntities();

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.T_COMPOSITE_MASTER);

    final TestDomainExtended.TestDomainSecondExtension second = new TestDomainExtended.TestDomainSecondExtension();
    entities = second.getEntities();

    entities.entity(TestDomainExtended.TestDomainSecondExtension.T_SECOND_EXTENDED);

    entities.entity(TestDomainExtended.T_EXTENDED);

    entities.entity(TestDomain.T_COMPOSITE_MASTER);

    assertNotNull(second.getProcedure(TestDomainExtended.PROC_TYPE));
    assertNotNull(second.getFunction(TestDomainExtended.FUNC_TYPE));
    assertNotNull(second.getReport(TestDomainExtended.REP_TYPE));

    //entity type name clash
    assertThrows(IllegalArgumentException.class, TestDomainExtended.TestDomainThirdExtension::new);
  }
}
