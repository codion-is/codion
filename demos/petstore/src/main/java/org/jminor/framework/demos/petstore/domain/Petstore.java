/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.StringProvider;

import java.sql.Types;

import static org.jminor.framework.domain.KeyGenerators.increment;
import static org.jminor.framework.domain.property.Properties.*;

public final class Petstore extends Domain {

  public Petstore() {
    address();
    category();
    product();
    sellerContactInfo();
    item();
    tag();
    tagItem();
  }

  public static final String T_ADDRESS = "address";
  public static final String ADDRESS_ID = "Address id";
  public static final String ADDRESS_STREET_1 = "Street 1";
  public static final String ADDRESS_STREET_2 = "Street 2";
  public static final String ADDRESS_CITY = "City";
  public static final String ADDRESS_STATE = "State";
  public static final String ADDRESS_ZIP = "Zip";
  public static final String ADDRESS_LATITUDE = "Latitude";
  public static final String ADDRESS_LONGITUDE = "Longitude";

  void address() {
    define(T_ADDRESS, "petstore.address",
            primaryKeyProperty(ADDRESS_ID)
                    .setColumnName("addressid"),
            columnProperty(ADDRESS_STREET_1, Types.VARCHAR, ADDRESS_STREET_1)
                    .setColumnName("street1").setMaxLength(55).setNullable(false),
            columnProperty(ADDRESS_STREET_2, Types.VARCHAR, ADDRESS_STREET_2)
                    .setColumnName("street2").setMaxLength(55),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, ADDRESS_CITY)
                    .setColumnName("city").setMaxLength(55).setNullable(false),
            columnProperty(ADDRESS_STATE, Types.VARCHAR, ADDRESS_STATE)
                    .setColumnName("state").setMaxLength(25).setNullable(false),
            columnProperty(ADDRESS_ZIP, Types.INTEGER, ADDRESS_ZIP)
                    .setColumnName("zip").setNullable(false),
            columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, ADDRESS_LATITUDE)
                    .setColumnName("latitude").setNullable(false).setMaximumFractionDigits(2),
            columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, ADDRESS_LONGITUDE)
                    .setColumnName("longitude").setNullable(false).setMaximumFractionDigits(2))
            .setKeyGenerator(increment("petstore.address", "addressid"))
            .setOrderBy(orderBy().ascending(ADDRESS_CITY, ADDRESS_STREET_1, ADDRESS_STREET_2))
            .setStringProvider(new StringProvider(ADDRESS_STREET_1).addText(" ")
                    .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
                    .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE))
            .setCaption("Addresses");
  }

  public static final String T_CATEGORY = "category";
  public static final String CATEGORY_ID = "Category id";
  public static final String CATEGORY_NAME = "Name";
  public static final String CATEGORY_DESCRIPTION = "Description";
  public static final String CATEGORY_IMAGE_URL = "Image URL";

  void category() {
    define(T_CATEGORY, "petstore.category",
            primaryKeyProperty(CATEGORY_ID)
                    .setColumnName("categoryid"),
            columnProperty(CATEGORY_NAME, Types.VARCHAR, CATEGORY_NAME)
                    .setColumnName("name").setMaxLength(25).setNullable(false),
            columnProperty(CATEGORY_DESCRIPTION, Types.VARCHAR, CATEGORY_DESCRIPTION)
                    .setColumnName("description").setMaxLength(255).setNullable(false),
            columnProperty(CATEGORY_IMAGE_URL, Types.VARCHAR, CATEGORY_IMAGE_URL)
                    .setColumnName("imageurl").setHidden(true))
            .setKeyGenerator(increment("petstore.category", "categoryid"))
            .setOrderBy(orderBy().ascending(CATEGORY_NAME))
            .setStringProvider(new StringProvider(CATEGORY_NAME))
            .setCaption("Categories");
  }

  public static final String T_PRODUCT = "product";
  public static final String PRODUCT_ID = "Product id";
  public static final String PRODUCT_CATEGORY_ID = "Category id";
  public static final String PRODUCT_CATEGORY_FK = "Category";
  public static final String PRODUCT_NAME = "Name";
  public static final String PRODUCT_DESCRIPTION = "Description";
  public static final String PRODUCT_IMAGE_URL = "Image URL";

  void product() {
    define(T_PRODUCT, "petstore.product",
            primaryKeyProperty(PRODUCT_ID)
                    .setColumnName("productid"),
            foreignKeyProperty(PRODUCT_CATEGORY_FK, PRODUCT_CATEGORY_FK, T_CATEGORY,
                    columnProperty(PRODUCT_CATEGORY_ID)
                            .setColumnName("categoryid")).setNullable(false),
            columnProperty(PRODUCT_NAME, Types.VARCHAR, PRODUCT_NAME)
                    .setColumnName("name").setMaxLength(25).setNullable(false),
            columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, PRODUCT_DESCRIPTION)
                    .setColumnName("description").setMaxLength(255).setNullable(false),
            columnProperty(PRODUCT_IMAGE_URL, Types.VARCHAR, PRODUCT_IMAGE_URL)
                    .setColumnName("imageurl").setMaxLength(55).setHidden(true))
            .setKeyGenerator(increment("petstore.product", "productid"))
            .setOrderBy(orderBy().ascending(PRODUCT_NAME))
            .setStringProvider(new StringProvider(PRODUCT_CATEGORY_FK)
                    .addText(" - ").addValue(PRODUCT_NAME))
            .setCaption("Products");
  }

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo";
  public static final String SELLER_CONTACT_INFO_ID = "Contactinfo id";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "First name";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "Last name";
  public static final String SELLER_CONTACT_INFO_EMAIL = "Email";

  void sellerContactInfo() {
    define(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            primaryKeyProperty(SELLER_CONTACT_INFO_ID)
                    .setColumnName("contactinfoid"),
            columnProperty(SELLER_CONTACT_INFO_FIRST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_FIRST_NAME)
                    .setColumnName("firstname").setMaxLength(24).setNullable(false),
            columnProperty(SELLER_CONTACT_INFO_LAST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_LAST_NAME)
                    .setColumnName("lastname").setMaxLength(24).setNullable(false),
            columnProperty(SELLER_CONTACT_INFO_EMAIL, Types.VARCHAR, SELLER_CONTACT_INFO_EMAIL)
                    .setColumnName("email").setMaxLength(24).setNullable(false))
            .setKeyGenerator(increment("petstore.sellercontactinfo", "contactinfoid"))
            .setOrderBy(orderBy()
                    .ascending(SELLER_CONTACT_INFO_LAST_NAME, SELLER_CONTACT_INFO_FIRST_NAME))
            .setStringProvider(new StringProvider(SELLER_CONTACT_INFO_LAST_NAME)
                    .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME))
            .setCaption("Seller info");
  }

  public static final String T_ITEM = "item";
  public static final String ITEM_ID = "Item id";
  public static final String ITEM_PRODUCT_ID = "Product id";
  public static final String ITEM_PRODUCT_FK = "Product";
  public static final String ITEM_NAME = "Name";
  public static final String ITEM_DESCRIPTION = "Description";
  public static final String ITEM_IMAGE_URL = "Image URL";
  public static final String ITEM_IMAGE_THUMB_URL = "Image thumbnail URL";
  public static final String ITEM_PRICE = "Price";
  public static final String ITEM_C0NTACT_INFO_ID = "Contactinfo id";
  public static final String ITEM_C0NTACT_INFO_FK = "Contact info";
  public static final String ITEM_ADDRESS_ID = "Address id";
  public static final String ITEM_ADDRESS_FK = "Address";
  public static final String ITEM_DISABLED = "Disabled";

  void item() {
    define(T_ITEM, "petstore.item",
            primaryKeyProperty(ITEM_ID)
                    .setColumnName("itemid"),
            foreignKeyProperty(ITEM_PRODUCT_FK, ITEM_PRODUCT_FK, T_PRODUCT,
                    columnProperty(ITEM_PRODUCT_ID)
                            .setColumnName("productid"))
                    .setFetchDepth(2).setNullable(false),
            columnProperty(ITEM_NAME, Types.VARCHAR, ITEM_NAME)
                    .setColumnName("name").setMaxLength(30).setNullable(false),
            columnProperty(ITEM_DESCRIPTION, Types.VARCHAR, ITEM_DESCRIPTION)
                    .setColumnName("description").setMaxLength(500).setNullable(false),
            columnProperty(ITEM_IMAGE_URL, Types.VARCHAR, ITEM_IMAGE_URL)
                    .setColumnName("imageurl").setMaxLength(55).setHidden(true),
            columnProperty(ITEM_IMAGE_THUMB_URL, Types.VARCHAR, ITEM_IMAGE_THUMB_URL)
                    .setColumnName("imagethumburl").setMaxLength(55).setHidden(true),
            columnProperty(ITEM_PRICE, Types.DECIMAL, ITEM_PRICE)
                    .setColumnName("price").setNullable(false).setMaximumFractionDigits(2),
            foreignKeyProperty(ITEM_C0NTACT_INFO_FK, ITEM_C0NTACT_INFO_FK, T_SELLER_CONTACT_INFO,
                    columnProperty(ITEM_C0NTACT_INFO_ID).setColumnName("contactinfo_contactinfoid"))
                    .setNullable(false),
            foreignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(ITEM_ADDRESS_ID).setColumnName("address_addressid"))
                    .setNullable(false),
            booleanProperty(ITEM_DISABLED, Types.INTEGER, ITEM_DISABLED, 1, 0)
                    .setColumnName("disabled").setDefaultValue(false))
            .setKeyGenerator(increment("petstore.item", "itemid"))
            .setOrderBy(orderBy().ascending(ITEM_NAME))
            .setStringProvider(new StringProvider(ITEM_PRODUCT_FK)
                    .addText(" - ").addValue(ITEM_NAME))
            .setCaption("Items");
  }

  public static final String T_TAG = "tag";
  public static final String TAG_ID = "Tag id";
  public static final String TAG_TAG = "Tag";
  public static final String TAG_REFCOUNT = "Reference count";

  void tag() {
    define(T_TAG, "petstore.tag",
            primaryKeyProperty(TAG_ID)
                    .setColumnName("tagid"),
            columnProperty(TAG_TAG, Types.VARCHAR, TAG_TAG)
                    .setColumnName("tag").setMaxLength(30).setNullable(false),
            subqueryProperty(TAG_REFCOUNT, Types.INTEGER, TAG_REFCOUNT,
                    "select count(*) from petstore.tag_item where tagid = tag.tagid")
                    .setColumnName("refcount"))
            .setKeyGenerator(increment("petstore.tag", "tagid"))
            .setOrderBy(orderBy().ascending(TAG_TAG))
            .setSelectTableName("petstore.tag tag")
            .setStringProvider(new StringProvider(TAG_TAG))
            .setCaption("Tags");
  }

  public static final String T_TAG_ITEM = "tag_item";
  public static final String TAG_ITEM_ITEM_ID = "Item id";
  public static final String TAG_ITEM_ITEM_FK = "Item";
  public static final String TAG_ITEM_TAG_ID = "Tag id";
  public static final String TAG_ITEM_TAG_FK = "Tag";

  void tagItem() {
    define(T_TAG_ITEM, "petstore.tag_item",
            foreignKeyProperty(TAG_ITEM_ITEM_FK, TAG_ITEM_ITEM_FK, T_ITEM,
                    primaryKeyProperty(TAG_ITEM_ITEM_ID)
                            .setColumnName("itemid").setPrimaryKeyIndex(0))
                    .setNullable(false),
            foreignKeyProperty(TAG_ITEM_TAG_FK, TAG_ITEM_TAG_FK, T_TAG,
                    primaryKeyProperty(TAG_ITEM_TAG_ID)
                            .setColumnName("tagid").setPrimaryKeyIndex(1))
                    .setNullable(false))
            .setStringProvider(new StringProvider(TAG_ITEM_ITEM_FK)
                    .addText(" - ").addValue(TAG_ITEM_TAG_FK))
            .setCaption("Item tags");
  }
}
