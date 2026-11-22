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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.dbms.sqlite;

import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.emptyList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class StatementsTest {

	@Test
	void split() {
		assertEquals(List.of("create table a (id integer)", "insert into a values (1)"),
						Statements.split("create table a (id integer);\ninsert into a values (1);\n"));
		// the last without a semicolon
		assertEquals(List.of("select 1", "select 2"), Statements.split("select 1; select 2"));
		// nothing but whitespace and semicolons, and comments alone
		assertEquals(emptyList(), Statements.split(" ;\n; "));
		assertEquals(emptyList(), Statements.split("-- a comment; with a semicolon\n/* another; */"));
	}

	@Test
	void quotesAndComments() {
		// semicolons in strings, an escaped quote included, quoted identifiers and comments
		assertEquals(List.of("insert into a values ('x;y', 'it''s; here')", "select \"a;b\", `c;d`, [e;f] from a"),
						Statements.split("insert into a values ('x;y', 'it''s; here'); select \"a;b\", `c;d`, [e;f] from a;"));
		assertEquals(List.of("-- leading; comment\ncreate table b (id integer)", "/* a; block */ select 1"),
						Statements.split("-- leading; comment\ncreate table b (id integer); /* a; block */ select 1;"));
		// a block comment closing only after it opens
		assertEquals(List.of("/*/ still a comment; */ select 1"), Statements.split("/*/ still a comment; */ select 1;"));
	}

	@Test
	void trigger() {
		String trigger = "create trigger a_b after insert on a begin insert into b values (new.id); update c set n = n + 1; end";
		assertEquals(List.of(trigger, "insert into a values (1)"), Statements.split(trigger + ";\ninsert into a values (1);"));
		String temporary = "CREATE TEMP TRIGGER IF NOT EXISTS a_c AFTER DELETE ON a BEGIN DELETE FROM c; END";
		assertEquals(List.of(temporary, "select 1"), Statements.split(temporary + "; select 1;"));
		// begin and end as words of a statement other than a trigger
		assertEquals(List.of("select 'begin' as begin_at", "select 1"), Statements.split("select 'begin' as begin_at; select 1;"));
	}
}
