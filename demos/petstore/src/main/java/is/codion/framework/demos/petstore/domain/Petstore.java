/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.StringFactory;

import java.math.BigDecimal;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.OrderBy.orderBy;
import static is.codion.framework.domain.property.Properties.*;

public final class Petstore extends DefaultDomain {

  static final DomainType DOMAIN = domainType(Petstore.class);

  public Petstore() {
    super(DOMAIN);
    address();
    category();
    product();
    sellerContactInfo();
    item();
    tag();
    tagItem();
  }

  public interface Address {
    EntityType TYPE = DOMAIN.entityType("address");

    Attribute<Integer> ID = TYPE.integerAttribute("Address id");
    Attribute<String> STREET_1 = TYPE.stringAttribute("Street 1");
    Attribute<String> STREET_2 = TYPE.stringAttribute("Street 2");
    Attribute<String> CITY = TYPE.stringAttribute("City");
    Attribute<String> STATE = TYPE.stringAttribute("State");
    Attribute<Integer> ZIP = TYPE.integerAttribute("Zip");
    Attribute<Double> LATITUDE = TYPE.doubleAttribute("Latitude");
    Attribute<Double> LONGITUDE = TYPE.doubleAttribute("Longitude");
  }

  void address() {
    define(Address.TYPE, "petstore.address",
            primaryKeyProperty(Address.ID)
                    .columnName("addressid"),
            columnProperty(Address.STREET_1, Address.STREET_1.getName())
                    .columnName("street1")
                    .maximumLength(55)
                    .nullable(false),
            columnProperty(Address.STREET_2, Address.STREET_2.getName())
                    .columnName("street2")
                    .maximumLength(55),
            columnProperty(Address.CITY, Address.CITY.getName())
                    .columnName("city")
                    .maximumLength(55)
                    .nullable(false),
            columnProperty(Address.STATE, Address.STATE.getName())
                    .columnName("state")
                    .maximumLength(25)
                    .nullable(false),
            columnProperty(Address.ZIP, Address.ZIP.getName())
                    .columnName("zip")
                    .nullable(false),
            columnProperty(Address.LATITUDE, Address.LATITUDE.getName())
                    .columnName("latitude")
                    .nullable(false)
                    .maximumFractionDigits(2),
            columnProperty(Address.LONGITUDE, Address.LONGITUDE.getName())
                    .columnName("longitude")
                    .nullable(false)
                    .maximumFractionDigits(2))
            .keyGenerator(increment("petstore.address", "addressid"))
            .orderByAscending(Address.CITY, Address.STREET_1, Address.STREET_2)
            .stringFactory(StringFactory.builder()
                    .value(Address.STREET_1).text(" ")
                    .value(Address.STREET_2).text(", ")
                    .value(Address.CITY).text(" ")
                    .value(Address.ZIP).text(", ")
                    .value(Address.STATE))
            .caption("Addresses");
  }

  public interface Category {
    EntityType TYPE = DOMAIN.entityType("category");

    Attribute<Integer> ID = TYPE.integerAttribute("Category id");
    Attribute<String> NAME = TYPE.stringAttribute("Name");
    Attribute<String> DESCRIPTION = TYPE.stringAttribute("Description");
    Attribute<String> IMAGE_URL = TYPE.stringAttribute("Image URL");
  }

  void category() {
    define(Category.TYPE, "petstore.category",
            primaryKeyProperty(Category.ID)
                    .columnName("categoryid"),
            columnProperty(Category.NAME, Category.NAME.getName())
                    .columnName("name")
                    .maximumLength(25)
                    .nullable(false),
            columnProperty(Category.DESCRIPTION, Category.DESCRIPTION.getName())
                    .columnName("description")
                    .maximumLength(255)
                    .nullable(false),
            columnProperty(Category.IMAGE_URL, Category.IMAGE_URL.getName())
                    .columnName("imageurl")
                    .hidden(true))
            .keyGenerator(increment("petstore.category", "categoryid"))
            .orderByAscending(Category.NAME)
            .stringFactory(Category.NAME)
            .caption("Categories");
  }

  public interface Product {
    EntityType TYPE = DOMAIN.entityType("product");

    Attribute<Integer> ID = TYPE.integerAttribute("Product id");
    Attribute<Integer> CATEGORY_ID = TYPE.integerAttribute("Category id");
    Attribute<String> NAME = TYPE.stringAttribute("Name");
    Attribute<String> DESCRIPTION = TYPE.stringAttribute("Description");
    Attribute<String> IMAGE_URL = TYPE.stringAttribute("Image URL");

