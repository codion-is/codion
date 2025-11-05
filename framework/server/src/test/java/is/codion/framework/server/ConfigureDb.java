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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.server;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.utilities.user.User;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import java.sql.SQLException;

public final class ConfigureDb extends DomainModel {

	private static final DomainType DOMAIN = DomainType.domainType(ConfigureDb.class);

	public ConfigureDb() {
		super(DOMAIN);
		add(configured());
	}

	interface Configured {
		EntityType TYPE = DOMAIN.entityType("employees.configured");
		Column<Integer> ID = TYPE.integerColumn("id");
	}

	EntityDefinition configured() {
		return Configured.TYPE.define(Configured.ID.define().primaryKey()).build();
	}

	@Override
	public void configure(Database database) {
		try {
			database.createConnection(User.parse("sa")).createStatement()
							.execute("create table employees.configured(id integer)");
		}
		catch (SQLException e) {
			throw new DatabaseException(e);
		}
	}
}
