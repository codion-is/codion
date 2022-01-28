package is.codion.framework.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import static is.codion.framework.domain.property.Properties.*;

public final class TestFkDomain extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestFkDomain.class);

  public TestFkDomain() {
    super(DOMAIN);
    testFkMaster();
    testFkDetail();
  }

  public interface TestFkMaster {
    EntityType TYPE = DOMAIN.entityType("TestFKMaster");

    Attribute<Integer> ID1 = TYPE.integerAttribute("id1");
    Attribute<Integer> ID2 = TYPE.integerAttribute("id2");
  }

  void testFkMaster() {
    define(TestFkMaster.TYPE,
            columnProperty(TestFkMaster.ID1)
                    .primaryKeyIndex(0)//,
            //here's what we're testing for, a missing fk reference property
//            columnProperty(TestFKMaster.ID1)
//                    .primaryKeyIndex(1)
    );
  }

  public interface TestFkDetail {
    EntityType TYPE = DOMAIN.entityType("TestFKMaster");

    Attribute<Integer> ID = TYPE.integerAttribute("id");
    Attribute<Integer> MASTER_ID1 = TYPE.integerAttribute("master_id1");
    Attribute<Integer> MASTER_ID2 = TYPE.integerAttribute("master_id2");
    ForeignKey MASTER_FK = TYPE.foreignKey("master",
            MASTER_ID1, TestFkMaster.ID1,
            MASTER_ID2, TestFkMaster.ID2);
  }

  void testFkDetail() {
    define(TestFkDetail.TYPE,
            primaryKeyProperty(TestFkDetail.ID),
            columnProperty(TestFkDetail.MASTER_ID1),
            columnProperty(TestFkDetail.MASTER_ID2),
            foreignKeyProperty(TestFkDetail.MASTER_FK));
  }
}
