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
 * Copyright (c) 2019 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.generator;

import is.codion.common.db.database.Database;
import is.codion.common.user.User;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainGeneratorModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void petstore() throws Exception {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().refresh();
		model.schemaModel().comparator().set(comparing(SchemaRow::name));

		List<SchemaRow> schema = model.schemaModel().items().stream()
						.filter(item -> item.schema.equals("PETSTORE"))
						.collect(toList());
		model.schemaModel().selectionModel().setSelectedIndex(model.schemaModel().indexOf(schema.get(0)));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.petstore.domain");
		String petstoreApi = textFileContents(DomainGeneratorModelTest.class, "PetstoreAPI.java");
		assertEquals(petstoreApi, model.domainApi().get());
		String petstoreImpl = textFileContents(DomainGeneratorModelTest.class, "PetstoreImpl.java");
		assertEquals(petstoreImpl, model.domainImpl().get());
		String petstoreCombined = textFileContents(DomainGeneratorModelTest.class, "Petstore.java");
		assertEquals(petstoreCombined, model.domainCombined().get());

		model.close();
	}

	@Test
	void chinook() throws Exception {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().refresh();
		model.schemaModel().comparator().set(comparing(SchemaRow::name));

		List<SchemaRow> schema = model.schemaModel().items().stream()
						.filter(item -> item.schema.equals("CHINOOK"))
						.collect(toList());
		model.schemaModel().selectionModel().setSelectedIndex(model.schemaModel().indexOf(schema.get(0)));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.chinook.domain");
		String chinookApi = textFileContents(DomainGeneratorModelTest.class, "ChinookAPI.java");
		assertEquals(chinookApi, model.domainApi().get());
		String chinookImpl = textFileContents(DomainGeneratorModelTest.class, "ChinookImpl.java");
		assertEquals(chinookImpl, model.domainImpl().get());
		String chinookCombined = textFileContents(DomainGeneratorModelTest.class, "Chinook.java");
		assertEquals(chinookCombined, model.domainCombined().get());

		model.close();
	}

	@Test
	void world() throws Exception {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().refresh();
		model.schemaModel().comparator().set(comparing(SchemaRow::name));

		List<SchemaRow> schema = model.schemaModel().items().stream()
						.filter(item -> item.schema.equals("WORLD"))
						.collect(toList());
		model.schemaModel().selectionModel().setSelectedIndex(model.schemaModel().indexOf(schema.get(0)));
		model.populateSelected(s -> {});
		model.domainPackage().set("is.codion.world.domain");
		String worldApi = textFileContents(DomainGeneratorModelTest.class, "WorldAPI.java");
		assertEquals(worldApi, model.domainApi().get());
		String worldImpl = textFileContents(DomainGeneratorModelTest.class, "WorldImpl.java");
		assertEquals(worldImpl, model.domainImpl().get());
		String worldCombined = textFileContents(DomainGeneratorModelTest.class, "World.java");
		assertEquals(worldCombined, model.domainCombined().get());

		model.close();
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n"));
		}
	}
}
