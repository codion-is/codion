/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 *
 */
package org.jminor.framework.model;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.model.SearchType;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestEntityCriteria extends TestCase {

  public TestEntityCriteria(String name) {
    super(name);
  }

  public void test() {
    final CriteriaSet set1 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("stringProperty", Type.STRING), SearchType.LIKE, "value"),
            new PropertyCriteria(new Property("intProperty", Type.INT), SearchType.LIKE, 666)
    );
    Assert.assertEquals("where (stringProperty = 'value' and intProperty = 666)", new EntityCriteria(null, set1).getWhereClause());
    final CriteriaSet set2 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("doubleProperty", Type.DOUBLE), SearchType.LIKE, 666.666),
            new PropertyCriteria(new Property("stringProperty2", Type.STRING), SearchType.LIKE, "value2").setCaseSensitive(false)
    );
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set1, set2);
    assertEquals("where ((stringProperty = 'value' and intProperty = 666) " + "or"
            + " (doubleProperty = 666.666 and upper(stringProperty2) = upper('value2')))",
            new EntityCriteria(null, set3).getWhereClause());
  }
}
