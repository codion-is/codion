/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.property.Properties.columnProperty;

public class TestDomainExtended extends TestDomain {

  public static final DomainType DOMAIN = TestDomain.DOMAIN.extend(TestDomainExtended.class);

  public static final EntityType<Entity> T_EXTENDED = DOMAIN.entityType("extended.entity");
  public static final Attribute<Integer> EXTENDED_ID = T_EXTENDED.integerAttribute("id");
  public static final Attribute<String> EXTENDED_NAME = T_EXTENDED.stringAttribute("name");

  public TestDomainExtended() {
    this(DOMAIN);
  }

  public TestDomainExtended(final DomainType domainType) {
    super(domainType);
    extended();
  }

  final void extended() {
    define(T_EXTENDED,
            columnProperty(EXTENDED_ID).primaryKeyIndex(0),
            columnProperty(EXTENDED_NAME));
  }

  public static final class TestDomainSecondExtenion extends TestDomainExtended {

    public static final DomainType DOMAIN = TestDomainExtended.DOMAIN.extend(TestDomainSecondExtenion.class);

    public static final EntityType<Entity> T_SECOND_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Attribute<Integer> EXTENDED_ID = T_SECOND_EXTENDED.integerAttribute("id");
    public static final Attribute<String> EXTENDED_NAME = T_SECOND_EXTENDED.stringAttribute("name");

    public TestDomainSecondExtenion() {
      super(DOMAIN);
      extendedSecond();
    }

    final void extendedSecond() {
      define(T_SECOND_EXTENDED,
              columnProperty(EXTENDED_ID).primaryKeyIndex(0),
              columnProperty(EXTENDED_NAME));
    }
  }
}
