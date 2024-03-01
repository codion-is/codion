/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petstore.domain;

import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.sequence;
import static is.codion.framework.domain.entity.OrderBy.ascending;

public final class Petstore extends DefaultDomain {

  public static final DomainType DOMAIN = domainType(Petstore.class);

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

    Column<Integer> ID = TYPE.integerColumn("Address id");
    Column<String> STREET_1 = TYPE.stringColumn("Street 1");
    Column<String> STREET_2 = TYPE.stringColumn("Street 2");
    Column<String> CITY = TYPE.stringColumn("City");
    Column<String> STATE = TYPE.stringColumn("State");
    Column<Integer> ZIP = TYPE.integerColumn("Zip");
    Column<Double> LATITUDE = TYPE.doubleColumn("Latitude");
    Column<Double> LONGITUDE = TYPE.doubleColumn("Longitude");
  }

  void address() {
    add(Address.TYPE.define(
                    Address.ID.define()
                            .primaryKey()
                            .name("addressid"),
                    Address.STREET_1.define()
                            .column()
                            .caption(Address.STREET_1.name())
                            .name("street1")
                            .maximumLength(55)
                            .nullable(false),
                    Address.STREET_2.define()
                            .column()
                            .caption(Address.STREET_2.name())
                            .name("street2")
                            .maximumLength(55),
                    Address.CITY.define()
                            .column()
                            .caption(Address.CITY.name())
                            .name("city")
                            .maximumLength(55)
                            .nullable(false),
                    Address.STATE.define()
                            .column()
                            .caption(Address.STATE.name())
                            .name("state")
                            .maximumLength(25)
                            .nullable(false),
                    Address.ZIP.define()
                            .column()
                            .caption(Address.ZIP.name())
                            .name("zip")
                            .nullable(false),
                    Address.LATITUDE.define()
                            .column()
                            .caption(Address.LATITUDE.name())
                            .name("latitude")
                            .nullable(false)
                            .maximumFractionDigits(2),
                    Address.LONGITUDE.define()
                            .column()
                            .caption(Address.LONGITUDE.name())
                            .name("longitude")
                            .nullable(false)
                            .maximumFractionDigits(2))
            .tableName("petstore.address")
            .keyGenerator(sequence("petstore.address_seq"))
            .orderBy(ascending(Address.CITY, Address.STREET_1, Address.STREET_2))
            .stringFactory(StringFactory.builder()
                    .value(Address.STREET_1).text(" ")
                    .value(Address.STREET_2).text(", ")
                    .value(Address.CITY).text(" ")
                    .value(Address.ZIP).text(", ")
                    .value(Address.STATE)
                    .build())
            .caption("Addresses"));
  }

  public interface Category {
    EntityType TYPE = DOMAIN.entityType("category");

    Column<Integer> ID = TYPE.integerColumn("Category id");
    Column<String> NAME = TYPE.stringColumn("Name");
    Column<String> DESCRIPTION = TYPE.stringColumn("Description");
    Column<String> IMAGE_URL = TYPE.stringColumn("Image URL");
  }

  void category() {
    add(Category.TYPE.define(
                    Category.ID.define()
                            .primaryKey()
                            .name("categoryid"),
                    Category.NAME.define()
                            .column()
                            .caption(Category.NAME.name())
                            .name("name")
                            .maximumLength(25)
                            .nullable(false),
                    Category.DESCRIPTION.define()
                            .column()
                            .caption(Category.DESCRIPTION.name())
                            .name("description")
                            .maximumLength(255)
                            .nullable(false),
                    Category.IMAGE_URL.define()
                            .column()
                            .caption(Category.IMAGE_URL.name())
                            .name("imageurl")
                            .hidden(true))
            .tableName("petstore.category")
            .keyGenerator(sequence("petstore.category_seq"))
            .orderBy(ascending(Category.NAME))
            .stringFactory(Category.NAME)
            .caption("Categories"));
  }

  public interface Product {
    EntityType TYPE = DOMAIN.entityType("product");

    Column<Integer> ID = TYPE.integerColumn("Product id");
    Column<Integer> CATEGORY_ID = TYPE.integerColumn("Category id");
    Column<String> NAME = TYPE.stringColumn("Name");
    Column<String> DESCRIPTION = TYPE.stringColumn("Description");
    Column<String> IMAGE_URL = TYPE.stringColumn("Image URL");

    ForeignKey CATEGORY_FK = TYPE.foreignKey("Category", CATEGORY_ID, Category.ID);
  }

  void product() {
    add(Product.TYPE.define(
                    Product.ID.define()
                            .primaryKey()
                            .name("productid"),
                    Product.CATEGORY_ID.define()
                            .column()
                            .name("categoryid")
                            .nullable(false),
                    Product.CATEGORY_FK.define()
                            .foreignKey()
                            .caption(Product.CATEGORY_FK.name()),
                    Product.NAME.define()
                            .column()
                            .caption(Product.NAME.name())
                            .name("name")
                            .maximumLength(25)
                            .nullable(false),
                    Product.DESCRIPTION.define()
                            .column()
                            .caption(Product.DESCRIPTION.name())
                            .name("description")
                            .maximumLength(255)
                            .nullable(false),
                    Product.IMAGE_URL.define()
                            .column()
                            .caption(Product.IMAGE_URL.name())
                            .name("imageurl")
                            .maximumLength(55)
                            .hidden(true))
            .tableName("petstore.product")
            .keyGenerator(sequence("petstore.product_seq"))
            .orderBy(ascending(Product.NAME))
            .stringFactory(StringFactory.builder()
                    .value(Product.CATEGORY_FK)
                    .text(" - ")
                    .value(Product.NAME)
                    .build())
            .caption("Products"));
  }

  public interface SellerContactInfo {
    EntityType TYPE = DOMAIN.entityType("sellercontactinfo");

    Column<Integer> ID = TYPE.integerColumn("Contactinfo id");
    Column<String> FIRST_NAME = TYPE.stringColumn("First name");
    Column<String> LAST_NAME = TYPE.stringColumn("Last name");
    Column<String> EMAIL = TYPE.stringColumn("Email");
  }

  void sellerContactInfo() {
    add(SellerContactInfo.TYPE.define(
                    SellerContactInfo.ID.define()
                            .primaryKey()
                            .name("contactinfoid"),
                    SellerContactInfo.FIRST_NAME.define()
                            .column()
                            .caption(SellerContactInfo.FIRST_NAME.name())
                            .searchable(true)
                            .name("firstname")
                            .maximumLength(24)
                            .nullable(false),
                    SellerContactInfo.LAST_NAME.define()
                            .column()
                            .caption(SellerContactInfo.LAST_NAME.name())
                            .searchable(true)
                            .name("lastname")
                            .maximumLength(24)
                            .nullable(false),
                    SellerContactInfo.EMAIL.define()
                            .column()
                            .caption(SellerContactInfo.EMAIL.name())
                            .name("email")
                            .maximumLength(24)
                            .nullable(false))
            .tableName("petstore.sellercontactinfo")
            .keyGenerator(sequence("petstore.sellercontactinfo_seq"))
            .orderBy(ascending(SellerContactInfo.LAST_NAME, SellerContactInfo.FIRST_NAME))
            .stringFactory(StringFactory.builder()
                    .value(SellerContactInfo.LAST_NAME)
                    .text(", ")
                    .value(SellerContactInfo.FIRST_NAME)
                    .build())
            .caption("Seller info"));
  }

  public interface Item {
    EntityType TYPE = DOMAIN.entityType("item");

    Column<Integer> ID = TYPE.integerColumn("Item id");
    Column<Integer> PRODUCT_ID = TYPE.integerColumn("Product id");
    Column<String> NAME = TYPE.stringColumn("Name");
    Column<String> DESCRIPTION = TYPE.stringColumn("Description");
    Column<String> IMAGE_URL = TYPE.stringColumn("Image URL");
    Column<String> IMAGE_THUMB_URL = TYPE.stringColumn("Image thumbnail URL");
    Column<BigDecimal> PRICE = TYPE.bigDecimalColumn("Price");
    Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("Contactinfo id");
    Column<Integer> ADDRESS_ID = TYPE.integerColumn("Address id");
    Column<Boolean> DISABLED = TYPE.booleanColumn("Disabled");

    ForeignKey PRODUCT_FK = TYPE.foreignKey("Product", PRODUCT_ID, Product.ID);
    ForeignKey CONTACT_INFO_FK = TYPE.foreignKey("Contact info", CONTACT_INFO_ID, SellerContactInfo.ID);
    ForeignKey ADDRESS_FK = TYPE.foreignKey("Address", ADDRESS_ID, Address.ID);
  }

  void item() {
    add(Item.TYPE.define(
                    Item.ID.define()
                            .primaryKey()
                            .name("itemid"),
                    Item.PRODUCT_ID.define()
                            .column()
                            .name("productid")
                            .nullable(false),
                    Item.PRODUCT_FK.define()
                            .foreignKey(2)
                            .caption(Item.PRODUCT_FK.name()),
                    Item.NAME.define()
                            .column()
                            .caption(Item.NAME.name())
                            .name("name")
                            .maximumLength(30)
                            .nullable(false),
                    Item.DESCRIPTION.define()
                            .column()
                            .caption(Item.DESCRIPTION.name())
                            .name("description")
                            .maximumLength(500)
                            .nullable(false),
                    Item.IMAGE_URL.define()
                            .column()
                            .caption(Item.IMAGE_URL.name())
                            .name("imageurl")
                            .maximumLength(55)
                            .hidden(true),
                    Item.IMAGE_THUMB_URL.define()
                            .column()
                            .caption(Item.IMAGE_THUMB_URL.name())
                            .name("imagethumburl")
                            .maximumLength(55)
                            .hidden(true),
                    Item.PRICE.define()
                            .column()
                            .caption(Item.PRICE.name())
                            .name("price")
                            .nullable(false)
                            .maximumFractionDigits(2),
                    Item.CONTACT_INFO_ID.define()
                            .column()
                            .name("contactinfo_contactinfoid")
                            .nullable(false),
                    Item.CONTACT_INFO_FK.define()
                            .foreignKey()
                            .caption(Item.CONTACT_INFO_FK.name()),
                    Item.ADDRESS_ID.define()
                            .column()
                            .name("address_addressid")
                            .nullable(false),
                    Item.ADDRESS_FK.define()
                            .foreignKey()
                            .caption("Address"),
                    Item.DISABLED.define()
                            .booleanColumn(Integer.class, 1, 0)
                            .caption(Item.DISABLED.name())
                            .name("disabled")
                            .defaultValue(false)
                            .nullable(false))
            .tableName("petstore.item")
            .keyGenerator(sequence("petstore.item_seq"))
            .orderBy(ascending(Item.NAME))
            .stringFactory(StringFactory.builder()
                    .value(Item.PRODUCT_FK)
                    .text(" - ")
                    .value(Item.NAME)
                    .build())
            .caption("Items"));
  }

  public interface Tag {
    EntityType TYPE = DOMAIN.entityType("tag");

    Column<Integer> ID = TYPE.integerColumn("Tag id");
    Column<String> TAG = TYPE.stringColumn("Tag");
    Column<Integer> REFCOUNT = TYPE.integerColumn("Reference count");
  }

  void tag() {
    add(Tag.TYPE.define(
                    Tag.ID.define()
                            .primaryKey()
                            .name("tagid"),
                    Tag.TAG.define()
                            .column()
                            .caption(Tag.TAG.name())
                            .name("tag")
                            .maximumLength(30)
                            .nullable(false),
                    Tag.REFCOUNT.define()
                            .subquery("SELECT COUNT(*) FROM petstore.tag_item WHERE tagid = tag.tagid")
                            .caption(Tag.REFCOUNT.name())
                            .name("refcount"))
            .tableName("petstore.tag")
            .keyGenerator(sequence("petstore.tag_seq"))
            .orderBy(ascending(Tag.TAG))
            .selectTableName("petstore.tag tag")
            .stringFactory(Tag.TAG)
            .caption("Tags"));
  }

  public interface TagItem {
    EntityType TYPE = DOMAIN.entityType("tag_item");

    Column<Integer> ITEM_ID = TYPE.integerColumn("Item id");
    Column<Integer> TAG_ID = TYPE.integerColumn("Tag id");

    ForeignKey ITEM_FK = TYPE.foreignKey("Item", ITEM_ID, Item.ID);
    ForeignKey TAG_FK = TYPE.foreignKey("Tag", TAG_ID, Tag.ID);
  }

  void tagItem() {
    add(TagItem.TYPE.define(
                    TagItem.ITEM_ID.define()
                            .primaryKey(0)
                            .name("itemid"),
                    TagItem.ITEM_FK.define()
                            .foreignKey(3)
                            .caption(TagItem.ITEM_FK.name()),
                    TagItem.TAG_ID.define()
                            .primaryKey(1)
                            .name("tagid"),
                    TagItem.TAG_FK.define()
                            .foreignKey()
                            .caption(TagItem.TAG_FK.name()))
            .tableName("petstore.tag_item")
            .stringFactory(StringFactory.builder()
                    .value(TagItem.ITEM_FK)
                    .text(" - ")
                    .value(TagItem.TAG_FK)
                    .build())
            .caption("Item tags"));
  }
}
