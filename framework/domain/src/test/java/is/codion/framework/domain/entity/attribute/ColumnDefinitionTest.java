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
package is.codion.framework.domain.entity.attribute;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ColumnDefinitionTest {

	private static final DomainType DOMAIN_TYPE = DomainType.domainType("columnDefinitionTest");
	private static final EntityType ENTITY_TYPE = DOMAIN_TYPE.entityType("entityType");

	@Test
	void readsAndWritesTheColumnClass() throws SQLException {
		Timestamp timestamp = Timestamp.valueOf("2026-09-26 13:45:30.123456");
		ResultSet resultSet = (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {ResultSet.class},
						(proxy, method, arguments) -> method.getName().equals("getObject") && arguments[1] == Timestamp.class ? timestamp : null);
		// not the LocalDateTime based default getter for the TIMESTAMP type it shares
		ColumnDefinition<Timestamp> column = (ColumnDefinition<Timestamp>) ENTITY_TYPE.column("timestamp", Timestamp.class).as()
						.column()
						.build();
		assertEquals(timestamp, column.get(resultSet, 1, null));
		// and not the LocalDateTime based default setter either, with a TIMESTAMP typed null
		List<String> calls = new ArrayList<>();
		PreparedStatement statement = (PreparedStatement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {PreparedStatement.class},
						(proxy, method, arguments) -> {
							calls.add(method.getName() + "(" + arguments[0] + ", " + arguments[1] + ")");
							return null;
						});
		column.set(statement, 1, timestamp, null);
		column.set(statement, 2, null, null);
		assertEquals(Arrays.asList("setObject(1, " + timestamp + ")", "setNull(2, " + Types.TIMESTAMP + ")"), calls);
		// the class of the converter's column, not the attribute's
		ColumnDefinition<LocalDateTime> converted = (ColumnDefinition<LocalDateTime>) ENTITY_TYPE.localDateTimeColumn("converted").as()
						.column()
						.converter(Timestamp.class, new Column.Converter<LocalDateTime, Timestamp>() {
							@Override
							public Timestamp toColumn(LocalDateTime value, Statement statement) {
								return Timestamp.valueOf(value);
							}

							@Override
							public LocalDateTime fromColumn(Timestamp columnValue) {
								return columnValue.toLocalDateTime();
							}
						})
						.build();
		assertEquals(timestamp.toLocalDateTime(), converted.get(resultSet, 1, null));
	}

	@Test
	void setColumnName() {
		assertEquals("hello", ((ColumnDefinition<?>) ENTITY_TYPE.integerColumn("attribute").as().column().name("hello").build()).name());
	}

	@Test
	void setColumnNameNull() {
		assertThrows(NullPointerException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column().name(null));
	}

	@Test
	void subqueryColumns() {
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").readOnly(true));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").readOnly(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test").as().subquery("select").updatable(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test")
						.as()
						.subquery("select")
						.insertable(false));
		assertThrows(UnsupportedOperationException.class, () -> ENTITY_TYPE.integerColumn("test")
						.as()
						.subquery("select")
						.expression("expression"));
	}

	@Test
	void searchableNonVarchar() {
		assertThrows(IllegalStateException.class, () -> ENTITY_TYPE.integerColumn("attribute").as().column().searchable(true));
	}
}
