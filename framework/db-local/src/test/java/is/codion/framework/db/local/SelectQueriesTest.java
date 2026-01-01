/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.local;

import is.codion.common.db.database.Database;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.local.TestDomain.Employee;
import is.codion.framework.db.local.TestDomain.EmpnoDeptno;
import is.codion.framework.db.local.TestDomain.Job;
import is.codion.framework.db.local.TestDomain.Master;
import is.codion.framework.db.local.TestDomain.NoPrimaryKey;
import is.codion.framework.db.local.TestDomain.Query;
import is.codion.framework.db.local.TestDomain.QueryColumnsWhereClause;
import is.codion.framework.db.local.TestDomain.QueryFromClause;
import is.codion.framework.db.local.TestDomain.QueryWithCte;
import is.codion.framework.db.local.TestDomain.QueryWithMultipleCtes;
import is.codion.framework.db.local.TestDomain.QueryWithRecursiveCte;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.ColumnDefinition;
import is.codion.framework.domain.entity.condition.Condition;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.domain.entity.condition.Condition.or;
import static org.junit.jupiter.api.Assertions.*;

public final class SelectQueriesTest {

	private TestDomain testDomain;
	private SelectQueries queries;
	private EntityDefinition employeeDefinition;
	private EntityDefinition jobDefinition;

	@BeforeEach
	void setUp() {
		testDomain = new TestDomain();
		queries = new SelectQueries(Database.instance());
		employeeDefinition = testDomain.entities().definition(Employee.TYPE);
		jobDefinition = testDomain.entities().definition(Job.TYPE);
	}

	@Test
	void builder() {
		SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(Query.TYPE))
						.entitySelectQuery();
		assertEquals("SELECT empno, ename\nFROM employees.employee e\nORDER BY ename", builder.build());

		builder.columns("empno");
		assertEquals("SELECT empno\nFROM employees.employee e\nORDER BY ename", builder.build());

		builder.forUpdate(true);
		assertEquals("SELECT empno\nFROM employees.employee\nORDER BY ename\nFOR UPDATE NOWAIT", builder.build());

		builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
						.entitySelectQuery();
		assertEquals("SELECT e.empno, e.ename\nFROM employees.employee e\nWHERE e.deptno > 10", builder.build());

		builder.orderBy("ename");
		assertEquals("SELECT e.empno, e.ename\nFROM employees.employee e\nWHERE e.deptno > 10\nORDER BY ename", builder.build());

