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
import is.codion.swing.framework.model.tools.metadata.MetaDataSchema;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Comparator;

import static java.util.stream.Collectors.joining;
import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainGeneratorModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final String ADDRESS_DEF;
	private static final String TAG_ITEM_DEF;
	private static final String PRODUCT_DEF;

	static {
		try {
			ADDRESS_DEF = textFileContents(DomainGeneratorModelTest.class, "address.txt").trim();
			TAG_ITEM_DEF = textFileContents(DomainGeneratorModelTest.class, "tagitem.txt").trim();
			PRODUCT_DEF = textFileContents(DomainGeneratorModelTest.class, "product.txt").trim();
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private DomainGeneratorModel model;

	@BeforeEach
	void setUp() throws Exception {
		model = DomainGeneratorModel.domainGeneratorModel(Database.instance(), UNIT_TEST_USER);
		model.schemaModel().refresh();
		model.schemaModel().comparator().set(Comparator.comparing(MetaDataSchema::name));
		model.schemaModel().selectionModel().setSelectedIndex(1);
		model.populateSelected(schema -> {});
		model.definitionModel().comparator().set(Comparator.
						<DefinitionRow, String>comparing(definitionRow -> definitionRow.definition.entityType().domainType().name())
						.thenComparing(definitionRow -> definitionRow.definition.entityType().name()));
	}

	@AfterEach
	void tearDown() {
		model.close();
	}

	@Test
	void address() {
		model.definitionModel().selectionModel().setSelectedIndex(0);
		String addressDef = model.domainSource().get().trim();
		assertEquals(ADDRESS_DEF, addressDef);
	}

	@Test
	void product() {
		model.definitionModel().selectionModel().setSelectedIndex(3);
		String productDef = model.domainSource().get().trim();
		assertEquals(PRODUCT_DEF, productDef);
	}

	@Test
	void tagItem() {
		model.definitionModel().selectionModel().setSelectedIndex(6);
		String tagItemDef = model.domainSource().get().trim();
		assertEquals(TAG_ITEM_DEF, tagItemDef);
	}

	private static String textFileContents(Class<?> resourceClass, String resourceName) throws IOException {
		try (BufferedReader input = new BufferedReader(new InputStreamReader(resourceClass.getResourceAsStream(resourceName)))) {
			return input.lines().collect(joining("\n"));
		}
	}
}
