/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.local.TestDomain.Query;
import is.codion.framework.db.local.TestDomain.QueryColumnsWhereClause;
import is.codion.framework.db.local.TestDomain.QueryFromClause;
import is.codion.framework.domain.entity.OrderBy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class SelectQueriesTest {

  private final TestDomain testDomain = new TestDomain();
  private final SelectQueries queries = new SelectQueries(Database.instance());

  @Test
  void builder() {
    SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(Query.TYPE))
            .entitySelectQuery();
    assertEquals("SELECT empno, ename\nFROM scott.emp e\nORDER BY ename", builder.build());

    builder.columns("empno");
    assertEquals("SELECT empno\nFROM scott.emp e\nORDER BY ename", builder.build());

    builder.forUpdate(true);
    assertEquals("SELECT empno\nFROM scott.emp\nORDER BY ename\nFOR UPDATE NOWAIT", builder.build());

    builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("SELECT e.empno, e.ename\nFROM scott.emp e\nWHERE e.deptno > 10", builder.build());

    builder.orderBy("ename");
    assertEquals("SELECT e.empno, e.ename\nFROM scott.emp e\nWHERE e.deptno > 10\nORDER BY ename", builder.build());

    builder = queries.builder(testDomain.entities().definition(QueryFromClause.TYPE))
            .entitySelectQuery();
    assertEquals("SELECT empno, ename\nFROM scott.emp\nORDER BY ename", builder.build());
  }

  @Test
  void select() {
    SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .entitySelectQuery();
    assertEquals("SELECT e.empno, e.ename\nFROM scott.emp e\nWHERE e.deptno > 10", builder.build());

    Select select = Select.where(QueryColumnsWhereClause.ENAME.equalTo("SCOTT"))
            .attributes(QueryColumnsWhereClause.ENAME)
            .having(QueryColumnsWhereClause.EMPNO.equalTo(4))
            .orderBy(OrderBy.descending(QueryColumnsWhereClause.EMPNO))
            .build();
    builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
            .select(select);

    //select should not affect columns when the columns are hardcoded in the entity query
    assertEquals("SELECT e.empno, e.ename\nFROM scott.emp e\nWHERE e.deptno > 10\nAND ename = ?\nHAVING empno = ?\nORDER BY empno DESC", builder.build());
  }
}
