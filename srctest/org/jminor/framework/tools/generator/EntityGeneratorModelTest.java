/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.tools.generator;

import org.jminor.common.model.EventAdapter;
import org.jminor.common.model.User;
import org.jminor.common.model.Util;
import org.jminor.common.model.table.SortingDirective;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class EntityGeneratorModelTest {

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
    addressBuilder.append("Entities.define(T_ADDRESS,").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.primaryKeyProperty(ADDRESS_ADDRESSID),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STREET1, Types.VARCHAR, \"Street1\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STREET2, Types.VARCHAR, \"Street2\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_CITY, Types.VARCHAR, \"City\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_STATE, Types.VARCHAR, \"State\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaxLength(25),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_ZIP, Types.INTEGER, \"Zip\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, \"Latitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaximumFractionDigits(2),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("        Properties.columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, \"Longitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .setMaximumFractionDigits(2)").append(Util.LINE_SEPARATOR);
    addressBuilder.append(");").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

    ADDRESS_DEF = addressBuilder.toString();

    final StringBuilder tagItemBuilder = new StringBuilder();
    tagItemBuilder.append("public static final String T_TAG_ITEM = \"petstore.tag_item\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID = \"tagid\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_TAGID_FK = \"tagid_fk\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID = \"itemid\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("public static final String TAG_ITEM_ITEMID_FK = \"itemid_fk\";").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("Entities.define(T_TAG_ITEM,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("        Properties.foreignKeyProperty(TAG_ITEM_TAGID_FK, \"Tagid\", T_TAG,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                Properties.primaryKeyProperty(TAG_ITEM_TAGID))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .setNullable(false),").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("        Properties.foreignKeyProperty(TAG_ITEM_ITEMID_FK, \"Itemid\", T_ITEM,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                Properties.primaryKeyProperty(TAG_ITEM_ITEMID)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                        .setIndex(1))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append(");").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

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
    productBuilder.append("Entities.define(T_PRODUCT,").append(Util.LINE_SEPARATOR);
    productBuilder.append("        Properties.primaryKeyProperty(PRODUCT_PRODUCTID),").append(Util.LINE_SEPARATOR);
    productBuilder.append("        Properties.foreignKeyProperty(PRODUCT_CATEGORYID_FK, \"Categoryid\", T_CATEGORY,").append(Util.LINE_SEPARATOR);
    productBuilder.append("                Properties.columnProperty(PRODUCT_CATEGORYID))").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false),").append(Util.LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_NAME, Types.VARCHAR, \"Name\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(25),").append(Util.LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, \"Description\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setNullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(255),").append(Util.LINE_SEPARATOR);
    productBuilder.append("        Properties.columnProperty(PRODUCT_IMAGEURL, Types.VARCHAR, \"Imageurl\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .setMaxLength(55)").append(Util.LINE_SEPARATOR);
    productBuilder.append(");").append(Util.LINE_SEPARATOR).append(Util.LINE_SEPARATOR);

    PRODUCT_DEF = productBuilder.toString();
  }

  private EntityGeneratorModel model;

  @Before
  public void setUp() throws Exception{
    model = new EntityGeneratorModel(new User("scott", "tiger"), "PETSTORE");
    model.getTableModel().getSortModel().setSortingDirective(EntityGeneratorModel.SCHEMA_COLUMN_ID, SortingDirective.ASCENDING, false);
    model.getTableModel().getSortModel().setSortingDirective(EntityGeneratorModel.TABLE_COLUMN_ID, SortingDirective.ASCENDING, true);
  }

  @After
  public void tearDown() {
    model.exit();
  }

  @Test
  public void address() {
    assertNotNull(model.getDocument());
    final Collection<Object> counter = new ArrayList<Object>();
    final EventAdapter listener = new EventAdapter() {
      @Override
      public void eventOccurred() {
        counter.add(new Object());
      }
    };
    model.addRefreshStartedListener(listener);
    model.addRefreshDoneListener(listener);

    model.getTableModel().getSelectionModel().setSelectedIndex(0);
    assertEquals(2, counter.size());
    final String addressDef = model.getDocumentText();
    assertEquals(ADDRESS_DEF, addressDef);

    model.removeRefreshStartedListener(listener);
    model.removeRefreshDoneListener(listener);
  }

  @Test
  public void product() {
    model.getTableModel().getSelectionModel().setSelectedIndex(3);
    final String productDef = model.getDocumentText();
    assertEquals(PRODUCT_DEF, productDef);
  }

  @Test
  public void tagItem() throws Exception {
    model.getTableModel().getSelectionModel().setSelectedIndex(6);
    final String tagItemDef = model.getDocumentText();
    assertEquals(TAG_ITEM_DEF, tagItemDef);
  }
}
