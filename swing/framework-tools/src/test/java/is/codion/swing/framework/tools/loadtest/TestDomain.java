/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;

import static is.codion.framework.domain.entity.EntityDefinition.definition;
import static is.codion.framework.domain.property.Property.columnProperty;
import static is.codion.framework.domain.property.Property.primaryKeyProperty;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
  }

  public static final EntityType T_DEPARTMENT = DOMAIN.entityType("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerAttribute("deptno");
  public static final Attribute<String> DEPARTMENT_NAME = T_DEPARTMENT.stringAttribute("dname");
  public static final Attribute<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringAttribute("loc");

  void department() {
    add(definition(
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.name())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.name())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.name())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringFactory(DEPARTMENT_NAME)
            .caption("Department"));
  }
}
