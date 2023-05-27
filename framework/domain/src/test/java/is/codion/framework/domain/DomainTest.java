/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.TestDomain.DOMAIN;
import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.columnProperty;
import static is.codion.framework.domain.property.Property.primaryKeyProperty;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DomainTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("keyWithSameIndex");
      domain.add(definition(
              primaryKeyProperty(entityType.attribute("1", Integer.class)).primaryKeyIndex(0),
              primaryKeyProperty(entityType.attribute("2", Integer.class)).primaryKeyIndex(1),
              primaryKeyProperty(entityType.attribute("3", Integer.class)).primaryKeyIndex(1)));
    });
  }

  @Test
  void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("keyWithSameIndex2");
      domain.add(definition(
              primaryKeyProperty(entityType.attribute("1", Integer.class)),
              primaryKeyProperty(entityType.attribute("2", Integer.class)),
              primaryKeyProperty(entityType.attribute("3", Integer.class))));
    });
  }

  @Test
  void redefine() {
    EntityType entityType = DOMAIN.entityType("redefine");
    Attribute<Integer> attribute = entityType.integerAttribute("attribute");
    domain.add(definition(primaryKeyProperty(attribute)));
    assertThrows(IllegalArgumentException.class, () -> domain.add(definition(primaryKeyProperty(attribute))));
  }

  @Test
  void foreignKeyReferencingUndefinedAttribute() {
    EntityType entityType = DOMAIN.entityType("test.entity");
    Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
    EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
    Attribute<Integer> refIdNotPartOfEntity = referencedEntityType.attribute("id_none", Integer.class);
    ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refIdNotPartOfEntity);
    domain.add(definition(primaryKeyProperty(refId)));

    assertThrows(IllegalArgumentException.class, () -> domain.add(definition(
            primaryKeyProperty(entityType.attribute("id", Integer.class)),
            columnProperty(fkId),
            Property.foreignKeyProperty(foreignKey, "caption"))));
  }

  @Test
  void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("test.entity");
      Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
      EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
      Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
      ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
      domain.add(definition(
              primaryKeyProperty(entityType.attribute("id", Integer.class)),
              columnProperty(fkId),
              Property.foreignKeyProperty(foreignKey, "caption")));
    });
  }

  @Test
  void foreignKeyReferencingUndefinedEntityNonStrict() {
    domain.setStrictForeignKeys(false);
    EntityType entityType = DOMAIN.entityType("test.entity");
    Attribute<Integer> fkId = entityType.attribute("fk_id", Integer.class);
    EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    Attribute<Integer> refId = referencedEntityType.attribute("id", Integer.class);
    ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
    domain.add(definition(
            primaryKeyProperty(entityType.attribute("id", Integer.class)),
            columnProperty(fkId),
            Property.foreignKeyProperty(foreignKey, "caption")));
    domain.setStrictForeignKeys(true);
  }

  @Test
  void defineProcedureExisting() {
    ProcedureType<DatabaseConnection, Object> procedureType = ProcedureType.procedureType("operationId");
    DatabaseProcedure<DatabaseConnection, Object> operation = (databaseConnection, arguments) -> {};
    domain.add(procedureType, operation);
    assertThrows(IllegalArgumentException.class, () -> domain.add(procedureType, operation));
  }

  @Test
  void defineFunctionExisting() {
    FunctionType<DatabaseConnection, Object, Object> functionType = FunctionType.functionType("operationId");
    DatabaseFunction<DatabaseConnection, Object, Object> function = (databaseConnection, arguments) -> null;
    domain.add(functionType, function);
    assertThrows(IllegalArgumentException.class, () -> domain.add(functionType, function));
  }

  @Test
  void functionNonExisting() {
    FunctionType<?, ?, ?> functionType = FunctionType.functionType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.function(functionType));
  }

  @Test
  void frocedureNonExisting() {
    ProcedureType<?, ?> procedureType = ProcedureType.procedureType("nonexisting");
    assertThrows(IllegalArgumentException.class, () -> domain.procedure(procedureType));
  }

  @Test
  void conditionProvider() {
    EntityType nullConditionProvider1 = DOMAIN.entityType("nullConditionProvider1");
    assertThrows(NullPointerException.class, () -> domain.add(definition(primaryKeyProperty(nullConditionProvider1.integerAttribute("id")))
            .conditionProvider(null, (attributes, values) -> null)));
    EntityType nullConditionProvider2 = DOMAIN.entityType("nullConditionProvider2");
    assertThrows(NullPointerException.class, () -> domain.add(definition(primaryKeyProperty(nullConditionProvider2.integerAttribute("id")))
            .conditionProvider(nullConditionProvider2.conditionType("id"), null)));
    EntityType nullConditionProvider3 = DOMAIN.entityType("nullConditionProvider3");
    ConditionType nullConditionType = nullConditionProvider3.conditionType("id");
    assertThrows(IllegalStateException.class, () -> domain.add(definition(primaryKeyProperty(nullConditionProvider3.integerAttribute("id")))
            .conditionProvider(nullConditionType, (attributes, values) -> null)
            .conditionProvider(nullConditionType, (attributes, values) -> null)));
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
  void foreignKeyComparator() {
    EntityType ref = DOMAIN.entityType("fkCompRef");
    Attribute<Integer> id = ref.integerAttribute("id");

    EntityType entityType = DOMAIN.entityType("fkComp");
    Attribute<Integer> ref_id = entityType.integerAttribute("ref_id");
    ForeignKey foreignKey = entityType.foreignKey("fk", ref_id, id);
    assertThrows(UnsupportedOperationException.class, () -> Property.foreignKeyProperty(foreignKey).comparator((o1, o2) -> 0));
  }
}
