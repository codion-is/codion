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
package is.codion.common.db.database;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ColumnValuesTest {

	private final Map<Integer, SetValue<?>> setters = ColumnValues.setters();

	@Test
	void settersAreTyped() throws SQLException {
		// setObject(index, value, type) leaves converting the value to the driver, which many do not support for these
		assertEquals("setShort(1, 1)", set(Types.SMALLINT, (short) 1));
		assertEquals("setInt(1, 1)", set(Types.INTEGER, 1));
		assertEquals("setLong(1, 1)", set(Types.BIGINT, 1L));
		assertEquals("setDouble(1, 1.5)", set(Types.DOUBLE, 1.5));
		assertEquals("setBigDecimal(1, 1.50)", set(Types.DECIMAL, new BigDecimal("1.50")));
		assertEquals("setString(1, a)", set(Types.VARCHAR, "a"));
		assertEquals("setString(1, a)", set(Types.CHAR, 'a'));
		assertEquals("setBoolean(1, true)", set(Types.BOOLEAN, true));
		assertEquals("setBytes(1, [1, 2])", set(Types.BLOB, new byte[] {1, 2}));
		// the driver inferring the type
		assertEquals("setObject(1, 2026-09-26)", set(Types.DATE, LocalDate.of(2026, 9, 26)));
		UUID uuid = UUID.randomUUID();
		assertEquals("setObject(1, " + uuid + ")", set(Types.OTHER, uuid));
	}

	@Test
	void nulls() throws SQLException {
		assertEquals("setNull(1, " + Types.INTEGER + ")", set(Types.INTEGER, null));
		assertEquals("setNull(1, " + Types.DATE + ")", set(Types.DATE, null));
		assertEquals("setNull(1, " + Types.BOOLEAN + ")", set(Types.BOOLEAN, null));
		// a null type of the driver's choosing, neither BLOB nor VARBINARY working for every database
		assertEquals("setBytes(1, null)", set(Types.BLOB, null));
		// an untyped null, a null of type OTHER being rejected by Oracle and Derby
		assertEquals("setObject(1, null)", set(Types.OTHER, null));
	}

	@Test
	void objectGetters() throws SQLException {
		UUID uuid = UUID.randomUUID();
		// the driver converting the value to the class
		assertEquals("getObject(1, class java.util.UUID)", get(ColumnValues.getter(UUID.class, false), uuid));
		// without getObject(int, Class), which Android's ResultSet does not declare, the value cast to the class
		assertEquals("getObject(1)", get(ColumnValues.getter(UUID.class, true), uuid));
		assertThrows(ClassCastException.class, () -> get(ColumnValues.getter(UUID.class, true), uuid.toString()));
	}

	private String get(GetValue<?> getter, Object value) throws SQLException {
		List<String> calls = new ArrayList<>();
		ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {ResultSet.class}, (proxy, method, arguments) -> {
							calls.add(method.getName() + "(" + Arrays.stream(arguments)
											.map(String::valueOf)
											.collect(joining(", ")) + ")");
							return value;
						});
		assertEquals(value, getter.get(resultSet, 1));

		return String.join(" ", calls);
	}

	private String set(int sqlType, @Nullable Object value) throws SQLException {
		List<String> calls = new ArrayList<>();
		PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(),
						new Class<?>[] {PreparedStatement.class}, (proxy, method, arguments) -> {
							calls.add(method.getName() + "(" + Arrays.stream(arguments)
											.map(argument -> argument instanceof byte[] bytes ? Arrays.toString(bytes) : String.valueOf(argument))
											.collect(joining(", ")) + ")");
							return null;
						});
		((SetValue<Object>) setters.get(sqlType)).set(statement, 1, value);

		return String.join(" ", calls);
	}
}
