/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;

import java.sql.Types;

public class Petstore {

  private Petstore() {}
  public static void init() {}

  public static final String VERSION = "@petstore-v1";

  public static final String T_ADDRESS = "address" + VERSION;
  public static final String ADDRESS_ID = "addressid";
  public static final String ADDRESS_STREET_1 = "street1";
  public static final String ADDRESS_STREET_2 = "street2";
  public static final String ADDRESS_CITY = "city";
  public static final String ADDRESS_STATE = "state";
  public static final String ADDRESS_ZIP = "zip";
  public static final String ADDRESS_ZIP_FK = "zip_fk";
  public static final String ADDRESS_LATITUDE = "latitude";
  public static final String ADDRESS_LONGITUDE = "longitude";

  static {
    Entities.define(T_ADDRESS, "petstore.address",
            Properties.primaryKeyProperty(ADDRESS_ID),
            Properties.columnProperty(ADDRESS_STREET_1, Types.VARCHAR, "Street 1").setMaxLength(55).setNullable(false),
            Properties.columnProperty(ADDRESS_STREET_2, Types.VARCHAR, "Street 2").setMaxLength(55),
            Properties.columnProperty(ADDRESS_CITY, Types.VARCHAR, "City").setMaxLength(55).setNullable(false),
            Properties.columnProperty(ADDRESS_STATE, Types.VARCHAR, "State").setMaxLength(25).setNullable(false),
            Properties.columnProperty(ADDRESS_ZIP, Types.INTEGER, "Zip").setNullable(false),
            Properties.columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, "Latitude").setNullable(false).setMaximumFractionDigits(2),
            Properties.columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, "Longitude").setNullable(false).setMaximumFractionDigits(2))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.address", ADDRESS_ID))
            .setOrderByClause(ADDRESS_CITY + ", " + ADDRESS_STREET_1 + ", " + ADDRESS_STREET_2)
            .setStringProvider(new Entities.StringProvider(ADDRESS_STREET_1).addText(" ")
            .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
            .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE))
            .setCaption("Addresses");
  }

  public static final String T_CATEGORY = "category" + VERSION;
  public static final String CATEGORY_ID = "categoryid";
  public static final String CATEGORY_NAME = "name";
  public static final String CATEGORY_DESCRIPTION = "description";
  public static final String CATEGORY_IMAGE_URL = "imageurl";

  static {
    Entities.define(T_CATEGORY, "petstore.category",
            Properties.primaryKeyProperty(CATEGORY_ID),
            Properties.columnProperty(CATEGORY_NAME, Types.VARCHAR, "Name").setMaxLength(25).setNullable(false),
            Properties.columnProperty(CATEGORY_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(255).setNullable(false),
            Properties.columnProperty(CATEGORY_IMAGE_URL, Types.VARCHAR, "Image URL").setHidden(true))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.category", CATEGORY_ID))
            .setOrderByClause(CATEGORY_NAME)
            .setStringProvider(new Entities.StringProvider(CATEGORY_NAME))
            .setCaption("Categories");
  }

  public static final String T_PRODUCT = "product" + VERSION;
  public static final String PRODUCT_ID = "productid";
  public static final String PRODUCT_CATEGORY_ID = "categoryid";
  public static final String PRODUCT_CATEGORY_FK = "category_fk";
  public static final String PRODUCT_NAME = "name";
  public static final String PRODUCT_DESCRIPTION = "description";
  public static final String PRODUCT_IMAGE_URL = "imageurl";

  static {
    Entities.define(T_PRODUCT, "petstore.product",
            Properties.primaryKeyProperty(PRODUCT_ID),
            Properties.foreignKeyProperty(PRODUCT_CATEGORY_FK, "Category", T_CATEGORY,
                    Properties.columnProperty(PRODUCT_CATEGORY_ID)).setNullable(false),
            Properties.columnProperty(PRODUCT_NAME, Types.VARCHAR, "Name").setMaxLength(25).setNullable(false),
            Properties.columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(255).setNullable(false),
            Properties.columnProperty(PRODUCT_IMAGE_URL, Types.VARCHAR, "Image URL").setMaxLength(55).setHidden(true))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.product", PRODUCT_ID))
            .setOrderByClause(PRODUCT_NAME)
            .setStringProvider(new Entities.StringProvider(PRODUCT_CATEGORY_FK)
            .addText(" - ").addValue(PRODUCT_NAME))
            .setCaption("Products");
  }

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo" + VERSION;
  public static final String SELLER_CONTACT_INFO_ID = "contactinfoid";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "firstname";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "lastname";
  public static final String SELLER_CONTACT_INFO_EMAIL = "email";

  static {
    Entities.define(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            Properties.primaryKeyProperty(SELLER_CONTACT_INFO_ID),
            Properties.columnProperty(SELLER_CONTACT_INFO_FIRST_NAME, Types.VARCHAR, "First name").setMaxLength(24).setNullable(false),
            Properties.columnProperty(SELLER_CONTACT_INFO_LAST_NAME, Types.VARCHAR, "Last name").setMaxLength(24).setNullable(false),
            Properties.columnProperty(SELLER_CONTACT_INFO_EMAIL, Types.VARCHAR, "Email").setMaxLength(24).setNullable(false))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.sellercontactinfo", SELLER_CONTACT_INFO_ID))
            .setOrderByClause(SELLER_CONTACT_INFO_LAST_NAME + ", "+ SELLER_CONTACT_INFO_FIRST_NAME)
            .setStringProvider(new Entities.StringProvider(SELLER_CONTACT_INFO_LAST_NAME)
            .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME))
            .setCaption("Seller info");
  }

  public static final String T_ITEM = "item" + VERSION;
  public static final String ITEM_ID = "itemid";
  public static final String ITEM_PRODUCT_ID = "productid";
  public static final String ITEM_PRODUCT_FK = "product_fk";
  public static final String ITEM_NAME = "name";
  public static final String ITEM_DESCRIPTION = "description";
  public static final String ITEM_IMAGE_URL = "imageurl";
  public static final String ITEM_IMAGE_THUMB_URL = "imagethumburl";
  public static final String ITEM_PRICE = "price";
  public static final String ITEM_ADDRESS_ID = "address_addressid";
  public static final String ITEM_ADDRESS_FK = "address_fk";
  public static final String ITEM_C0NTACT_INFO_ID = "contactinfo_contactinfoid";
  public static final String ITEM_C0NTACT_INFO_FK = "contactinfo_fk";
  public static final String ITEM_DISABLED = "disabled";

  static {
    Entities.define(T_ITEM, "petstore.item",
            Properties.primaryKeyProperty(ITEM_ID),
            Properties.foreignKeyProperty(ITEM_PRODUCT_FK, "Product", T_PRODUCT,
                    Properties.columnProperty(ITEM_PRODUCT_ID)).setFetchDepth(2).setNullable(false),
            Properties.columnProperty(ITEM_NAME, Types.VARCHAR, "Name").setMaxLength(30).setNullable(false),
            Properties.columnProperty(ITEM_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(500).setNullable(false),
            Properties.columnProperty(ITEM_IMAGE_URL, Types.VARCHAR, "Image URL").setMaxLength(55).setHidden(true),
            Properties.columnProperty(ITEM_IMAGE_THUMB_URL, Types.VARCHAR, "Image thumbnail URL").setMaxLength(55).setHidden(true),
            Properties.columnProperty(ITEM_PRICE, Types.DOUBLE, "Price").setNullable(false).setMaximumFractionDigits(2),
            Properties.foreignKeyProperty(ITEM_C0NTACT_INFO_FK, "Contact info", T_SELLER_CONTACT_INFO,
                    Properties.columnProperty(ITEM_C0NTACT_INFO_ID)).setNullable(false),
            Properties.foreignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    Properties.columnProperty(ITEM_ADDRESS_ID)).setNullable(false),
            Properties.booleanProperty(ITEM_DISABLED, Types.INTEGER, "Disabled", 1, 0).setDefaultValue(false))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.item", ITEM_ID))
            .setOrderByClause(ITEM_NAME)
            .setStringProvider(new Entities.StringProvider(ITEM_PRODUCT_FK).addText(" - ").addValue(ITEM_NAME))
            .setCaption("Items");
  }

  public static final String T_TAG = "tag" + VERSION;
  public static final String TAG_ID = "tagid";
  public static final String TAG_TAG = "tag";
  public static final String TAG_REFCOUNT = "refcount";

  static {
    Entities.define(T_TAG, "petstore.tag",
            Properties.primaryKeyProperty(TAG_ID),
            Properties.columnProperty(TAG_TAG, Types.VARCHAR, "Tag").setMaxLength(30).setNullable(false),
            Properties.subqueryProperty(TAG_REFCOUNT, Types.INTEGER, "Reference count",
                    "select count(*) from petstore.tag_item  where tagid = tag.tagid"))
            .setKeyGenerator(Entities.incrementKeyGenerator("petstore.tag", TAG_ID))
            .setOrderByClause(TAG_TAG)
            .setSelectTableName("petstore.tag tag")
            .setStringProvider(new Entities.StringProvider(TAG_TAG))
            .setCaption("Tags");
  }

  public static final String T_TAG_ITEM = "tag_item" + VERSION;
  public static final String TAG_ITEM_TAG_ID = "tagid";
  public static final String TAG_ITEM_TAG_FK = "tag_fk";
  public static final String TAG_ITEM_ITEM_ID = "itemid";
  public static final String TAG_ITEM_ITEM_FK = "item_fk";

  static {
    Entities.define(T_TAG_ITEM, "petstore.tag_item",
            Properties.foreignKeyProperty(TAG_ITEM_ITEM_FK, "Item", T_ITEM,
                    Properties.primaryKeyProperty(TAG_ITEM_ITEM_ID, Types.INTEGER).setPrimaryKeyIndex(0)).setNullable(false),
            Properties.foreignKeyProperty(TAG_ITEM_TAG_FK, "Tag", T_TAG,
                    Properties.primaryKeyProperty(TAG_ITEM_TAG_ID, Types.INTEGER).setPrimaryKeyIndex(1)).setNullable(false))
            .setStringProvider(new Entities.StringProvider(TAG_ITEM_ITEM_FK).addText(" - ").addValue(TAG_ITEM_TAG_FK))
            .setCaption("Item tags");
  }
}
