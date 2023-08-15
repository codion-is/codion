/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.common.db.connection.DatabaseConnection;
import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.operation.DatabaseProcedure;
import is.codion.common.db.operation.FunctionType;
import is.codion.common.db.operation.ProcedureType;
import is.codion.framework.domain.entity.ConditionType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.junit.jupiter.api.Test;

import static is.codion.framework.domain.TestDomain.DOMAIN;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DomainTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void keyWithSameIndex() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("keyWithSameIndex");
      domain.add(entityType.define(
              entityType.column("1", Integer.class).primaryKey().primaryKeyIndex(0),
              entityType.column("2", Integer.class).primaryKey().primaryKeyIndex(1),
              entityType.column("3", Integer.class).primaryKey().primaryKeyIndex(1)));
    });
  }

  @Test
  void keyWithSameIndex2() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("keyWithSameIndex2");
      domain.add(entityType.define(
              entityType.column("1", Integer.class).primaryKey(),
              entityType.column("2", Integer.class).primaryKey(),
              entityType.column("3", Integer.class).primaryKey()));
    });
  }

  @Test
  void redefine() {
    EntityType entityType = DOMAIN.entityType("redefine");
    Column<Integer> column = entityType.integerColumn("column");
    domain.add(entityType.define(column.primaryKey()));
    assertThrows(IllegalArgumentException.class, () -> domain.add(entityType.define(column.primaryKey())));
  }

  @Test
  void foreignKeyReferencingUndefinedColumn() {
    EntityType entityType = DOMAIN.entityType("test.entity");
    Column<Integer> fkId = entityType.column("fk_id", Integer.class);
    EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    Column<Integer> refId = referencedEntityType.column("id", Integer.class);
    Column<Integer> refIdNotPartOfEntity = referencedEntityType.column("id_none", Integer.class);
    ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refIdNotPartOfEntity);
    domain.add(referencedEntityType.define(refId.primaryKey()));

    assertThrows(IllegalArgumentException.class, () -> domain.add(entityType.define(
            entityType.column("id", Integer.class).primaryKey(),
            fkId.column(),
            foreignKey.foreignKey()
                    .caption("caption"))));
  }

  @Test
  void foreignKeyReferencingUndefinedEntity() {
    assertThrows(IllegalArgumentException.class, () -> {
      EntityType entityType = DOMAIN.entityType("test.entity");
      Column<Integer> fkId = entityType.column("fk_id", Integer.class);
      EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
      Column<Integer> refId = referencedEntityType.column("id", Integer.class);
      ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
      domain.add(entityType.define(
              entityType.column("id", Integer.class).primaryKey(),
              fkId.column(),
              foreignKey.foreignKey()
                    .caption("caption")));
    });
  }

  @Test
  void foreignKeyReferencingUndefinedEntityNonStrict() {
    domain.setStrictForeignKeys(false);
    EntityType entityType = DOMAIN.entityType("test.entity");
    Column<Integer> fkId = entityType.column("fk_id", Integer.class);
    EntityType referencedEntityType = DOMAIN.entityType("test.referenced_entity");
    Column<Integer> refId = referencedEntityType.column("id", Integer.class);
    ForeignKey foreignKey = entityType.foreignKey("fk_id_fk", fkId, refId);
    domain.add(entityType.define(
            entityType.column("id", Integer.class).primaryKey(),
            fkId.column(),
            foreignKey.foreignKey()
                    .caption("caption")));
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
    assertThrows(NullPointerException.class, () -> domain.add(nullConditionProvider1.define(nullConditionProvider1.integerColumn("id").primaryKey())
            .conditionProvider(null, (columns, values) -> null)));
    EntityType nullConditionProvider2 = DOMAIN.entityType("nullConditionProvider2");
    assertThrows(NullPointerException.class, () -> domain.add(nullConditionProvider2.define(nullConditionProvider2.integerColumn("id").primaryKey())
            .conditionProvider(nullConditionProvider2.conditionType("id"), null)));
    EntityType nullConditionProvider3 = DOMAIN.entityType("nullConditionProvider3");
    ConditionType nullConditionType = nullConditionProvider3.conditionType("id");
    assertThrows(IllegalStateException.class, () -> domain.add(nullConditionProvider3.define(nullConditionProvider3.integerColumn("id").primaryKey())
            .conditionProvider(nullConditionType, (columns, values) -> null)
            .conditionProvider(nullConditionType, (columns, values) -> null)));
  }

  @Test
  void missingForeignKeyReference() {
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testForeignKeys());
  }

  @Test
  void misconfiguredPrimaryKeyColumnIndexes() {
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes1());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes2());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes3());
    assertThrows(IllegalArgumentException.class, () -> new TestKeysDomain().testPrimaryKeyIndexes4());
  }

  @Test
  void foreignKeyComparator() {
    EntityType ref = DOMAIN.entityType("fkCompRef");
    Column<Integer> id = ref.integerColumn("id");

    EntityType entityType = DOMAIN.entityType("fkComp");
    Column<Integer> ref_id = entityType.integerColumn("ref_id");
    ForeignKey foreignKey = entityType.foreignKey("fk", ref_id, id);
    assertThrows(UnsupportedOperationException.class, () -> foreignKey.foreignKey().comparator((o1, o2) -> 0));
  }
}
