/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain;

import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;

import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.foreignKeyProperty;

public final class TestDomainExtended extends DefaultDomain {

  public static final DomainType DOMAIN = DomainType.domainType(TestDomainExtended.class);

  public static final EntityType<Entity> T_EXTENDED = DOMAIN.entityType("extended.entity");
  public static final Attribute<Integer> EXTENDED_ID = T_EXTENDED.integerAttribute("id");
  public static final Attribute<String> EXTENDED_NAME = T_EXTENDED.stringAttribute("name");
  public static final Attribute<Integer> EXTENDED_DEPT_ID = T_EXTENDED.integerAttribute("dept_id");
  public static final ForeignKey EXTENDED_DEPT_FK = T_EXTENDED.foreignKey("dept_fk", EXTENDED_DEPT_ID, TestDomain.Department.NO);

  public TestDomainExtended() {
    this(DOMAIN);
  }

  public TestDomainExtended(final DomainType domainType) {
    super(domainType);
    addEntities(new TestDomain());
    extended();
  }

  final void extended() {
    define(T_EXTENDED,
            columnProperty(EXTENDED_ID).primaryKeyIndex(0),
            columnProperty(EXTENDED_NAME),
            columnProperty(EXTENDED_DEPT_ID),
            foreignKeyProperty(EXTENDED_DEPT_FK));
  }

  public static final class TestDomainSecondExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainSecondExtension.class);

    public static final EntityType<Entity> T_SECOND_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Attribute<Integer> EXTENDED_ID = T_SECOND_EXTENDED.integerAttribute("id");
    public static final Attribute<String> EXTENDED_NAME = T_SECOND_EXTENDED.stringAttribute("name");

    public TestDomainSecondExtension() {
      super(DOMAIN);
      addEntities(new TestDomainExtended());
      extendedSecond();
    }

    final void extendedSecond() {
      define(T_SECOND_EXTENDED,
              columnProperty(EXTENDED_ID).primaryKeyIndex(0),
              columnProperty(EXTENDED_NAME));
    }
  }

  public static final class TestDomainThirdExtension extends DefaultDomain {

    public static final DomainType DOMAIN = DomainType.domainType(TestDomainThirdExtension.class);

    public static final EntityType<Entity> T_THIRD_EXTENDED = DOMAIN.entityType("extended.second_entity");
    public static final Attribute<Integer> EXTENDED_ID = T_THIRD_EXTENDED.integerAttribute("id");
    public static final Attribute<String> EXTENDED_NAME = T_THIRD_EXTENDED.stringAttribute("name");

    public TestDomainThirdExtension() {
      super(DOMAIN);
      addEntities(new TestDomainSecondExtension());
      extendedThird();
    }

    final void extendedThird() {
      define(T_THIRD_EXTENDED,
              columnProperty(EXTENDED_ID).primaryKeyIndex(0),
              columnProperty(EXTENDED_NAME));
    }
  }
}
