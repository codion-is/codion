/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools;

import org.jminor.common.model.SortingDirective;
import org.jminor.common.model.User;
import org.jminor.framework.tools.generator.EntityGeneratorModel;

import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class EntityGeneratorModelTest {

  private static final String LINE_SEPARATOR = System.getProperty("line.separator");
  private static final String ADDRESS_DEF;
  private static final String TAG_ITEM_DEF;
  private static final String PRODUCT_DEF;

  static {
    final StringBuilder addressBuilder = new StringBuilder();
    addressBuilder.append("public static final String T_ADDRESS = \"petstore.address\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_ADDRESSID = \"addressid\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STREET1 = \"street1\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STREET2 = \"street2\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_CITY = \"city\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_STATE = \"state\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_ZIP = \"zip\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_LATITUDE = \"latitude\";").append(LINE_SEPARATOR);
    addressBuilder.append("public static final String ADDRESS_LONGITUDE = \"longitude\";").append(LINE_SEPARATOR);
    addressBuilder.append(LINE_SEPARATOR);
    addressBuilder.append("Entities.define(T_ADDRESS,").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.primaryKeyProperty(ADDRESS_ADDRESSID),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STREET1, Types.VARCHAR, \"Street1\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STREET2, Types.VARCHAR, \"Street2\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_CITY, Types.VARCHAR, \"City\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STATE, Types.VARCHAR, \"State\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(25),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_ZIP, Types.INTEGER, \"Zip\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, \"Latitude\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaximumFractionDigits(2),").append(LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, \"Longitude\")").append(LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    addressBuilder.append("                .setMaximumFractionDigits(2)").append(LINE_SEPARATOR);
    addressBuilder.append(");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);

    ADDRESS_DEF = addressBuilder.toString();

    final StringBuilder tagItemBuilder = new StringBuilder();
    tagItemBuilder.append("public static final String T_TAG_ITEM = \"petstore.tag_item\";").append(LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID = \"tagid\";").append(LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID_FK = \"tagid_fk\";").append(LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID = \"itemid\";").append(LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID_FK = \"itemid_fk\";").append(LINE_SEPARATOR);
    tagItemBuilder.append(LINE_SEPARATOR);
    tagItemBuilder.append("Entities.define(T_TAG_ITEM,").append(LINE_SEPARATOR);
    tagItemBuilder.append("        Properties.foreignKeyProperty(TAG_ITEM_TAGID_FK, \"Tagid\",T_TAG,").append(LINE_SEPARATOR);
    tagItemBuilder.append("                Properties.primaryKeyProperty(TAG_ITEM_ITEM_ID)").append(LINE_SEPARATOR);
    tagItemBuilder.append("                        .setIndex(0))").append(LINE_SEPARATOR);
    tagItemBuilder.append("                .setNullable(false),").append(LINE_SEPARATOR);
    tagItemBuilder.append("        Properties.foreignKeyProperty(TAG_ITEM_ITEMID_FK, \"Itemid\",T_ITEM,").append(LINE_SEPARATOR);
    tagItemBuilder.append("                Properties.primaryKeyProperty(TAG_ITEM_ITEM_ID)").append(LINE_SEPARATOR);
    tagItemBuilder.append("                        .setIndex(0))").append(LINE_SEPARATOR);
    tagItemBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    tagItemBuilder.append(");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);

    TAG_ITEM_DEF = tagItemBuilder.toString();

    final StringBuilder productBuilder = new StringBuilder();
    productBuilder.append("public static final String T_PRODUCT = \"petstore.product\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_PRODUCTID = \"productid\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_CATEGORYID = \"categoryid\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_CATEGORYID_FK = \"categoryid_fk\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_NAME = \"name\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_DESCRIPTION = \"description\";").append(LINE_SEPARATOR);
    productBuilder.append("public static final String PRODUCT_IMAGEURL = \"imageurl\";").append(LINE_SEPARATOR);
    productBuilder.append(LINE_SEPARATOR);
    productBuilder.append("Entities.define(T_PRODUCT,").append(LINE_SEPARATOR);
    productBuilder.append("        Properties.primaryKeyProperty(PRODUCT_PRODUCTID),").append(LINE_SEPARATOR);
    productBuilder.append("        Properties.foreignKeyProperty(PRODUCT_CATEGORYID_FK, \"Categoryid\",T_CATEGORY,").append(LINE_SEPARATOR);
    productBuilder.append("                Properties.columnProperty(PRODUCT_CATEGORYID))").append(LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false),").append(LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_NAME, Types.VARCHAR, \"Name\")").append(LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(25),").append(LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, \"Description\")").append(LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false)").append(LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(255),").append(LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_IMAGEURL, Types.VARCHAR, \"Imageurl\")").append(LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(55)").append(LINE_SEPARATOR);
    productBuilder.append(");").append(LINE_SEPARATOR).append(LINE_SEPARATOR);

    PRODUCT_DEF = productBuilder.toString();
  }

  private EntityGeneratorModel model;

  @Before
  public void setUp() throws Exception{
    model = new EntityGeneratorModel(new User("scott", "tiger"), "PETSTORE");
  }

  @After
  public void tearDown() {
    model.exit();
  }

  @Test
  public void address() {
    model.getTableModel().setSortingDirective(0, SortingDirective.ASCENDING);
    model.getTableModel().setSelectedItemIndex(0);
    final String addressDef = model.getDocumentText();
    assertEquals(ADDRESS_DEF, addressDef);
  }

  @Test
  public void product() {
    model.getTableModel().setSelectedItemIndex(3);
    final String productDef = model.getDocumentText();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  @Ignore
  public void tagItem() throws Exception {
    model.getTableModel().setSelectedItemIndex(6);
    final String tagItemDef = model.getDocumentText();
    System.out.println(tagItemDef);
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
