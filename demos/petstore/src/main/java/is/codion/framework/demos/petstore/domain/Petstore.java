/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;

import java.math.BigDecimal;
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
  public static final Attribute<Integer> ADDRESS_ID = attribute("Address id");
  public static final Attribute<String> ADDRESS_STREET_1 = attribute("Street 1");
  public static final Attribute<String> ADDRESS_STREET_2 = attribute("Street 2");
  public static final Attribute<String> ADDRESS_CITY = attribute("City");
  public static final Attribute<String> ADDRESS_STATE = attribute("State");
  public static final Attribute<Integer> ADDRESS_ZIP = attribute("Zip");
  public static final Attribute<Double> ADDRESS_LATITUDE = attribute("Latitude");
  public static final Attribute<Double> ADDRESS_LONGITUDE = attribute("Longitude");

  void address() {
    define(T_ADDRESS, "petstore.address",
            primaryKeyProperty(ADDRESS_ID, Types.INTEGER)
                    .columnName("addressid"),
            columnProperty(ADDRESS_STREET_1, Types.VARCHAR, ADDRESS_STREET_1.getId())
                    .columnName("street1").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STREET_2, Types.VARCHAR, ADDRESS_STREET_2.getId())
                    .columnName("street2").maximumLength(55),
            columnProperty(ADDRESS_CITY, Types.VARCHAR, ADDRESS_CITY.getId())
                    .columnName("city").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STATE, Types.VARCHAR, ADDRESS_STATE.getId())
                    .columnName("state").maximumLength(25).nullable(false),
            columnProperty(ADDRESS_ZIP, Types.INTEGER, ADDRESS_ZIP.getId())
                    .columnName("zip").nullable(false),
            columnProperty(ADDRESS_LATITUDE, Types.DOUBLE, ADDRESS_LATITUDE.getId())
                    .columnName("latitude").nullable(false).maximumFractionDigits(2),
            columnProperty(ADDRESS_LONGITUDE, Types.DOUBLE, ADDRESS_LONGITUDE.getId())
                    .columnName("longitude").nullable(false).maximumFractionDigits(2))
            .keyGenerator(increment("petstore.address", "addressid"))
            .orderBy(orderBy().ascending(ADDRESS_CITY, ADDRESS_STREET_1, ADDRESS_STREET_2))
            .stringProvider(new StringProvider(ADDRESS_STREET_1).addText(" ")
                    .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
                    .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE))
            .caption("Addresses");
  }

  public static final String T_CATEGORY = "category";
  public static final Attribute<Integer> CATEGORY_ID = attribute("Category id");
  public static final Attribute<String> CATEGORY_NAME = attribute("Name");
  public static final Attribute<String> CATEGORY_DESCRIPTION = attribute("Description");
  public static final Attribute<String> CATEGORY_IMAGE_URL = attribute("Image URL");

  void category() {
    define(T_CATEGORY, "petstore.category",
            primaryKeyProperty(CATEGORY_ID, Types.INTEGER)
                    .columnName("categoryid"),
            columnProperty(CATEGORY_NAME, Types.VARCHAR, CATEGORY_NAME.getId())
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(CATEGORY_DESCRIPTION, Types.VARCHAR, CATEGORY_DESCRIPTION.getId())
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(CATEGORY_IMAGE_URL, Types.VARCHAR, CATEGORY_IMAGE_URL.getId())
                    .columnName("imageurl").hidden(true))
            .keyGenerator(increment("petstore.category", "categoryid"))
            .orderBy(orderBy().ascending(CATEGORY_NAME))
            .stringProvider(new StringProvider(CATEGORY_NAME))
            .caption("Categories");
  }

  public static final String T_PRODUCT = "product";
  public static final Attribute<Integer> PRODUCT_ID = attribute("Product id");
  public static final Attribute<Integer> PRODUCT_CATEGORY_ID = attribute("Category id");
  public static final Attribute<Entity> PRODUCT_CATEGORY_FK = attribute("Category");
  public static final Attribute<String> PRODUCT_NAME = attribute("Name");
  public static final Attribute<String> PRODUCT_DESCRIPTION = attribute("Description");
  public static final Attribute<String> PRODUCT_IMAGE_URL = attribute("Image URL");

  void product() {
    define(T_PRODUCT, "petstore.product",
            primaryKeyProperty(PRODUCT_ID, Types.INTEGER)
                    .columnName("productid"),
            foreignKeyProperty(PRODUCT_CATEGORY_FK, PRODUCT_CATEGORY_FK.getId(), T_CATEGORY,
                    columnProperty(PRODUCT_CATEGORY_ID, Types.INTEGER)
                            .columnName("categoryid")).nullable(false),
            columnProperty(PRODUCT_NAME, Types.VARCHAR, PRODUCT_NAME.getId())
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(PRODUCT_DESCRIPTION, Types.VARCHAR, PRODUCT_DESCRIPTION.getId())
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(PRODUCT_IMAGE_URL, Types.VARCHAR, PRODUCT_IMAGE_URL.getId())
                    .columnName("imageurl").maximumLength(55).hidden(true))
            .keyGenerator(increment("petstore.product", "productid"))
            .orderBy(orderBy().ascending(PRODUCT_NAME))
            .stringProvider(new StringProvider(PRODUCT_CATEGORY_FK)
                    .addText(" - ").addValue(PRODUCT_NAME))
            .caption("Products");
  }

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo";
  public static final Attribute<Integer> SELLER_CONTACT_INFO_ID = attribute("Contactinfo id");
  public static final Attribute<String> SELLER_CONTACT_INFO_FIRST_NAME = attribute("First name");
  public static final Attribute<String> SELLER_CONTACT_INFO_LAST_NAME = attribute("Last name");
  public static final Attribute<String> SELLER_CONTACT_INFO_EMAIL = attribute("Email");

  void sellerContactInfo() {
    define(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            primaryKeyProperty(SELLER_CONTACT_INFO_ID, Types.INTEGER)
                    .columnName("contactinfoid"),
            columnProperty(SELLER_CONTACT_INFO_FIRST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_FIRST_NAME.getId())
                    .searchProperty(true).columnName("firstname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_LAST_NAME, Types.VARCHAR, SELLER_CONTACT_INFO_LAST_NAME.getId())
                    .searchProperty(true).columnName("lastname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_EMAIL, Types.VARCHAR, SELLER_CONTACT_INFO_EMAIL.getId())
                    .columnName("email").maximumLength(24).nullable(false))
            .keyGenerator(increment("petstore.sellercontactinfo", "contactinfoid"))
            .orderBy(orderBy()
                    .ascending(SELLER_CONTACT_INFO_LAST_NAME, SELLER_CONTACT_INFO_FIRST_NAME))
            .stringProvider(new StringProvider(SELLER_CONTACT_INFO_LAST_NAME)
                    .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME))
            .caption("Seller info");
  }

  public static final String T_ITEM = "item";
  public static final Attribute<Integer> ITEM_ID = attribute("Item id");
  public static final Attribute<Integer> ITEM_PRODUCT_ID = attribute("Product id");
  public static final Attribute<Entity> ITEM_PRODUCT_FK = attribute("Product");
  public static final Attribute<String> ITEM_NAME = attribute("Name");
  public static final Attribute<String> ITEM_DESCRIPTION = attribute("Description");
  public static final Attribute<String> ITEM_IMAGE_URL = attribute("Image URL");
  public static final Attribute<String> ITEM_IMAGE_THUMB_URL = attribute("Image thumbnail URL");
  public static final Attribute<BigDecimal> ITEM_PRICE = attribute("Price");
  public static final Attribute<Integer> ITEM_C0NTACT_INFO_ID = attribute("Contactinfo id");
  public static final Attribute<Entity> ITEM_C0NTACT_INFO_FK = attribute("Contact info");
  public static final Attribute<Integer> ITEM_ADDRESS_ID = attribute("Address id");
  public static final Attribute<Entity> ITEM_ADDRESS_FK = attribute("Address");
  public static final Attribute<Boolean> ITEM_DISABLED = attribute("Disabled");

  void item() {
    define(T_ITEM, "petstore.item",
            primaryKeyProperty(ITEM_ID, Types.INTEGER)
                    .columnName("itemid"),
            foreignKeyProperty(ITEM_PRODUCT_FK, ITEM_PRODUCT_FK.getId(), T_PRODUCT,
                    columnProperty(ITEM_PRODUCT_ID, Types.INTEGER)
                            .columnName("productid"))
                    .fetchDepth(2).nullable(false),
            columnProperty(ITEM_NAME, Types.VARCHAR, ITEM_NAME.getId())
                    .columnName("name").maximumLength(30).nullable(false),
            columnProperty(ITEM_DESCRIPTION, Types.VARCHAR, ITEM_DESCRIPTION.getId())
                    .columnName("description").maximumLength(500).nullable(false),
            columnProperty(ITEM_IMAGE_URL, Types.VARCHAR, ITEM_IMAGE_URL.getId())
                    .columnName("imageurl").maximumLength(55).hidden(true),
            columnProperty(ITEM_IMAGE_THUMB_URL, Types.VARCHAR, ITEM_IMAGE_THUMB_URL.getId())
                    .columnName("imagethumburl").maximumLength(55).hidden(true),
            columnProperty(ITEM_PRICE, Types.DECIMAL, ITEM_PRICE.getId())
                    .columnName("price").nullable(false).maximumFractionDigits(2),
            foreignKeyProperty(ITEM_C0NTACT_INFO_FK, ITEM_C0NTACT_INFO_FK.getId(), T_SELLER_CONTACT_INFO,
                    columnProperty(ITEM_C0NTACT_INFO_ID, Types.INTEGER).columnName("contactinfo_contactinfoid"))
                    .nullable(false),
            foreignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(ITEM_ADDRESS_ID, Types.INTEGER).columnName("address_addressid"))
                    .nullable(false),
            booleanProperty(ITEM_DISABLED, Types.INTEGER, ITEM_DISABLED.getId(), 1, 0)
                    .columnName("disabled").defaultValue(false))
            .keyGenerator(increment("petstore.item", "itemid"))
            .orderBy(orderBy().ascending(ITEM_NAME))
            .stringProvider(new StringProvider(ITEM_PRODUCT_FK)
                    .addText(" - ").addValue(ITEM_NAME))
            .caption("Items");
  }

  public static final String T_TAG = "tag";
  public static final Attribute<Integer> TAG_ID = attribute("Tag id");
  public static final Attribute<String> TAG_TAG = attribute("Tag");
  public static final Attribute<Integer> TAG_REFCOUNT = attribute("Reference count");

  void tag() {
    define(T_TAG, "petstore.tag",
            primaryKeyProperty(TAG_ID, Types.INTEGER)
                    .columnName("tagid"),
            columnProperty(TAG_TAG, Types.VARCHAR, TAG_TAG.getId())
                    .columnName("tag").maximumLength(30).nullable(false),
            subqueryProperty(TAG_REFCOUNT, Types.INTEGER, TAG_REFCOUNT.getId(),
                    "select count(*) from petstore.tag_item where tagid = tag.tagid")
                    .columnName("refcount"))
            .keyGenerator(increment("petstore.tag", "tagid"))
            .orderBy(orderBy().ascending(TAG_TAG))
            .selectTableName("petstore.tag tag")
            .stringProvider(new StringProvider(TAG_TAG))
            .caption("Tags");
  }

  public static final String T_TAG_ITEM = "tag_item";
  public static final Attribute<Integer> TAG_ITEM_ITEM_ID = attribute("Item id");
  public static final Attribute<Entity> TAG_ITEM_ITEM_FK = attribute("Item");
  public static final Attribute<Integer> TAG_ITEM_TAG_ID = attribute("Tag id");
  public static final Attribute<Entity> TAG_ITEM_TAG_FK = attribute("Tag");

  void tagItem() {
    define(T_TAG_ITEM, "petstore.tag_item",
            foreignKeyProperty(TAG_ITEM_ITEM_FK, TAG_ITEM_ITEM_FK.getId(), T_ITEM,
                    primaryKeyProperty(TAG_ITEM_ITEM_ID, Types.INTEGER)
                            .columnName("itemid").primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(TAG_ITEM_TAG_FK, TAG_ITEM_TAG_FK.getId(), T_TAG,
                    primaryKeyProperty(TAG_ITEM_TAG_ID, Types.INTEGER)
                            .columnName("tagid").primaryKeyIndex(1))
                    .nullable(false))
            .stringProvider(new StringProvider(TAG_ITEM_ITEM_FK)
                    .addText(" - ").addValue(TAG_ITEM_TAG_FK))
            .caption("Item tags");
  }
}
