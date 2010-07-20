/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.sql.Types;

public class EntityCriteriaTest {

  @Test
  public void test() {
    final CriteriaSet<Property.ColumnProperty> set1 = new CriteriaSet<Property.ColumnProperty>(
            CriteriaSet.Conjunction.AND,
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("stringProperty", Types.VARCHAR), SearchType.LIKE, "value"),
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("intProperty", Types.INTEGER), SearchType.LIKE, 666)
    );
    final EntityCriteria criteria = EntityCriteriaUtil.criteria("entityID", set1);
    assertEquals("where (stringProperty like ? and intProperty = ?)", criteria.getWhereClause());
    assertEquals(set1, criteria.getCriteria());
    final CriteriaSet<Property.ColumnProperty> set2 = new CriteriaSet<Property.ColumnProperty>(
            CriteriaSet.Conjunction.AND,
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("doubleProperty", Types.DOUBLE), SearchType.LIKE, 666.666),
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("stringProperty2", Types.VARCHAR), false, SearchType.LIKE, "value2")
    );
    final CriteriaSet<Property.ColumnProperty> set3 = new CriteriaSet<Property.ColumnProperty>(CriteriaSet.Conjunction.OR, set1, set2);
    assertEquals("where ((stringProperty like ? and intProperty = ?) " + "or"
            + " (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            EntityCriteriaUtil.criteria("entityID", set3).getWhereClause());
  }
}
