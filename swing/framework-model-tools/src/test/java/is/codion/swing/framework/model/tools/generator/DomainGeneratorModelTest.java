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
import java.util.Comparator;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainGeneratorModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	@Test
	void test() throws Exception {
		DomainGeneratorModel model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.domainPackage().set("is.codion.petstore.domain");
		model.schemaModel().refresh();
		model.schemaModel().comparator().set(comparing(SchemaRow::name));
		model.schemaModel().selectionModel().setSelectedIndex(1);
		model.populateSelected(schema -> {});
		model.definitionModel().comparator().set(Comparator.
						<DefinitionRow, String>comparing(definitionRow -> definitionRow.definition.entityType().domainType().name())
						.thenComparing(definitionRow -> definitionRow.definition.entityType().name()));
		model.definitionModel().selectionModel().selectAll();
		String petstoreApi = textFileContents(DomainGeneratorModelTest.class, "PetstoreAPI.java");
		assertEquals(petstoreApi, model.domainApi().get());
		String petstoreImpl = textFileContents(DomainGeneratorModelTest.class, "PetstoreImpl.java");
		assertEquals(petstoreImpl, model.domainImpl().get());
		String petstoreCombined = textFileContents(DomainGeneratorModelTest.class, "Petstore.java");
		assertEquals(petstoreCombined, model.domainCombined().get());
		model.close();
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n"));
		}
	}
}
