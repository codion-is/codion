/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.Criteria;
import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.dbms.Database;
import org.jminor.common.db.dbms.DatabaseProvider;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import org.junit.Test;

import java.sql.Types;

public class EntityCriteriaTest {

  private static final Criteria.ValueProvider VALUE_PROVIDER = EntityCriteriaUtil.getCriteriaValueProvider();

  @Test
  public void test() {
    try {
      new EntityCriteria(null);
      fail();
    }
    catch (IllegalArgumentException e) {}
    final Database database = DatabaseProvider.createInstance();
    final CriteriaSet set1 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("stringProperty", Types.VARCHAR), SearchType.LIKE, "value"),
            new PropertyCriteria(new Property("intProperty", Types.INTEGER), SearchType.LIKE, 666)
    );
    final EntityCriteria criteria = new EntityCriteria("entityID", set1);
    assertEquals("where (stringProperty = 'value' and intProperty = 666)", criteria.getWhereClause(database, VALUE_PROVIDER));
    assertEquals(set1, criteria.getCriteria());
    final CriteriaSet set2 = new CriteriaSet(
            CriteriaSet.Conjunction.AND,
            new PropertyCriteria(new Property("doubleProperty", Types.DOUBLE), SearchType.LIKE, 666.666),
            new PropertyCriteria(new Property("stringProperty2", Types.VARCHAR), SearchType.LIKE, "value2").setCaseSensitive(false)
    );
    final CriteriaSet set3 = new CriteriaSet(CriteriaSet.Conjunction.OR, set1, set2);
    assertEquals("where ((stringProperty = 'value' and intProperty = 666) " + "or"
            + " (doubleProperty = 666.666 and upper(stringProperty2) = upper('value2')))",
            new EntityCriteria("entityID", set3).getWhereClause(database, VALUE_PROVIDER));
  }
}
