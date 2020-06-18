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
    addressBuilder.append("public interface Address {").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  EntityType<Entity> TYPE = DOMAIN.entityType(\"petstore.address\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<Integer> ADDRESSID = TYPE.integerAttribute(\"addressid\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<String> STREET1 = TYPE.stringAttribute(\"street1\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<String> STREET2 = TYPE.stringAttribute(\"street2\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<String> CITY = TYPE.stringAttribute(\"city\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<String> STATE = TYPE.stringAttribute(\"state\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<Integer> ZIP = TYPE.integerAttribute(\"zip\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<Double> LATITUDE = TYPE.doubleAttribute(\"latitude\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  Attribute<Double> LONGITUDE = TYPE.doubleAttribute(\"longitude\");").append(Util.LINE_SEPARATOR);
    addressBuilder.append("}").append(Util.LINE_SEPARATOR);
    addressBuilder.append(Util.LINE_SEPARATOR);
    addressBuilder.append("void address() {").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  define(Address.TYPE,").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.ADDRESSID)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .primaryKeyIndex(0),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.STREET1, \"Street1\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.STREET2, \"Street2\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.CITY, \"City\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(55),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.STATE, \"State\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumLength(25),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.ZIP, \"Zip\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.LATITUDE, \"Latitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumFractionDigits(2),").append(Util.LINE_SEPARATOR);
    addressBuilder.append("          columnProperty(Address.LONGITUDE, \"Longitude\")").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("                .maximumFractionDigits(2)").append(Util.LINE_SEPARATOR);
    addressBuilder.append("  );").append(Util.LINE_SEPARATOR);
    addressBuilder.append("}");

    ADDRESS_DEF = addressBuilder.toString();

    final StringBuilder tagItemBuilder = new StringBuilder();
    tagItemBuilder.append("public interface TagItem {").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  EntityType<Entity> TYPE = DOMAIN.entityType(\"petstore.tag_item\");").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  Attribute<Integer> TAGID = TYPE.integerAttribute(\"tagid\");").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  Attribute<Entity> TAGID_FK = TYPE.entityAttribute(\"tagid_fk\");").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  Attribute<Integer> ITEMID = TYPE.integerAttribute(\"itemid\");").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  Attribute<Entity> ITEMID_FK = TYPE.entityAttribute(\"itemid_fk\");").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("}").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("void tagItem() {").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  define(TagItem.TYPE,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("          foreignKeyProperty(TagItem.TAGID_FK, \"Tagid\", Tag.TYPE,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                columnProperty(TagItem.TAGID)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                        .primaryKeyIndex(0))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("          foreignKeyProperty(TagItem.ITEMID_FK, \"Itemid\", Item.TYPE,").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                columnProperty(TagItem.ITEMID)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                        .primaryKeyIndex(1))").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("  );").append(Util.LINE_SEPARATOR);
    tagItemBuilder.append("}");

    TAG_ITEM_DEF = tagItemBuilder.toString();

    final StringBuilder productBuilder = new StringBuilder();
    productBuilder.append("public interface Product {").append(Util.LINE_SEPARATOR);
    productBuilder.append("  EntityType<Entity> TYPE = DOMAIN.entityType(\"petstore.product\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<Integer> PRODUCTID = TYPE.integerAttribute(\"productid\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<Integer> CATEGORYID = TYPE.integerAttribute(\"categoryid\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<Entity> CATEGORYID_FK = TYPE.entityAttribute(\"categoryid_fk\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<String> NAME = TYPE.stringAttribute(\"name\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<String> DESCRIPTION = TYPE.stringAttribute(\"description\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("  Attribute<String> IMAGEURL = TYPE.stringAttribute(\"imageurl\");").append(Util.LINE_SEPARATOR);
    productBuilder.append("}").append(Util.LINE_SEPARATOR);
    productBuilder.append(Util.LINE_SEPARATOR);
    productBuilder.append("void product() {").append(Util.LINE_SEPARATOR);
    productBuilder.append("  define(Product.TYPE,").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(Product.PRODUCTID)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .primaryKeyIndex(0),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          foreignKeyProperty(Product.CATEGORYID_FK, \"Categoryid\", Category.TYPE,").append(Util.LINE_SEPARATOR);
    productBuilder.append("                columnProperty(Product.CATEGORYID))").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(Product.NAME, \"Name\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(25),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(Product.DESCRIPTION, \"Description\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .nullable(false)").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(255),").append(Util.LINE_SEPARATOR);
    productBuilder.append("          columnProperty(Product.IMAGEURL, \"Imageurl\")").append(Util.LINE_SEPARATOR);
    productBuilder.append("                .maximumLength(55)").append(Util.LINE_SEPARATOR);
    productBuilder.append("  );").append(Util.LINE_SEPARATOR);
    productBuilder.append("}");

    PRODUCT_DEF = productBuilder.toString();
  }

  private EntityGeneratorModel model;

  @BeforeEach
  public void setUp() throws Exception {
    model = new EntityGeneratorModel(UNIT_TEST_USER);
    model.getSchemaModel().getSelectionModel().setSelectedIndex(model.getSchemaModel().indexOf(new EntityGeneratorModel.Schema("PETSTORE")));
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
