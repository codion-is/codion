/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.DatabaseFactory;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.condition.SelectCondition;
import is.codion.framework.db.local.TestDomain.Query;
import is.codion.framework.db.local.TestDomain.QueryFromClause;
import is.codion.framework.db.local.TestDomain.QueryWhereClause;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SelectQueriesTest {

  private final TestDomain testDomain = new TestDomain();
  private final SelectQueries queries = new SelectQueries(DatabaseFactory.getDatabase());

  @Test
  void builder() {
    SelectQueries.Builder builder = queries.builder(testDomain.getEntities().getDefinition(Query.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp\norder by ename", builder.build());

    builder.columns("empno");
    assertEquals("select empno\nfrom scott.emp\norder by ename", builder.build());

    builder = queries.builder(testDomain.getEntities().getDefinition(QueryWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp\nwhere deptno > 10\norder by ename desc", builder.build());

    builder.orderBy("ename");
    assertEquals("select empno, ename\nfrom scott.emp\nwhere deptno > 10\norder by ename", builder.build());

    builder = queries.builder(testDomain.getEntities().getDefinition(QueryFromClause.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp\norder by ename", builder.build());
  }

  @Test
  void selectCondition() {
    SelectQueries.Builder builder = queries.builder(testDomain.getEntities().getDefinition(QueryWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp\nwhere deptno > 10\norder by ename desc", builder.build());

    final SelectCondition condition = Conditions.where(QueryWhereClause.ENAME)
            .equalTo("SCOTT")
            .toSelectCondition()
            .selectAttributes(QueryWhereClause.ENAME)
            .orderBy(OrderBy.orderBy()
                    .descending(QueryWhereClause.EMPNO));
    builder = queries.builder(testDomain.getEntities().getDefinition(QueryWhereClause.TYPE))
            .selectCondition(condition);

    assertEquals("select ename\nfrom scott.emp\nwhere deptno > 10\nand ename = ?\norder by empno desc", builder.build());
  }
}
