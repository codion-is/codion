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

import is.codion.common.Text;
import is.codion.common.db.database.Database;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DomainGeneratorModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String ADDRESS_DEF;
  private static final String TAG_ITEM_DEF;
  private static final String PRODUCT_DEF;

  static {
    try {
      ADDRESS_DEF = Text.textFileContents(DomainGeneratorModelTest.class, "address.txt").trim();
      TAG_ITEM_DEF = Text.textFileContents(DomainGeneratorModelTest.class, "tagitem.txt").trim();
      PRODUCT_DEF = Text.textFileContents(DomainGeneratorModelTest.class, "product.txt").trim();
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
    model.schemaModel().sortModel().setSortOrder(1, SortOrder.ASCENDING);
    model.schemaModel().selectionModel().setSelectedIndex(1);
    model.populateSelected(schema -> {});
    model.definitionModel().sortModel().setSortOrder(0, SortOrder.ASCENDING);
    model.definitionModel().sortModel().addSortOrder(1, SortOrder.ASCENDING);
  }

  @AfterEach
  void tearDown() {
    model.close();
  }

  @Test
  void address() {
    model.definitionModel().selectionModel().setSelectedIndex(0);
    String addressDef = model.domainSourceObserver().get().trim();
    assertEquals(ADDRESS_DEF, addressDef);
  }

  @Test
  void product() {
    model.definitionModel().selectionModel().setSelectedIndex(3);
    String productDef = model.domainSourceObserver().get().trim();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  void tagItem() {
    model.definitionModel().selectionModel().setSelectedIndex(6);
    String tagItemDef = model.domainSourceObserver().get().trim();
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