    ForeignKey CATEGORY_FK = TYPE.foreignKey("Category", CATEGORY_ID, Category.ID);
  }

  void product() {
    define(Product.TYPE, "petstore.product",
            primaryKeyProperty(Product.ID)
                    .columnName("productid"),
            columnProperty(Product.CATEGORY_ID)
                    .columnName("categoryid")
                    .nullable(false),
            foreignKeyProperty(Product.CATEGORY_FK, Product.CATEGORY_FK.getName()),
            columnProperty(Product.NAME, Product.NAME.getName())
                    .columnName("name")
                    .maximumLength(25)
                    .nullable(false),
            columnProperty(Product.DESCRIPTION, Product.DESCRIPTION.getName())
                    .columnName("description")
                    .maximumLength(255)
                    .nullable(false),
            columnProperty(Product.IMAGE_URL, Product.IMAGE_URL.getName())
                    .columnName("imageurl")
                    .maximumLength(55)
                    .hidden(true))
            .keyGenerator(increment("petstore.product", "productid"))
            .orderByAscending(Product.NAME)
            .stringFactory(StringFactory.builder()
                    .value(Product.CATEGORY_FK)
                    .text(" - ")
                    .value(Product.NAME))
            .caption("Products");
  }

  public interface SellerContactInfo {
    EntityType TYPE = DOMAIN.entityType("sellercontactinfo");

    Attribute<Integer> ID = TYPE.integerAttribute("Contactinfo id");
    Attribute<String> FIRST_NAME = TYPE.stringAttribute("First name");
    Attribute<String> LAST_NAME = TYPE.stringAttribute("Last name");
    Attribute<String> EMAIL = TYPE.stringAttribute("Email");
  }

  void sellerContactInfo() {
    define(SellerContactInfo.TYPE, "petstore.sellercontactinfo",
            primaryKeyProperty(SellerContactInfo.ID)
                    .columnName("contactinfoid"),
            columnProperty(SellerContactInfo.FIRST_NAME, SellerContactInfo.FIRST_NAME.getName())
                    .searchProperty(true)
                    .columnName("firstname")
                    .maximumLength(24)
                    .nullable(false),
            columnProperty(SellerContactInfo.LAST_NAME, SellerContactInfo.LAST_NAME.getName())
                    .searchProperty(true)
                    .columnName("lastname")
                    .maximumLength(24)
                    .nullable(false),
            columnProperty(SellerContactInfo.EMAIL, SellerContactInfo.EMAIL.getName())
                    .columnName("email")
                    .maximumLength(24)
                    .nullable(false))
            .keyGenerator(increment("petstore.sellercontactinfo", "contactinfoid"))
            .orderBy(orderBy()
                    .ascending(SellerContactInfo.LAST_NAME, SellerContactInfo.FIRST_NAME))
            .stringFactory(StringFactory.builder()
                    .value(SellerContactInfo.LAST_NAME)
                    .text(", ")
                    .value(SellerContactInfo.FIRST_NAME))
            .caption("Seller info");
  }

  public interface Item {
    EntityType TYPE = DOMAIN.entityType("item");

    Attribute<Integer> ID = TYPE.integerAttribute("Item id");
    Attribute<Integer> PRODUCT_ID = TYPE.integerAttribute("Product id");
    Attribute<String> NAME = TYPE.stringAttribute("Name");
    Attribute<String> DESCRIPTION = TYPE.stringAttribute("Description");
    Attribute<String> IMAGE_URL = TYPE.stringAttribute("Image URL");
    Attribute<String> IMAGE_THUMB_URL = TYPE.stringAttribute("Image thumbnail URL");
    Attribute<BigDecimal> PRICE = TYPE.bigDecimalAttribute("Price");
    Attribute<Integer> CONTACT_INFO_ID = TYPE.integerAttribute("Contactinfo id");
    Attribute<Integer> ADDRESS_ID = TYPE.integerAttribute("Address id");
    Attribute<Boolean> DISABLED = TYPE.booleanAttribute("Disabled");

    ForeignKey PRODUCT_FK = TYPE.foreignKey("Product", PRODUCT_ID, Product.ID);
    ForeignKey CONTACT_INFO_FK = TYPE.foreignKey("Contact info", CONTACT_INFO_ID, SellerContactInfo.ID);
    ForeignKey ADDRESS_FK = TYPE.foreignKey("Address", ADDRESS_ID, Address.ID);
  }

