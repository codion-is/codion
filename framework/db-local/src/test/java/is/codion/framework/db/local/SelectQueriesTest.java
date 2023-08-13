/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.Select;
import is.codion.framework.db.local.TestDomain.Query;
import is.codion.framework.db.local.TestDomain.QueryColumnsWhereClause;
import is.codion.framework.db.local.TestDomain.QueryFromClause;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static is.codion.framework.db.condition.Condition.column;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SelectQueriesTest {

  private final TestDomain testDomain = new TestDomain();
  private final SelectQueries queries = new SelectQueries(Database.instance());

  @Test
  void builder() {
    SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(Query.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp e\norder by ename", builder.build());

    builder.columns("empno");
    assertEquals("select empno\nfrom scott.emp e\norder by ename", builder.build());

    builder.forUpdate(true);
    assertEquals("select empno\nfrom scott.emp\norder by ename\nfor update nowait", builder.build());

    builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("select e.empno, e.ename\nfrom scott.emp e\nwhere e.deptno > 10", builder.build());

    builder.orderBy("ename");
    assertEquals("select e.empno, e.ename\nfrom scott.emp e\nwhere e.deptno > 10\norder by ename", builder.build());

    builder = queries.builder(testDomain.entities().definition(QueryFromClause.TYPE))
            .entitySelectQuery();
    assertEquals("select empno, ename\nfrom scott.emp\norder by ename", builder.build());
  }

  @Test
  void selectCondition() {
    SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("select e.empno, e.ename\nfrom scott.emp e\nwhere e.deptno > 10", builder.build());

    Select select = Select.where(column(QueryColumnsWhereClause.ENAME).equalTo("SCOTT"))
            .attributes(QueryColumnsWhereClause.ENAME)
            .orderBy(OrderBy.descending(QueryColumnsWhereClause.EMPNO))
            .build();
    builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .selectCondition(select);

    //select should not affect columns when the columns are hardcoded in the entity query
    assertEquals("select e.empno, e.ename\nfrom scott.emp e\nwhere e.deptno > 10\nand ename = ?\norder by empno desc", builder.build());
  }
}
