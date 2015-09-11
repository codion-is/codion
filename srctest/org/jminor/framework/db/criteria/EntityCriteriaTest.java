/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.criteria;

import org.jminor.common.db.criteria.CriteriaSet;
import org.jminor.common.db.criteria.CriteriaUtil;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.SearchType;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class EntityCriteriaTest {

  @Test
  public void test() {
    final CriteriaSet<Property.ColumnProperty> set1 = CriteriaUtil.criteriaSet(
            Conjunction.AND,
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("stringProperty", Types.VARCHAR), SearchType.LIKE, "value"),
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("intProperty", Types.INTEGER), SearchType.LIKE, 666)
    );
    final EntityCriteria criteria = EntityCriteriaUtil.criteria("entityID", set1);
    assertEquals("(stringProperty like ? and intProperty = ?)", criteria.getWhereClause());
    assertEquals(set1, criteria.getCriteria());
    final CriteriaSet<Property.ColumnProperty> set2 = CriteriaUtil.criteriaSet(
            Conjunction.AND,
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("doubleProperty", Types.DOUBLE), SearchType.LIKE, 666.666),
            EntityCriteriaUtil.propertyCriteria(Properties.columnProperty("stringProperty2", Types.VARCHAR), SearchType.LIKE, false, "value2")
    );
    final CriteriaSet<Property.ColumnProperty> set3 = CriteriaUtil.criteriaSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty like ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            EntityCriteriaUtil.criteria("entityID", set3).getWhereClause());
  }
}
