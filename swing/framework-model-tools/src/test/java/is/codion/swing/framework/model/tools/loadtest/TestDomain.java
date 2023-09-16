/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.model.tools.loadtest;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

public final class TestDomain extends DefaultDomain {

  static final DomainType DOMAIN = DomainType.domainType(TestDomain.class);

  public TestDomain() {
    super(DOMAIN);
    department();
  }

  public static final EntityType T_DEPARTMENT = DOMAIN.entityType("scott.dept");
  public static final Column<Integer> DEPARTMENT_ID = T_DEPARTMENT.integerColumn("deptno");
  public static final Column<String> DEPARTMENT_NAME = T_DEPARTMENT.stringColumn("dname");
  public static final Column<String> DEPARTMENT_LOCATION = T_DEPARTMENT.stringColumn("loc");

  void department() {
    add(T_DEPARTMENT.define(
            DEPARTMENT_ID.define()
                    .primaryKey()
                    .caption(DEPARTMENT_ID.name())
                    .updatable(true).nullable(false),
            DEPARTMENT_NAME.define()
                    .column()
                    .caption(DEPARTMENT_NAME.name())
                    .searchColumn(true)
                    .maximumLength(14)
                    .nullable(false),
            DEPARTMENT_LOCATION.define()
                    .column()
                    .caption(DEPARTMENT_LOCATION.name())
                    .maximumLength(13))
            .smallDataset(true)
            .stringFactory(DEPARTMENT_NAME)
            .caption("Department"));
  }
}
