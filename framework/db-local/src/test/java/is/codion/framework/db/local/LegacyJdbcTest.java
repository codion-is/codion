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
package is.codion.framework.db.local;

import is.codion.common.db.database.ConnectionProvider;
import is.codion.common.db.database.Database;
import is.codion.common.utilities.user.User;
import is.codion.dbms.h2.H2DatabaseFactory;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static is.codion.common.db.database.Database.LEGACY_JDBC;
import static is.codion.framework.db.local.LocalEntityConnection.localEntityConnection;
import static is.codion.framework.domain.DomainType.domainType;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A UUID key and a foreign key referencing it, written and read back with {@link Database#LEGACY_JDBC}, over
 * connections whose result sets lack getObject(int, Class), as Android's do. A value of a class without an SQL type of
 * its own is then read via getObject(int), H2 returning the UUID itself.
 */
public final class LegacyJdbcTest {

	private static final DomainType DOMAIN = domainType("LegacyJdbcTest");

	interface Area {
		EntityType TYPE = DOMAIN.entityType("legacy.area");

		Column<UUID> ID = TYPE.column("id", UUID.class);
		Column<String> NAME = TYPE.stringColumn("name");
	}

	interface Visit {
		EntityType TYPE = DOMAIN.entityType("legacy.visit");

		Column<UUID> ID = TYPE.column("id", UUID.class);
		Column<UUID> AREA_ID = TYPE.column("area_id", UUID.class);

		ForeignKey AREA_FK = TYPE.foreignKey("area_fk", AREA_ID, Area.ID);
	}

	@Test
	void uuid() {
		try (LocalEntityConnection connection = localEntityConnection(legacyDatabase(), new LegacyDomain(), User.user("sa"))) {
			Entity area = connection.insertSelect(connection.entities().entity(Area.TYPE)
							.with(Area.ID, UUID.randomUUID())
							.with(Area.NAME, "Grótta")
							.build());
			Entity visit = connection.insertSelect(connection.entities().entity(Visit.TYPE)
							.with(Visit.ID, UUID.randomUUID())
							.with(Visit.AREA_FK, area)
							.build());
			Entity selected = connection.selectSingle(Visit.ID.equalTo(visit.get(Visit.ID)));
			assertEquals(visit.get(Visit.ID), selected.get(Visit.ID));
			assertEquals(area.get(Area.ID), selected.get(Visit.AREA_ID));
			assertEquals("Grótta", selected.get(Visit.AREA_FK).get(Area.NAME));
		}
	}

	// A database reads LEGACY_JDBC when created
	private static Database legacyDatabase() {
		LEGACY_JDBC.set(true);
		try {
			Database database = H2DatabaseFactory.database("jdbc:h2:mem:LegacyJdbcTest", "src/test/sql/create_legacy_jdbc.sql");
			database.connectionProvider(new ConnectionProvider() {
				@Override
				public Connection connection(User user, String url) throws SQLException {
					return android(ConnectionProvider.super.connection(user, url), Connection.class);
				}
			});

			return database;
		}
		finally {
			LEGACY_JDBC.set(false);
		}
	}

	private static final List<Class<?>> WRAPPED = Arrays.asList(Statement.class, PreparedStatement.class, ResultSet.class);

	// The given JDBC object, and the statements and result sets reached from it, a result set lacking
	// getObject(int, Class), calling it failing as on Android, where the method does not exist
	private static <T> T android(T target, Class<T> type) {
		return type.cast(Proxy.newProxyInstance(LegacyJdbcTest.class.getClassLoader(), new Class<?>[] {type}, (proxy, method, arguments) -> {
			if (type == ResultSet.class && method.getName().equals("getObject")
							&& Arrays.equals(method.getParameterTypes(), new Class<?>[] {int.class, Class.class})) {
				throw new NoSuchMethodError("java.sql.ResultSet.getObject(int, Class)");
			}
			try {
				Object result = method.invoke(target, arguments);

				return result != null && WRAPPED.contains(method.getReturnType()) ?
								android(result, (Class<Object>) method.getReturnType()) : result;
			}
			catch (InvocationTargetException e) {
				throw e.getCause();
			}
		}));
	}

	private static final class LegacyDomain extends DomainModel {

		private LegacyDomain() {
			super(DOMAIN);
			add(Area.TYPE.as()
							.attributes(
											Area.ID.as()
															.primaryKey(),
											Area.NAME.as()
															.column())
							.build());
			add(Visit.TYPE.as()
							.attributes(
											Visit.ID.as()
															.primaryKey(),
											Visit.AREA_ID.as()
															.column(),
											Visit.AREA_FK.as()
															.foreignKey())
							.build());
		}
	}
}
