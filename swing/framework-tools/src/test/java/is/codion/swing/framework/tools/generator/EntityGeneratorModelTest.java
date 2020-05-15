/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.tools.generator;

import is.codion.common.Util;
import is.codion.common.event.EventListener;
import is.codion.common.model.table.SortingDirective;
import is.codion.common.user.User;
import is.codion.common.user.Users;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityGeneratorModelTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  private static final String ADDRESS_DEF;
  private static final String TAG_ITEM_DEF;
  private static final String PRODUCT_DEF;

  static {
    final StringBuilder addressBuilder = new StringBuilder();
    addressBuilder.append("public static final String T_ADDRESS = \"petstore.address\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_ADDRESSID = \"addressid\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STREET1 = \"street1\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STREET2 = \"street2\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_CITY = \"city\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STATE = \"state\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_ZIP = \"zip\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_LATITUDE = \"latitude\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_LONGITUDE = \"longitude\";").append(Util.LINE_SEPARATOR);
    addressBuilder.append(Util.LINE_SEPARATOR);
    addressBuilder.append("void address() {").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  define(T_ADDRESS,").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_ADDRESSID)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .primaryKeyIndex(0),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_STREET1, Types.VARCHAR, \"Street1\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_STREET2, Types.VARCHAR, \"Street2\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_CITY, Types.VARCHAR, \"City\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_STATE, Types.VARCHAR, \"State\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(25),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_ZIP, Types.INTEGER, \"Zip\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, \"Latitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumFractionDigits(2),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, \"Longitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumFractionDigits(2)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  );").append(Util.LINE_SEPARATOR);
    addressBuilder.append("}");

    ADDRESS_DEF = addressBuilder.toString();

    final StringBuilder tagItemBuilder = new StringBuilder();
    tagItemBuilder.append("public static final String T_TAG_ITEM = \"petstore.tag_item\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID = \"tagid\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID_FK = \"tagid_fk\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID = \"itemid\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID_FK = \"itemid_fk\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("void tagItem() {").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  define(T_TAG_ITEM,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("          foreignKeyProperty(TAG_ITEM_TAGID_FK, \"Tagid\", T_TAG,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                columnProperty(TAG_ITEM_TAGID)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                        .primaryKeyIndex(0))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("          foreignKeyProperty(TAG_ITEM_ITEMID_FK, \"Itemid\", T_ITEM,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                columnProperty(TAG_ITEM_ITEMID)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                        .primaryKeyIndex(1))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  );").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("}");

    TAG_ITEM_DEF = tagItemBuilder.toString();

    final StringBuilder productBuilder = new StringBuilder();
    productBuilder.append("public static final String T_PRODUCT = \"petstore.product\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_PRODUCTID = \"productid\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_CATEGORYID = \"categoryid\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_CATEGORYID_FK = \"categoryid_fk\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_NAME = \"name\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_DESCRIPTION = \"description\";").append(Util.LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_IMAGEURL = \"imageurl\";").append(Util.LINE_SEPARATOR);
    productBuilder.append(Util.LINE_SEPARATOR);
    productBuilder.append("void product() {").append(Util.LINE_SEPARATOR);
    productBuilder.append("  define(T_PRODUCT,").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(PRODUCT_PRODUCTID)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .primaryKeyIndex(0),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          foreignKeyProperty(PRODUCT_CATEGORYID_FK, \"Categoryid\", T_CATEGORY,").append(Util.LINE_SEPARATOR);
    productBuilder.append("                columnProperty(PRODUCT_CATEGORYID))").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(PRODUCT_NAME, Types.VARCHAR, \"Name\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(25),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, \"Description\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(255),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(PRODUCT_IMAGEURL, Types.VARCHAR, \"Imageurl\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(55)").append(Util.LINE_SEPARATOR);
    productBuilder.append("  );").append(Util.LINE_SEPARATOR);
    productBuilder.append("}");

    PRODUCT_DEF = productBuilder.toString();
  }

  private EntityGeneratorModel model;

  @BeforeEach
  public void setUp() throws Exception {
    model = new EntityGeneratorModel(UNIT_TEST_USER, "PETSTORE");
    model.getTableModel().getSortModel().setSortingDirective(EntityGeneratorModel.SCHEMA_COLUMN_ID, SortingDirective.ASCENDING);
    model.getTableModel().getSortModel().addSortingDirective(EntityGeneratorModel.TABLE_COLUMN_ID, SortingDirective.ASCENDING);
  }

  @AfterEach
  public void tearDown() {
    model.exit();
  }

  @Test
  public void address() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = counter::incrementAndGet;
    model.addRefreshStartedListener(listener);
    model.addRefreshDoneListener(listener);

    model.getTableModel().getSelectionModel().setSelectedIndex(0);
    assertEquals(2, counter.get());
    final String addressDef = model.getDefinitionTextValue().get();
    assertEquals(ADDRESS_DEF, addressDef);

    model.removeRefreshStartedListener(listener);
    model.removeRefreshDoneListener(listener);
  }

  @Test
  public void product() {
    model.getTableModel().getSelectionModel().setSelectedIndex(3);
    final String productDef = model.getDefinitionTextValue().get();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  public void tagItem() throws Exception {
    model.getTableModel().getSelectionModel().setSelectedIndex(6);
    final String tagItemDef = model.getDefinitionTextValue().get();
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
