/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.CriteriaSet;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class EntityCriteriaTest {

  @Test
  public void test() {
    final Database database = DatabaseProvider.createInstance();
    final CriteriaSet set1 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("stringProperty", Type.STRING), SearchType.LIKE, "value"),
            new PropertyCriteria(new Property("intProperty", Type.INT), SearchType.LIKE, 666)
    );
    assertEquals("where (stringProperty = 'value' and intProperty = 666)",
            new EntityCriteria("entityID", set1).getWhereClause(database));
    final CriteriaSet set2 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("doubleProperty", Type.DOUBLE), SearchType.LIKE, 666.666),
            new PropertyCriteria(new Property("stringProperty2", Type.STRING), SearchType.LIKE, "value2").setCaseSensitive(false)
    );
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set1, set2);
    assertEquals("where ((stringProperty = 'value' and intProperty = 666) " + "or"
            + " (doubleProperty = 666.666 and upper(stringProperty2) = upper('value2')))",
            new EntityCriteria("entityID", set3).getWhereClause(database));
  }
}
