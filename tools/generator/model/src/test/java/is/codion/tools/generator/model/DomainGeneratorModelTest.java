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
 * Copyright (c) 2019 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.generator.model;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;
import is.codion.tools.generator.model.DomainGeneratorModel.SchemaColumns;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DomainGeneratorModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void petstore() {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().sort().ascending(SchemaColumns.Id.SCHEMA);
		model.schemaModel().items().refresh();
		model.schemaModel().selection().items().set(row -> row.schema().equals("PETSTORE"));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.petstore.domain");
		assertNotNull(model.domainApi().get());
		assertNotNull(model.domainImpl().get());
		assertNotNull(model.domainCombined().get());
	}

	@Test
	void chinook() {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().sort().ascending(SchemaColumns.Id.SCHEMA);
		model.schemaModel().items().refresh();
		model.schemaModel().selection().items().set(row -> row.schema().equals("CHINOOK"));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.chinook.domain");
		assertNotNull(model.domainApi().get());
		assertNotNull(model.domainImpl().get());
		assertNotNull(model.domainCombined().get());
	}

	@Test
	void world() {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().sort().ascending(SchemaColumns.Id.SCHEMA);
		model.schemaModel().items().refresh();
		model.schemaModel().selection().items().set(row -> row.schema().equals("WORLD"));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.world.domain");
		assertNotNull(model.domainApi().get());
		assertNotNull(model.domainImpl().get());
		assertNotNull(model.domainCombined().get());
	}
}