		builder = queries.builder(testDomain.entities().definition(QueryFromClause.TYPE))
						.entitySelectQuery();
		assertEquals("SELECT empno, ename\nFROM employees.employee\nORDER BY ename", builder.build());
	}

	@Test
	void select() {
		SelectQueries.Builder builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
						.entitySelectQuery();
		assertEquals("SELECT e.empno, e.ename\nFROM employees.employee e\nWHERE e.deptno > 10", builder.build());

		Select select = Select.where(QueryColumnsWhereClause.ENAME.equalTo("SCOTT"))
						.attributes(QueryColumnsWhereClause.ENAME)
						.having(QueryColumnsWhereClause.EMPNO.equalTo(4))
						.orderBy(OrderBy.builder()
										.descending(QueryColumnsWhereClause.EMPNO)
										.ascendingIgnoreCase(QueryColumnsWhereClause.ENAME)
										.build())
						.build();
		builder = queries.builder(testDomain.entities().definition(QueryColumnsWhereClause.TYPE))
						.select(select);

		//select should not affect columns when the columns are hardcoded in the entity query
		assertEquals("SELECT e.empno, e.ename\nFROM employees.employee e\nWHERE e.deptno > 10\nAND ename = ?\nHAVING empno = ?\nORDER BY empno DESC, UPPER(ename)", builder.build());
	}

	@Test
	void testBasicSelectQuery() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.all(Employee.TYPE).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("FROM employees.employee"));

		// Check that all selected columns are included
		List<ColumnDefinition<?>> selectedColumns = builder.selectedColumns();
		assertFalse(selectedColumns.isEmpty());
	}

	@Test
	void testSelectWithSpecificAttributes() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.where(Employee.DEPARTMENT.equalTo(10))
						.attributes(Employee.NAME, Employee.SALARY)
						.build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("empno")); // Primary key always included
		assertTrue(query.contains("ename"));
		assertTrue(query.contains("sal"));
		assertTrue(query.contains("WHERE deptno = ?"));
	}

	@Test
	void testSelectWithForeignKeyAttribute() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.where(Employee.DEPARTMENT.equalTo(10))
						.attributes(Employee.DEPARTMENT_FK)
						.build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("deptno")); // Foreign key column should be included

		List<ColumnDefinition<?>> selectedColumns = builder.selectedColumns();
		assertTrue(selectedColumns.stream().anyMatch(col -> col.attribute().equals(Employee.DEPARTMENT)));
	}

	@Test
	void testComplexWhereConditions() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Condition complexCondition = and(
						Employee.DEPARTMENT.equalTo(10),
						or(
										Employee.SALARY.greaterThan(1000.0),
										Employee.JOB.equalTo("MANAGER")
						)
		);
		Select select = Select.where(complexCondition).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("WHERE"));
		assertTrue(query.contains("deptno = ?"));
		assertTrue(query.contains("sal > ?"));
		assertTrue(query.contains("job = ?"));
	}

	@Test
	void testOrderByClause() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		OrderBy orderBy = OrderBy.builder()
						.ascending(Employee.DEPARTMENT)
						.descending(Employee.SALARY)
						.ascendingIgnoreCase(Employee.NAME)
						.build();

		Select select = Select.all(Employee.TYPE).orderBy(orderBy).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("ORDER BY"));
		assertTrue(query.contains("deptno"));
		assertTrue(query.contains("sal DESC"));
		assertTrue(query.contains("UPPER(ename)"));
	}

	@Test
	void testOrderByWithNullOrdering() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		OrderBy orderBy = OrderBy.builder()
						.ascending(OrderBy.NullOrder.NULLS_FIRST, Employee.COMMISSION)
						.descending(OrderBy.NullOrder.NULLS_LAST, Employee.SALARY)
						.build();

		Select select = Select.all(Employee.TYPE).orderBy(orderBy).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("comm NULLS FIRST"));
		assertTrue(query.contains("sal DESC NULLS LAST"));
	}

	@Test
	void testLimitOffset() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.all(Employee.TYPE)
						.limit(10)
						.offset(20)
						.build();
		builder.select(select);

		String query = builder.build();
		// Different databases have different syntax
		assertTrue(query.contains("10") || query.contains("20"));
	}

	@Test
	void testForUpdate() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.all(Employee.TYPE).forUpdate().build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("FOR UPDATE"));
		// forUpdate should use the regular table name, not the select table name
		assertTrue(query.contains("FROM employees.employee"));
		assertFalse(query.contains("FROM employees.employee e"));
	}

	@Test
	void testGroupByAndHaving() {
		SelectQueries.Builder builder = queries.builder(jobDefinition);
		Select select = Select.all(Job.TYPE)
						.having(Job.MAX_SALARY.greaterThan(5000.0))
						.build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("GROUP BY"));
		assertTrue(query.contains("HAVING"));
		// The Job entity has a having clause in its selectQuery
		assertTrue(query.contains("job <> 'PRESIDENT'"));
		assertTrue(query.contains("max(sal) > ?"));
	}

	@Test
	void testCountQuery() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Count count = Count.builder()
						.where(Employee.DEPARTMENT.equalTo(10))
						.having(Employee.COMMISSION.isNotNull())
						.build();
		builder.count(count);

		String query = builder.build();
		assertTrue(query.contains("COUNT(*)"));
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("WHERE"));
		assertTrue(query.contains("deptno = ?"));
	}

	@Test
	void testMultipleWhereConditions() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		builder.where("deptno = 10")
						.where("job = 'CLERK'")
						.where("sal > 1000");

		String query = builder.build();
		assertTrue(query.contains("WHERE deptno = 10"));
		assertTrue(query.contains("AND job = 'CLERK'"));
		assertTrue(query.contains("AND sal > 1000"));
	}

	@Test
	void testEntityWithCustomQuery() {
		EntityDefinition queryDef = testDomain.entities().definition(Query.TYPE);
		SelectQueries.Builder builder = queries.builder(queryDef);
		builder.entitySelectQuery();

		String query = builder.build();
		assertEquals("SELECT empno, ename\nFROM employees.employee e\nORDER BY ename", query);
	}

	@Test
	void testEntityWithAggregateColumns() {
		SelectQueries.Builder builder = queries.builder(jobDefinition);
		Select select = Select.all(Job.TYPE).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("job"));
		assertTrue(query.contains("max(sal)"));
		assertTrue(query.contains("min(sal)"));
		assertTrue(query.contains("max(comm)"));
		assertTrue(query.contains("min(comm)"));
		assertTrue(query.contains("GROUP BY job"));
	}


	@Test
	void testSelectWithoutUsingWhereClause() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.where(Employee.DEPARTMENT.equalTo(10)).build();

		// Use select but ignore where clause
		builder.select(select, false);

		String query = builder.build();
		assertFalse(query.contains("WHERE"));
	}

	@Test
	void testEntityWithJoinedQuery() {
		EntityDefinition empnoDeptno = testDomain.entities().definition(EmpnoDeptno.TYPE);
		SelectQueries.Builder builder = queries.builder(empnoDeptno);
		builder.entitySelectQuery();

		String query = builder.build();
		assertTrue(query.contains("FROM employees.employee e, employees.department d"));
		assertTrue(query.contains("WHERE e.deptno = d.deptno"));
		assertTrue(query.contains("ORDER BY e.deptno, e.ename"));
	}

	@Test
	void testCustomFromClause() {
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		builder.from("(SELECT * FROM employees.employee WHERE deptno = 10) emp");

		String query = builder.build();
		assertTrue(query.contains("FROM (SELECT * FROM employees.employee WHERE deptno = 10) emp"));
	}

	@Test
	void testNullAndEmptyStringHandling() {
		// Use an entity that doesn't have GROUP BY columns
		EntityDefinition masterDef = testDomain.entities().definition(Master.TYPE);
		SelectQueries.Builder builder = queries.builder(masterDef);
		builder.where((String) null)
						.where("")
						.groupBy(null)
						.groupBy("")
						.having(null)
						.having("");

		String query = builder.build();
		assertFalse(query.contains("WHERE"));
		assertFalse(query.contains("GROUP BY"));
		// Note: HAVING might appear if the entity has a default having clause

		// Test that whitespace is NOT trimmed (kept as actual WHERE clause)
		builder = queries.builder(masterDef);
		builder.where("   ");
		query = builder.build();
		assertTrue(query.contains("WHERE   ")); // Whitespace is preserved (3 spaces)
	}

	@Test
	void testColumnAliasing() {
		SelectQueries.Builder builder = queries.builder(jobDefinition);
		Select select = Select.all(Job.TYPE).build();
		builder.select(select);

		String query = builder.build();
		// Aggregate columns should have aliases
		assertTrue(query.contains("max(sal) AS max_salary"));
		assertTrue(query.contains("min(sal) AS min_salary"));
	}

	@Test
	void testSelectNonSelectedColumns() {
		// Test entity with lazy column
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		Select select = Select.where(Employee.NAME.equalTo("SCOTT"))
						.attributes(Employee.DATA_LAZY) // This column is marked as lazy
						.build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("data_lazy"));
	}

	@Test
	void testDatabaseSpecificClauses() {
		// Test that database-specific clauses are properly handled
		Database db = Database.instance();
		SelectQueries dbQueries = new SelectQueries(db);

		SelectQueries.Builder builder = dbQueries.builder(employeeDefinition);
		Select select = Select.all(Employee.TYPE)
						.limit(10)
						.forUpdate()
						.build();
		builder.select(select);

		String query = builder.build();
		assertNotNull(query);
		// Query should be valid SQL
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("FROM"));
	}

	@Test
	void testComplexHavingConditions() {
		// Test complex HAVING clause with combined conditions
		SelectQueries.Builder builder = queries.builder(jobDefinition);
		Condition having1 = Job.MAX_SALARY.greaterThan(5000.0);
		Condition having2 = Job.MIN_SALARY.lessThan(2000.0);
		Select select = Select.all(Job.TYPE)
						.having(and(having1, having2))
						.build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("HAVING"));
		// Should combine the entity's having clause with the select's having clause
		assertTrue(query.contains("job <> 'PRESIDENT'"));
		assertTrue(query.contains("max(sal) > ?"));
		assertTrue(query.contains("min(sal) < ?"));
	}

	@Test
	void testEntityWithNoSelectableColumns() {
		// Test entity with no primary key - should still work
		EntityDefinition noPkDef = testDomain.entities().definition(NoPrimaryKey.TYPE);
		SelectQueries.Builder builder = queries.builder(noPkDef);
		Select select = Select.all(NoPrimaryKey.TYPE).build();
		builder.select(select);

		String query = builder.build();
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("col1"));
		assertTrue(query.contains("col2"));
		assertTrue(query.contains("col3"));
		assertTrue(query.contains("col4"));
	}

	@Test
	void testChainedWhereConditions() {
		// Test multiple where clauses combined
		SelectQueries.Builder builder = queries.builder(employeeDefinition);
		builder.where(Employee.DEPARTMENT.equalTo(10))
						.where(Employee.JOB.equalTo("CLERK"))
						.where(Employee.SALARY.greaterThan(1000.0));

		String query = builder.build();
		assertTrue(query.contains("WHERE"));
		assertTrue(query.contains("deptno = ?"));
		assertTrue(query.contains("job = ?"));
		assertTrue(query.contains("sal > ?"));
		// Should be combined with AND
		assertEquals(2, query.split("AND").length - 1); // 2 ANDs for 3 conditions
	}

	@Test
	void testSimpleWithClause() {
		EntityDefinition queryWithCte = testDomain.entities().definition(QueryWithCte.TYPE);
		SelectQueries.Builder builder = queries.builder(queryWithCte);
		builder.entitySelectQuery();

		String query = builder.build();
		assertTrue(query.contains("WITH high_earners AS (SELECT empno, ename, deptno FROM employees.employee WHERE sal > 2000)"));
		assertTrue(query.contains("SELECT"));
		assertTrue(query.contains("FROM high_earners"));
		// WITH clause should come before SELECT
		assertTrue(query.indexOf("WITH") < query.indexOf("SELECT"));
	}

	@Test
	void testRecursiveWithClause() {
		EntityDefinition queryWithRecursiveCte = testDomain.entities().definition(QueryWithRecursiveCte.TYPE);
		SelectQueries.Builder builder = queries.builder(queryWithRecursiveCte);
		builder.entitySelectQuery();

		String query = builder.build();
		assertTrue(query.contains("WITH RECURSIVE"));
		// H2 requires explicit column names for recursive CTEs
		assertTrue(query.contains("emp_hierarchy (empno, ename, mgr, level) AS ("));
		assertTrue(query.contains("UNION ALL"));
		assertTrue(query.contains("FROM emp_hierarchy"));
		// WITH RECURSIVE should come before SELECT
		assertTrue(query.indexOf("WITH RECURSIVE") < query.indexOf("SELECT"));
	}

	@Test
	void testMultipleWithClauses() {
		EntityDefinition queryWithMultipleCtes = testDomain.entities().definition(QueryWithMultipleCtes.TYPE);
		SelectQueries.Builder builder = queries.builder(queryWithMultipleCtes);
		builder.entitySelectQuery();

		String query = builder.build();
		assertTrue(query.contains("WITH high_earners AS"));
		assertTrue(query.contains("selected_depts AS"));
		// CTEs should be comma-separated
		assertTrue(query.contains(","));
		assertTrue(query.contains("FROM high_earners he JOIN selected_depts sd"));
		// Multiple CTEs should maintain order
		assertTrue(query.indexOf("high_earners") < query.indexOf("selected_depts"));
	}

	@Test
	void testWithClauseAndSelectConditions() {
		EntityDefinition queryWithCte = testDomain.entities().definition(QueryWithCte.TYPE);
		SelectQueries.Builder builder = queries.builder(queryWithCte);
		Select select = Select.where(QueryWithCte.DEPTNO.equalTo(10))
						.orderBy(OrderBy.ascending(QueryWithCte.ENAME))
						.limit(5)
						.build();
		builder.select(select);

		String query = builder.build();
		// Should have both WITH clause and WHERE clause from Select
		assertTrue(query.contains("WITH high_earners AS"));
		assertTrue(query.contains("WHERE deptno = ?"));
		assertTrue(query.contains("ORDER BY ename"));
		assertTrue(query.contains("5")); // LIMIT
		// WITH should come first, then SELECT with WHERE
		int withIndex = query.indexOf("WITH");
		int selectIndex = query.indexOf("SELECT");
		int whereIndex = query.indexOf("WHERE");
		assertTrue(withIndex < selectIndex);
		assertTrue(selectIndex < whereIndex);
	}

	@Test
	void testWithClauseCaching() {
		// Test that WITH clauses are cached per entity type
		EntityDefinition queryWithCte = testDomain.entities().definition(QueryWithCte.TYPE);
		SelectQueries.Builder builder1 = queries.builder(queryWithCte);
		builder1.entitySelectQuery();
		String query1 = builder1.build();

		// Build again - should use cached WITH clause
		SelectQueries.Builder builder2 = queries.builder(queryWithCte);
		builder2.entitySelectQuery();
		String query2 = builder2.build();

		// Both queries should be identical
		assertEquals(query1, query2);
		assertTrue(query1.contains("WITH high_earners AS"));
	}

	@Test
	void includeExclude() {
		EntityDefinition employee = testDomain.entities().definition(Employee.TYPE);
		Select select = Select.all(Employee.TYPE).build();
		Collection<Attribute<?>> attributes = SelectQueries.attributes(select, employee);
		assertTrue(attributes.contains(Employee.ID));
		assertTrue(attributes.contains(Employee.MGR));
		assertFalse(attributes.contains(Employee.DATA_LAZY));
		select = Select.all(Employee.TYPE)
						.include(Employee.DATA_LAZY)
						.exclude(Employee.MGR_FK)
						.build();
		attributes = SelectQueries.attributes(select, employee);
		assertTrue(attributes.contains(Employee.ID));
		assertFalse(attributes.contains(Employee.MGR));
		assertFalse(attributes.contains(Employee.MGR_FK));
		assertTrue(attributes.contains(Employee.DATA_LAZY));
		select = Select.all(Employee.TYPE)
						.include(Employee.DATA_LAZY)
						.exclude(Employee.MGR_FK, Employee.DATA_LAZY)
						.build();
		attributes = SelectQueries.attributes(select, employee);
		assertTrue(attributes.contains(Employee.ID));
		assertFalse(attributes.contains(Employee.MGR));
		assertFalse(attributes.contains(Employee.MGR_FK));
		assertFalse(attributes.contains(Employee.DATA_LAZY));
	}
}
