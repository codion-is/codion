/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

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
    add(TestPrimaryKey.TYPE.define(
            TestPrimaryKey.ID1.define().primaryKey(0),
            TestPrimaryKey.ID2.define().primaryKey(1),
            TestPrimaryKey.ID3.define().primaryKey(3)));
  }

  public void testPrimaryKeyIndexes2() {
    add(TestPrimaryKey.TYPE.define(
            TestPrimaryKey.ID1.define().primaryKey(1),
            TestPrimaryKey.ID2.define().primaryKey(1),
            TestPrimaryKey.ID3.define().primaryKey(2)));
  }

  public void testPrimaryKeyIndexes3() {
    add(TestPrimaryKey.TYPE.define(
            TestPrimaryKey.ID1.define().primaryKey(-1)));
  }

  public void testPrimaryKeyIndexes4() {
    add(TestPrimaryKey.TYPE.define(
            TestPrimaryKey.ID1.define().primaryKey(10)));
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
    add(TestFkMaster.TYPE.define(
            TestFkMaster.ID1.define()
                    .primaryKey()//,
            //here's what we're testing for, a missing fk reference property
//            TestFKMaster.ID1.define().column()
//                    .primaryKeyIndex(1)
    ));
    add(TestFkMaster.TYPE.define(
            TestFkDetail.ID.define().primaryKey(),
            TestFkDetail.MASTER_ID1.define().column(),
            TestFkDetail.MASTER_ID2.define().column(),
            TestFkDetail.MASTER_FK.define()
                    .foreignKey()));
  }
}
