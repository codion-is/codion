/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;

import java.math.BigDecimal;

import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Attributes.*;
import static is.codion.framework.domain.property.Properties.*;
import static java.sql.Types.INTEGER;

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
  public static final Attribute<Integer> ADDRESS_ID = integerAttribute("Address id", T_ADDRESS);
  public static final Attribute<String> ADDRESS_STREET_1 = stringAttribute("Street 1", T_ADDRESS);
  public static final Attribute<String> ADDRESS_STREET_2 = stringAttribute("Street 2", T_ADDRESS);
  public static final Attribute<String> ADDRESS_CITY = stringAttribute("City", T_ADDRESS);
  public static final Attribute<String> ADDRESS_STATE = stringAttribute("State", T_ADDRESS);
  public static final Attribute<Integer> ADDRESS_ZIP = integerAttribute("Zip", T_ADDRESS);
  public static final Attribute<Double> ADDRESS_LATITUDE = doubleAttribute("Latitude", T_ADDRESS);
  public static final Attribute<Double> ADDRESS_LONGITUDE = doubleAttribute("Longitude", T_ADDRESS);

  void address() {
    define(T_ADDRESS, "petstore.address",
            primaryKeyProperty(ADDRESS_ID)
                    .columnName("addressid"),
            columnProperty(ADDRESS_STREET_1, ADDRESS_STREET_1.getName())
                    .columnName("street1").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STREET_2, ADDRESS_STREET_2.getName())
                    .columnName("street2").maximumLength(55),
            columnProperty(ADDRESS_CITY, ADDRESS_CITY.getName())
                    .columnName("city").maximumLength(55).nullable(false),
            columnProperty(ADDRESS_STATE, ADDRESS_STATE.getName())
                    .columnName("state").maximumLength(25).nullable(false),
            columnProperty(ADDRESS_ZIP, ADDRESS_ZIP.getName())
                    .columnName("zip").nullable(false),
            columnProperty(ADDRESS_LATITUDE, ADDRESS_LATITUDE.getName())
                    .columnName("latitude").nullable(false).maximumFractionDigits(2),
            columnProperty(ADDRESS_LONGITUDE, ADDRESS_LONGITUDE.getName())
                    .columnName("longitude").nullable(false).maximumFractionDigits(2))
            .keyGenerator(increment("petstore.address", "addressid"))
            .orderBy(orderBy().ascending(ADDRESS_CITY, ADDRESS_STREET_1, ADDRESS_STREET_2))
            .stringProvider(new StringProvider(ADDRESS_STREET_1).addText(" ")
                    .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
                    .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE))
            .caption("Addresses");
  }

  public static final String T_CATEGORY = "category";
  public static final Attribute<Integer> CATEGORY_ID = integerAttribute("Category id", T_CATEGORY);
  public static final Attribute<String> CATEGORY_NAME = stringAttribute("Name", T_CATEGORY);
  public static final Attribute<String> CATEGORY_DESCRIPTION = stringAttribute("Description", T_CATEGORY);
  public static final Attribute<String> CATEGORY_IMAGE_URL = stringAttribute("Image URL", T_CATEGORY);

  void category() {
    define(T_CATEGORY, "petstore.category",
            primaryKeyProperty(CATEGORY_ID)
                    .columnName("categoryid"),
            columnProperty(CATEGORY_NAME, CATEGORY_NAME.getName())
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(CATEGORY_DESCRIPTION, CATEGORY_DESCRIPTION.getName())
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(CATEGORY_IMAGE_URL, CATEGORY_IMAGE_URL.getName())
                    .columnName("imageurl").hidden(true))
            .keyGenerator(increment("petstore.category", "categoryid"))
            .orderBy(orderBy().ascending(CATEGORY_NAME))
            .stringProvider(new StringProvider(CATEGORY_NAME))
            .caption("Categories");
  }

  public static final String T_PRODUCT = "product";
  public static final Attribute<Integer> PRODUCT_ID = integerAttribute("Product id", T_PRODUCT);
  public static final Attribute<Integer> PRODUCT_CATEGORY_ID = integerAttribute("Category id", T_PRODUCT);
  public static final EntityAttribute PRODUCT_CATEGORY_FK = entityAttribute("Category", T_PRODUCT);
  public static final Attribute<String> PRODUCT_NAME = stringAttribute("Name", T_PRODUCT);
  public static final Attribute<String> PRODUCT_DESCRIPTION = stringAttribute("Description", T_PRODUCT);
  public static final Attribute<String> PRODUCT_IMAGE_URL = stringAttribute("Image URL", T_PRODUCT);

  void product() {
    define(T_PRODUCT, "petstore.product",
            primaryKeyProperty(PRODUCT_ID)
                    .columnName("productid"),
            foreignKeyProperty(PRODUCT_CATEGORY_FK, PRODUCT_CATEGORY_FK.getName(), T_CATEGORY,
                    columnProperty(PRODUCT_CATEGORY_ID)
                            .columnName("categoryid")).nullable(false),
            columnProperty(PRODUCT_NAME, PRODUCT_NAME.getName())
                    .columnName("name").maximumLength(25).nullable(false),
            columnProperty(PRODUCT_DESCRIPTION, PRODUCT_DESCRIPTION.getName())
                    .columnName("description").maximumLength(255).nullable(false),
            columnProperty(PRODUCT_IMAGE_URL, PRODUCT_IMAGE_URL.getName())
                    .columnName("imageurl").maximumLength(55).hidden(true))
            .keyGenerator(increment("petstore.product", "productid"))
            .orderBy(orderBy().ascending(PRODUCT_NAME))
            .stringProvider(new StringProvider(PRODUCT_CATEGORY_FK)
                    .addText(" - ").addValue(PRODUCT_NAME))
            .caption("Products");
  }

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo";
  public static final Attribute<Integer> SELLER_CONTACT_INFO_ID = integerAttribute("Contactinfo id", T_SELLER_CONTACT_INFO);
  public static final Attribute<String> SELLER_CONTACT_INFO_FIRST_NAME = stringAttribute("First name", T_SELLER_CONTACT_INFO);
  public static final Attribute<String> SELLER_CONTACT_INFO_LAST_NAME = stringAttribute("Last name", T_SELLER_CONTACT_INFO);
  public static final Attribute<String> SELLER_CONTACT_INFO_EMAIL = stringAttribute("Email", T_SELLER_CONTACT_INFO);

  void sellerContactInfo() {
    define(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            primaryKeyProperty(SELLER_CONTACT_INFO_ID)
                    .columnName("contactinfoid"),
            columnProperty(SELLER_CONTACT_INFO_FIRST_NAME, SELLER_CONTACT_INFO_FIRST_NAME.getName())
                    .searchProperty(true).columnName("firstname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_LAST_NAME, SELLER_CONTACT_INFO_LAST_NAME.getName())
                    .searchProperty(true).columnName("lastname").maximumLength(24).nullable(false),
            columnProperty(SELLER_CONTACT_INFO_EMAIL, SELLER_CONTACT_INFO_EMAIL.getName())
                    .columnName("email").maximumLength(24).nullable(false))
            .keyGenerator(increment("petstore.sellercontactinfo", "contactinfoid"))
            .orderBy(orderBy()
                    .ascending(SELLER_CONTACT_INFO_LAST_NAME, SELLER_CONTACT_INFO_FIRST_NAME))
            .stringProvider(new StringProvider(SELLER_CONTACT_INFO_LAST_NAME)
                    .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME))
            .caption("Seller info");
  }

  public static final String T_ITEM = "item";
  public static final Attribute<Integer> ITEM_ID = integerAttribute("Item id", T_ITEM);
  public static final Attribute<Integer> ITEM_PRODUCT_ID = integerAttribute("Product id", T_ITEM);
  public static final EntityAttribute ITEM_PRODUCT_FK = entityAttribute("Product", T_ITEM);
  public static final Attribute<String> ITEM_NAME = stringAttribute("Name", T_ITEM);
  public static final Attribute<String> ITEM_DESCRIPTION = stringAttribute("Description", T_ITEM);
  public static final Attribute<String> ITEM_IMAGE_URL = stringAttribute("Image URL", T_ITEM);
  public static final Attribute<String> ITEM_IMAGE_THUMB_URL = stringAttribute("Image thumbnail URL", T_ITEM);
  public static final Attribute<BigDecimal> ITEM_PRICE = bigDecimalAttribute("Price", T_ITEM);
  public static final Attribute<Integer> ITEM_C0NTACT_INFO_ID = integerAttribute("Contactinfo id", T_ITEM);
  public static final EntityAttribute ITEM_C0NTACT_INFO_FK = entityAttribute("Contact info", T_ITEM);
  public static final Attribute<Integer> ITEM_ADDRESS_ID = integerAttribute("Address id", T_ITEM);
  public static final EntityAttribute ITEM_ADDRESS_FK = entityAttribute("Address", T_ITEM);
  public static final Attribute<Boolean> ITEM_DISABLED = booleanAttribute("Disabled", T_ITEM);

  void item() {
    define(T_ITEM, "petstore.item",
            primaryKeyProperty(ITEM_ID)
                    .columnName("itemid"),
            foreignKeyProperty(ITEM_PRODUCT_FK, ITEM_PRODUCT_FK.getName(), T_PRODUCT,
                    columnProperty(ITEM_PRODUCT_ID)
                            .columnName("productid"))
                    .fetchDepth(2).nullable(false),
            columnProperty(ITEM_NAME, ITEM_NAME.getName())
                    .columnName("name").maximumLength(30).nullable(false),
            columnProperty(ITEM_DESCRIPTION, ITEM_DESCRIPTION.getName())
                    .columnName("description").maximumLength(500).nullable(false),
            columnProperty(ITEM_IMAGE_URL, ITEM_IMAGE_URL.getName())
                    .columnName("imageurl").maximumLength(55).hidden(true),
            columnProperty(ITEM_IMAGE_THUMB_URL, ITEM_IMAGE_THUMB_URL.getName())
                    .columnName("imagethumburl").maximumLength(55).hidden(true),
            columnProperty(ITEM_PRICE, ITEM_PRICE.getName())
                    .columnName("price").nullable(false).maximumFractionDigits(2),
            foreignKeyProperty(ITEM_C0NTACT_INFO_FK, ITEM_C0NTACT_INFO_FK.getName(), T_SELLER_CONTACT_INFO,
                    columnProperty(ITEM_C0NTACT_INFO_ID).columnName("contactinfo_contactinfoid"))
                    .nullable(false),
            foreignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    columnProperty(ITEM_ADDRESS_ID).columnName("address_addressid"))
                    .nullable(false),
            booleanProperty(ITEM_DISABLED, INTEGER, ITEM_DISABLED.getName(), 1, 0)
                    .columnName("disabled").defaultValue(false))
            .keyGenerator(increment("petstore.item", "itemid"))
            .orderBy(orderBy().ascending(ITEM_NAME))
            .stringProvider(new StringProvider(ITEM_PRODUCT_FK)
                    .addText(" - ").addValue(ITEM_NAME))
            .caption("Items");
  }

  public static final String T_TAG = "tag";
  public static final Attribute<Integer> TAG_ID = integerAttribute("Tag id", T_TAG);
  public static final Attribute<String> TAG_TAG = stringAttribute("Tag", T_TAG);
  public static final Attribute<Integer> TAG_REFCOUNT = integerAttribute("Reference count", T_TAG);

  void tag() {
    define(T_TAG, "petstore.tag",
            primaryKeyProperty(TAG_ID)
                    .columnName("tagid"),
            columnProperty(TAG_TAG, TAG_TAG.getName())
                    .columnName("tag").maximumLength(30).nullable(false),
            subqueryProperty(TAG_REFCOUNT, TAG_REFCOUNT.getName(),
                    "select count(*) from petstore.tag_item where tagid = tag.tagid")
                    .columnName("refcount"))
            .keyGenerator(increment("petstore.tag", "tagid"))
            .orderBy(orderBy().ascending(TAG_TAG))
            .selectTableName("petstore.tag tag")
            .stringProvider(new StringProvider(TAG_TAG))
            .caption("Tags");
  }

  public static final String T_TAG_ITEM = "tag_item";
  public static final Attribute<Integer> TAG_ITEM_ITEM_ID = integerAttribute("Item id", T_TAG_ITEM);
  public static final EntityAttribute TAG_ITEM_ITEM_FK = entityAttribute("Item", T_TAG_ITEM);
  public static final Attribute<Integer> TAG_ITEM_TAG_ID = integerAttribute("Tag id", T_TAG_ITEM);
  public static final EntityAttribute TAG_ITEM_TAG_FK = entityAttribute("Tag", T_TAG_ITEM);

  void tagItem() {
    define(T_TAG_ITEM, "petstore.tag_item",
            foreignKeyProperty(TAG_ITEM_ITEM_FK, TAG_ITEM_ITEM_FK.getName(), T_ITEM,
                    primaryKeyProperty(TAG_ITEM_ITEM_ID)
                            .columnName("itemid").primaryKeyIndex(0))
                    .nullable(false),
            foreignKeyProperty(TAG_ITEM_TAG_FK, TAG_ITEM_TAG_FK.getName(), T_TAG,
                    primaryKeyProperty(TAG_ITEM_TAG_ID)
                            .columnName("tagid").primaryKeyIndex(1))
                    .nullable(false))
            .stringProvider(new StringProvider(TAG_ITEM_ITEM_FK)
                    .addText(" - ").addValue(TAG_ITEM_TAG_FK))
            .caption("Item tags");
  }
}