  void item() {
    define(Item.TYPE, "petstore.item",
            primaryKeyProperty(Item.ID)
                    .columnName("itemid"),
            columnProperty(Item.PRODUCT_ID)
                    .columnName("productid")
                    .nullable(false),
            foreignKeyProperty(Item.PRODUCT_FK, Item.PRODUCT_FK.getName())
                    .fetchDepth(2),
            columnProperty(Item.NAME, Item.NAME.getName())
                    .columnName("name")
                    .maximumLength(30)
                    .nullable(false),
            columnProperty(Item.DESCRIPTION, Item.DESCRIPTION.getName())
                    .columnName("description")
                    .maximumLength(500)
                    .nullable(false),
            columnProperty(Item.IMAGE_URL, Item.IMAGE_URL.getName())
                    .columnName("imageurl")
                    .maximumLength(55)
                    .hidden(true),
            columnProperty(Item.IMAGE_THUMB_URL, Item.IMAGE_THUMB_URL.getName())
                    .columnName("imagethumburl")
                    .maximumLength(55)
                    .hidden(true),
            columnProperty(Item.PRICE, Item.PRICE.getName())
                    .columnName("price")
                    .nullable(false)
                    .maximumFractionDigits(2),
            columnProperty(Item.CONTACT_INFO_ID).columnName("contactinfo_contactinfoid")
                    .nullable(false),
            foreignKeyProperty(Item.CONTACT_INFO_FK, Item.CONTACT_INFO_FK.getName()),
            columnProperty(Item.ADDRESS_ID).columnName("address_addressid")
                    .nullable(false),
            foreignKeyProperty(Item.ADDRESS_FK, "Address"),
            booleanProperty(Item.DISABLED, Item.DISABLED.getName(), Integer.class, 1, 0)
                    .columnName("disabled")
                    .defaultValue(false)
                    .nullable(false))
            .keyGenerator(increment("petstore.item", "itemid"))
            .orderByAscending(Item.NAME)
            .stringFactory(StringFactory.builder()
                    .value(Item.PRODUCT_FK)
                    .text(" - ")
                    .value(Item.NAME))
            .caption("Items");
  }

  public interface Tag {
    EntityType TYPE = DOMAIN.entityType("tag");

    Attribute<Integer> ID = TYPE.integerAttribute("Tag id");
    Attribute<String> TAG = TYPE.stringAttribute("Tag");
    Attribute<Integer> REFCOUNT = TYPE.integerAttribute("Reference count");
  }

  void tag() {
    define(Tag.TYPE, "petstore.tag",
            primaryKeyProperty(Tag.ID)
                    .columnName("tagid"),
            columnProperty(Tag.TAG, Tag.TAG.getName())
                    .columnName("tag")
                    .maximumLength(30)
                    .nullable(false),
            subqueryProperty(Tag.REFCOUNT, Tag.REFCOUNT.getName(),
                    "select count(*) from petstore.tag_item where tagid = tag.tagid")
                    .columnName("refcount"))
            .keyGenerator(increment("petstore.tag", "tagid"))
            .orderByAscending(Tag.TAG)
            .selectTableName("petstore.tag tag")
            .stringFactory(Tag.TAG)
            .caption("Tags");
  }

  public interface TagItem {
    EntityType TYPE = DOMAIN.entityType("tag_item");

    Attribute<Integer> ITEM_ID = TYPE.integerAttribute("Item id");
    Attribute<Integer> TAG_ID = TYPE.integerAttribute("Tag id");

    ForeignKey ITEM_FK = TYPE.foreignKey("Item", ITEM_ID, Item.ID);
    ForeignKey TAG_FK = TYPE.foreignKey("Tag", TAG_ID, Tag.ID);
  }

  void tagItem() {
    define(TagItem.TYPE, "petstore.tag_item",
            columnProperty(TagItem.ITEM_ID)
                    .primaryKeyIndex(0)
                    .columnName("itemid"),
            foreignKeyProperty(TagItem.ITEM_FK, TagItem.ITEM_FK.getName())
                    .fetchDepth(3),
            columnProperty(TagItem.TAG_ID)
                    .primaryKeyIndex(1)
                    .columnName("tagid"),
            foreignKeyProperty(TagItem.TAG_FK, TagItem.TAG_FK.getName()))
            .stringFactory(StringFactory.builder()
                    .value(TagItem.ITEM_FK)
                    .text(" - ")
                    .value(TagItem.TAG_FK))
            .caption("Item tags");
  }
}
