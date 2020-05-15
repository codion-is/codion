/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;

import java.sql.Types;

import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

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
                    .columnName("addressid"),
            columnProperty(ADDRESS_STREET_1, Types.VARCHAR, ADDRESS_STREET_1)
                    .columnName("street1").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STREET_2, Types.VARCHAR, ADDRESS_STREET_2)
                    .columnName("street2").maximumLength(55),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, ADDRESS_CITY)
                    .columnName("city").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STATE, Types.VARCHAR, ADDRESS_STATE)
                    .columnName("state").maximumLength(25).nullable(false),
            columnProperty(ADDRESS_ZIP, Types.INTEGER, ADDRESS_ZIP)
                    .columnName("zip").nullable(false),
            columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, ADDRESS_LATITUDE)
                    .columnName("latitude").nullable(false).maximumFractionDigits(2),
            columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, ADDRESS_LONGITUDE)
                    .columnName("longitude").nullable(false).maximumFractionDigits(2))
            .keyGenerator(increment("petstore.address", "addressid"))
            .orderBy(orderBy().ascending(ADDRESS_CITY, ADDRESS_STREET_1, ADDRESS_STREET_2))
            .stringProvider(new StringProvider(ADDRESS_STREET_1).addText(" ")
                    .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
                    .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE))
            .caption("Addresses");
  }

  public static final String T_CATEGORY = "category";
  public static final String CATEGORY_ID = "Category id";
  public static final String CATEGORY_NAME = "Name";
  public static final String CATEGORY_DESCRIPTION = "Description";
  public static final String CATEGORY_IMAGE_URL = "Image URL";

  void category() {
    define(T_CATEGORY, "petstore.category",
            primaryKeyProperty(CATEGORY_ID)
                    .columnName("categoryid"),
            columnProperty(CATEGORY_NAME, Types.VARCHAR, CATEGORY_NAME)
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(CATEGORY_DESCRIPTION, Types.VARCHAR, CATEGORY_DESCRIPTION)
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(CATEGORY_IMAGE_URL, Types.VARCHAR, CATEGORY_IMAGE_URL)
                    .columnName("imageurl").hidden(true))
            .keyGenerator(increment("petstore.category", "categoryid"))
            .orderBy(orderBy().ascending(CATEGORY_NAME))
            .stringProvider(new StringProvider(CATEGORY_NAME))
            .caption("Categories");
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
                    .columnName("productid"),
            foreignKeyProperty(PRODUCT_CATEGORY_FK, PRODUCT_CATEGORY_FK, T_CATEGORY,
                    columnProperty(PRODUCT_CATEGORY_ID)
                            .columnName("categoryid")).nullable(false),
            columnProperty(PRODUCT_NAME, Types.VARCHAR, PRODUCT_NAME)
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, PRODUCT_DESCRIPTION)
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(PRODUCT_IMAGE_URL, Types.VARCHAR, PRODUCT_IMAGE_URL)
                    .columnName("imageurl").maximumLength(55).hidden(true))
            .keyGenerator(increment("petstore.product", "productid"))
            .orderBy(orderBy().ascending(PRODUCT_NAME))
            .stringProvider(new StringProvider(PRODUCT_CATEGORY_FK)
                    .addText(" - ").addValue(PRODUCT_NAME))
            .caption("Products");
  }

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo";
  public static final String SELLER_CONTACT_INFO_ID = "Contactinfo id";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "First name";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "Last name";
  public static final String SELLER_CONTACT_INFO_EMAIL = "Email";

  void sellerContactInfo() {
    define(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            primaryKeyProperty(SELLER_CONTACT_INFO_ID)
                    .columnName("contactinfoid"),
            columnProperty(SELLER_CONTACT_INFO_FIRST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_FIRST_NAME)
                    .searchProperty(true).columnName("firstname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_LAST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_LAST_NAME)
                    .searchProperty(true).columnName("lastname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_EMAIL, Types.VARCHAR, SELLER_CONTACT_INFO_EMAIL)
                    .columnName("email").maximumLength(24).nullable(false))
            .keyGenerator(increment("petstore.sellercontactinfo", "contactinfoid"))
            .orderBy(orderBy()
                    .ascending(SELLER_CONTACT_INFO_LAST_NAME, SELLER_CONTACT_INFO_FIRST_NAME))
            .stringProvider(new StringProvider(SELLER_CONTACT_INFO_LAST_NAME)
                    .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME))
            .caption("Seller info");
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
                    .columnName("itemid"),
            foreignKeyProperty(ITEM_PRODUCT_FK, ITEM_PRODUCT_FK, T_PRODUCT,
                    columnProperty(ITEM_PRODUCT_ID)
                            .columnName("productid"))
                    .fetchDepth(2).nullable(false),
            columnProperty(ITEM_NAME, Types.VARCHAR, ITEM_NAME)
                    .columnName("name").maximumLength(30).nullable(false),
            columnProperty(ITEM_DESCRIPTION, Types.VARCHAR, ITEM_DESCRIPTION)
                    .columnName("description").maximumLength(500).nullable(false),
            columnProperty(ITEM_IMAGE_URL, Types.VARCHAR, ITEM_IMAGE_URL)
                    .columnName("imageurl").maximumLength(55).hidden(true),
            columnProperty(ITEM_IMAGE_THUMB_URL, Types.VARCHAR, ITEM_IMAGE_THUMB_URL)
                    .columnName("imagethumburl").maximumLength(55).hidden(true),
            columnProperty(ITEM_PRICE, Types.DECIMAL, ITEM_PRICE)
                    .columnName("price").nullable(false).maximumFractionDigits(2),
            foreignKeyProperty(ITEM_C0NTACT_INFO_FK, ITEM_C0NTACT_INFO_FK, T_SELLER_CONTACT_INFO,
                    columnProperty(ITEM_C0NTACT_INFO_ID).columnName("contactinfo_contactinfoid"))
                    .nullable(false),
            foreignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(ITEM_ADDRESS_ID).columnName("address_addressid"))
                    .nullable(false),
            booleanProperty(ITEM_DISABLED, Types.INTEGER, ITEM_DISABLED, 1, 0)
                    .columnName("disabled").defaultValue(false))
            .keyGenerator(increment("petstore.item", "itemid"))
            .orderBy(orderBy().ascending(ITEM_NAME))
            .stringProvider(new StringProvider(ITEM_PRODUCT_FK)
                    .addText(" - ").addValue(ITEM_NAME))
            .caption("Items");
  }

  public static final String T_TAG = "tag";
  public static final String TAG_ID = "Tag id";
  public static final String TAG_TAG = "Tag";
  public static final String TAG_REFCOUNT = "Reference count";

  void tag() {
    define(T_TAG, "petstore.tag",
            primaryKeyProperty(TAG_ID)
                    .columnName("tagid"),
            columnProperty(TAG_TAG, Types.VARCHAR, TAG_TAG)
                    .columnName("tag").maximumLength(30).nullable(false),
            subqueryProperty(TAG_REFCOUNT, Types.INTEGER, TAG_REFCOUNT,
                    "select count(*) from petstore.tag_item where tagid = tag.tagid")
                    .columnName("refcount"))
            .keyGenerator(increment("petstore.tag", "tagid"))
            .orderBy(orderBy().ascending(TAG_TAG))
            .selectTableName("petstore.tag tag")
            .stringProvider(new StringProvider(TAG_TAG))
            .caption("Tags");
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
                            .columnName("itemid").primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(TAG_ITEM_TAG_FK, TAG_ITEM_TAG_FK, T_TAG,
                    primaryKeyProperty(TAG_ITEM_TAG_ID)
                            .columnName("tagid").primaryKeyIndex(1))
                    .nullable(false))
            .stringProvider(new StringProvider(TAG_ITEM_ITEM_FK)
                    .addText(" - ").addValue(TAG_ITEM_TAG_FK))
            .caption("Item tags");
  }
}
