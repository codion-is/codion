/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Column;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.*;

public final class TestKeysDomain extends DefaultDomain {

  private static final DomainType DOMAIN = DomainType.domainType(TestKeysDomain.class);

  TestKeysDomain() {
    super(DOMAIN);
  }

  public interface TestPrimaryKey {
    EntityType TYPE = DOMAIN.entityType("TestPrimaryKey");

    Column<Integer> ID1 = TYPE.integerColumn("id1");
    Column<Integer> ID2 = TYPE.integerColumn("id2");
    Column<Integer> ID3 = TYPE.integerColumn("id3");
  }

  public void testPrimaryKeyIndexes1() {
    add(definition(
            columnProperty(TestPrimaryKey.ID1)
                    .primaryKeyIndex(0),
            columnProperty(TestPrimaryKey.ID2)
                    .primaryKeyIndex(1),
            columnProperty(TestPrimaryKey.ID3)
                    .primaryKeyIndex(3)));
  }

  public void testPrimaryKeyIndexes2() {
    add(definition(
            columnProperty(TestPrimaryKey.ID1)
                    .primaryKeyIndex(1),
            columnProperty(TestPrimaryKey.ID2)
                    .primaryKeyIndex(1),
            columnProperty(TestPrimaryKey.ID3)
                    .primaryKeyIndex(2)));
  }

  public void testPrimaryKeyIndexes3() {
    add(definition(
            columnProperty(TestPrimaryKey.ID1)
                    .primaryKeyIndex(-1)));
  }

  public void testPrimaryKeyIndexes4() {
    add(definition(
            columnProperty(TestPrimaryKey.ID1)
                    .primaryKeyIndex(10)));
  }

  public interface TestFkMaster {
    EntityType TYPE = DOMAIN.entityType("TestFKMaster");

    Column<Integer> ID1 = TYPE.integerColumn("id1");
    Column<Integer> ID2 = TYPE.integerColumn("id2");
  }

  public interface TestFkDetail {
    EntityType TYPE = DOMAIN.entityType("TestFKMaster");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<Integer> MASTER_ID1 = TYPE.integerColumn("master_id1");
    Column<Integer> MASTER_ID2 = TYPE.integerColumn("master_id2");
    ForeignKey MASTER_FK = TYPE.foreignKey("master",
            MASTER_ID1, TestFkMaster.ID1,
            MASTER_ID2, TestFkMaster.ID2);
  }

  public void testForeignKeys() {
    add(definition(
            columnProperty(TestFkMaster.ID1)
                    .primaryKeyIndex(0)//,
            //here's what we're testing for, a missing fk reference property
//            columnProperty(TestFKMaster.ID1)
//                    .primaryKeyIndex(1)
    ));
    add(definition(
            primaryKeyProperty(TestFkDetail.ID),
            columnProperty(TestFkDetail.MASTER_ID1),
            columnProperty(TestFkDetail.MASTER_ID2),
            foreignKeyProperty(TestFkDetail.MASTER_FK)));
  }
}
