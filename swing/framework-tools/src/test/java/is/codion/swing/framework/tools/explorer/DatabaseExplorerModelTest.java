/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.Text;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DatabaseExplorerModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String ADDRESS_DEF;
  private static final String TAG_ITEM_DEF;
  private static final String PRODUCT_DEF;

  static {
    try {
      ADDRESS_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "address.txt").trim();
      TAG_ITEM_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "tagitem.txt").trim();
      PRODUCT_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "product.txt").trim();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DatabaseExplorerModel model;

  @BeforeEach
  void setUp() throws Exception {
    model = new DatabaseExplorerModel(DatabaseFactory.getDatabase(), UNIT_TEST_USER);
    model.getSchemaModel().refresh();
    model.getSchemaModel().sortModel().setSortOrder(0, SortOrder.ASCENDING);
    model.getSchemaModel().selectionModel().setSelectedIndex(2);
    model.populateSelected(schema -> {});
    model.getDefinitionModel().sortModel().setSortOrder(0, SortOrder.ASCENDING);
    model.getDefinitionModel().sortModel().addSortOrder(1, SortOrder.ASCENDING);
  }

  @AfterEach
  void tearDown() {
    model.close();
  }

  @Test
  void address() {
    model.getDefinitionModel().selectionModel().setSelectedIndex(0);
    String addressDef = model.getDomainSourceObserver().get().trim();
    assertEquals(ADDRESS_DEF, addressDef);
  }

  @Test
  void product() {
    model.getDefinitionModel().selectionModel().setSelectedIndex(3);
    String productDef = model.getDomainSourceObserver().get().trim();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  void tagItem() throws Exception {
    model.getDefinitionModel().selectionModel().setSelectedIndex(6);
    String tagItemDef = model.getDomainSourceObserver().get().trim();
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
