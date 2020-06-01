/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.loadtest;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.Attributes;
import is.codion.framework.domain.property.Identity;

import static is.codion.framework.domain.property.Properties.columnProperty;
import static is.codion.framework.domain.property.Properties.primaryKeyProperty;

public final class TestDomain extends Domain {

  public TestDomain() {
    department();
    registerEntities();
  }

  public static final Identity T_DEPARTMENT = Identity.identity("scott.dept");
  public static final Attribute<Integer> DEPARTMENT_ID = Attributes.integerAttribute("deptno", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_NAME = Attributes.stringAttribute("dname", T_DEPARTMENT);
  public static final Attribute<String> DEPARTMENT_LOCATION = Attributes.stringAttribute("loc", T_DEPARTMENT);

  void department() {
    define(T_DEPARTMENT,
            primaryKeyProperty(DEPARTMENT_ID, DEPARTMENT_ID.getName())
                    .updatable(true).nullable(false),
            columnProperty(DEPARTMENT_NAME, DEPARTMENT_NAME.getName())
                    .searchProperty(true).preferredColumnWidth(120).maximumLength(14).nullable(false),
            columnProperty(DEPARTMENT_LOCATION, DEPARTMENT_LOCATION.getName())
                    .preferredColumnWidth(150).maximumLength(13))
            .smallDataset(true)
            .stringProvider(new StringProvider(DEPARTMENT_NAME))
            .caption("Department");
  }
}
