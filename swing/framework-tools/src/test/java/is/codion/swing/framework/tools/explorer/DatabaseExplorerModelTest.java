/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.explorer;

import is.codion.common.Text;
import is.codion.common.db.database.DatabaseFactory;
import is.codion.common.model.table.TableSortModel.SortingDirective;
import is.codion.common.user.User;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public final class DatabaseExplorerModelTest {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String ADDRESS_DEF;
  private static final String TAG_ITEM_DEF;
  private static final String PRODUCT_DEF;

  static {
    try {
      ADDRESS_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "address.txt").trim();
      TAG_ITEM_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "tagitem.txt").trim();
      PRODUCT_DEF = Text.getTextFileContents(DatabaseExplorerModelTest.class, "product.txt").trim();
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DatabaseExplorerModel model;

  @BeforeEach
  public void setUp() throws Exception {
    model = new DatabaseExplorerModel(DatabaseFactory.getDatabase(), UNIT_TEST_USER);
    model.getSchemaModel().refresh();
    model.getSchemaModel().getSortModel().setSortingDirective(0, SortingDirective.ASCENDING);
    model.getSchemaModel().getSelectionModel().setSelectedIndex(1);
    model.populateSelected(schema -> {});
    model.getDefinitionModel().getSortModel().setSortingDirective(0, SortingDirective.ASCENDING);
    model.getDefinitionModel().getSortModel().addSortingDirective(1, SortingDirective.ASCENDING);
  }

  @AfterEach
  public void tearDown() {
    model.close();
  }

  @Test
  public void address() {
    model.getDefinitionModel().getSelectionModel().setSelectedIndex(0);
    final String addressDef = model.getDomainSourceObserver().get().trim();
    assertEquals(ADDRESS_DEF, addressDef);
  }

  @Test
  public void product() {
    model.getDefinitionModel().getSelectionModel().setSelectedIndex(3);
    final String productDef = model.getDomainSourceObserver().get().trim();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  public void tagItem() throws Exception {
    model.getDefinitionModel().getSelectionModel().setSelectedIndex(6);
    final String tagItemDef = model.getDomainSourceObserver().get().trim();
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
