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
package is.codion.framework.domain.db;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;

import static is.codion.framework.domain.db.MetaDataColumn.columnType;
import static org.junit.jupiter.api.Assertions.assertEquals;

// The type codes, names, sizes and digits as reported by the drivers, measured 2026-09-26
public final class MetaDataColumnTest {

	@Test
	void floatingPoint() {
		assertEquals(Double.class, columnType(Types.DOUBLE, "DOUBLE PRECISION", 53, 0, false));// H2
		assertEquals(Double.class, columnType(Types.REAL, "REAL", 24, 0, false));// H2
		assertEquals(Double.class, columnType(Types.DOUBLE, "DOUBLE", 22, 0, false));// MySQL
		assertEquals(Double.class, columnType(Types.REAL, "FLOAT", 12, 0, false));// MySQL
		assertEquals(Double.class, columnType(100, "BINARY_FLOAT", 4, -1, false));// Oracle
	}

	@Test
	void wholeNumbers() {
		assertEquals(Short.class, columnType(Types.TINYINT, "tinyint", 3, 0, false));// SQL Server, MySQL
		assertEquals(Integer.class, columnType(Types.NUMERIC, "NUMBER", 5, 0, false));// Oracle number(5)
		assertEquals(Integer.class, columnType(Types.NUMERIC, "NUMBER", 9, 0, false));
		assertEquals(Long.class, columnType(Types.NUMERIC, "NUMBER", 10, 0, false));// Oracle number(10), beyond an Integer
		assertEquals(Long.class, columnType(Types.NUMERIC, "NUMERIC", 18, 0, false));
		assertEquals(Long.class, columnType(Types.NUMERIC, "NUMBER", 38, 0, false));// Oracle integer
		assertEquals(BigDecimal.class, columnType(Types.NUMERIC, "NUMBER", 0, -127, false));// Oracle number, unconstrained
		assertEquals(BigDecimal.class, columnType(Types.DECIMAL, "DECIMAL", 10, 2, false));
	}

	@Test
	void strings() {
		assertEquals(Character.class, columnType(Types.CHAR, "CHARACTER", 1, 0, false));
		assertEquals(String.class, columnType(Types.CHAR, "CHARACTER", 10, 0, false));
		assertEquals(Character.class, columnType(Types.NCHAR, "nchar", 1, -1, false));// SQL Server
		assertEquals(String.class, columnType(Types.NVARCHAR, "nvarchar", 50, -1, false));// SQL Server
		assertEquals(String.class, columnType(Types.CLOB, "CLOB", 2147483647, -1, false));
		assertEquals(String.class, columnType(Types.CHAR, "uniqueidentifier", 36, -1, false));// SQL Server
	}

	@Test
	void binary() {
		assertEquals(byte[].class, columnType(Types.BINARY, "bytea", 2147483647, 0, false));// PostgreSQL
		assertEquals(byte[].class, columnType(Types.LONGVARBINARY, "LONGBLOB", 2147483647, -1, false));// MySQL
		assertEquals(byte[].class, columnType(Types.VARBINARY, "varbinary", 2147483647, -1, false));// SQL Server
	}

	@Test
	void byTypeName() {
		assertEquals(OffsetDateTime.class, columnType(Types.TIMESTAMP, "timestamptz", 35, 6, false));// PostgreSQL
		assertEquals(OffsetTime.class, columnType(Types.TIME, "timetz", 21, 6, false));// PostgreSQL
		assertEquals(OffsetDateTime.class, columnType(-101, "TIMESTAMP(6) WITH TIME ZONE", 13, 6, false));// Oracle
		assertEquals(OffsetDateTime.class, columnType(-155, "datetimeoffset", 34, 7, false));// SQL Server
		assertEquals(UUID.class, columnType(Types.BINARY, "UUID", 16, 0, false));// H2
		assertEquals(UUID.class, columnType(Types.OTHER, "uuid", 2147483647, 0, false));// PostgreSQL
		assertEquals(LocalDateTime.class, columnType(Types.TIMESTAMP, "DATE", 7, -1, false));// Oracle date, with a time
	}

	@Test
	void sqliteDeclaredTypes() {
		assertEquals(Long.class, columnType(Types.INTEGER, "BIGINT", 2000000000, 0, true));
		assertEquals(Short.class, columnType(Types.INTEGER, "SMALLINT", 2000000000, 0, true));
		assertEquals(Boolean.class, columnType(Types.INTEGER, "BOOLEAN", 2000000000, 0, true));
		assertEquals(LocalDate.class, columnType(Types.VARCHAR, "DATE", 2000000000, 10, true));
		assertEquals(LocalDateTime.class, columnType(Types.VARCHAR, "TIMESTAMP", 2000000000, 10, true));
		assertEquals(LocalDateTime.class, columnType(Types.VARCHAR, "DATETIME", 2000000000, 10, true));
		assertEquals(byte[].class, columnType(Types.VARCHAR, "BLOB", 2000000000, 0, true));
		assertEquals(Character.class, columnType(Types.VARCHAR, "CHAR", 1, 0, true));
		assertEquals(String.class, columnType(Types.VARCHAR, "TEXT", 2000000000, 0, true));
		assertEquals(Long.class, columnType(Types.FLOAT, "NUMERIC(18,0)", 18, 0, true));
		assertEquals(BigDecimal.class, columnType(Types.FLOAT, "DECIMAL(10,2)", 12, 2, true));
		// not readable by the driver, left as reported
		assertEquals(String.class, columnType(Types.VARCHAR, "TIMESTAMP WITH TIME ZONE", 2000000000, 10, true));
	}
}
