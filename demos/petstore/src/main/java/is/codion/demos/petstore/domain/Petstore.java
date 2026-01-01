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
 * Copyright (c) 2004 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.petstore.domain;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityFormatter;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.sql.Statement;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.attribute.Column.Generator.sequence;

public final class Petstore extends DomainModel {

	public static final DomainType DOMAIN = domainType(Petstore.class);

	public Petstore() {
		super(DOMAIN);
		add(address(), category(), product(), sellerContactInfo(), item(), tag(), tagItem());
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

	EntityDefinition address() {
		return Address.TYPE.as(
										Address.ID.as()
														.primaryKey()
														.generator(sequence("petstore.address_seq"))
														.name("addressid"),
										Address.STREET_1.as()
														.column()
														.caption(Address.STREET_1.name())
														.name("street1")
														.maximumLength(55)
														.nullable(false),
										Address.STREET_2.as()
														.column()
														.caption(Address.STREET_2.name())
														.name("street2")
														.maximumLength(55),
										Address.CITY.as()
														.column()
														.caption(Address.CITY.name())
														.name("city")
														.maximumLength(55)
														.nullable(false),
										Address.STATE.as()
														.column()
														.caption(Address.STATE.name())
														.name("state")
														.maximumLength(25)
														.nullable(false),
										Address.ZIP.as()
														.column()
														.caption(Address.ZIP.name())
														.name("zip")
														.nullable(false),
										Address.LATITUDE.as()
														.column()
														.caption(Address.LATITUDE.name())
														.name("latitude")
														.nullable(false)
														.fractionDigits(2),
										Address.LONGITUDE.as()
														.column()
														.caption(Address.LONGITUDE.name())
														.name("longitude")
														.nullable(false)
														.fractionDigits(2))
						.table("petstore.address")
						.orderBy(ascending(Address.CITY, Address.STREET_1, Address.STREET_2))
						.formatter(EntityFormatter.builder()
										.value(Address.STREET_1).text(" ")
										.value(Address.STREET_2).text(", ")
										.value(Address.CITY).text(" ")
										.value(Address.ZIP).text(", ")
										.value(Address.STATE)
										.build())
						.caption("Addresses")
						.build();
	}

	public interface Category {
		EntityType TYPE = DOMAIN.entityType("category");

		Column<Integer> ID = TYPE.integerColumn("Category id");
		Column<String> NAME = TYPE.stringColumn("Name");
		Column<String> DESCRIPTION = TYPE.stringColumn("Description");
		Column<String> IMAGE_URL = TYPE.stringColumn("Image URL");
	}

	EntityDefinition category() {
		return Category.TYPE.as(
										Category.ID.as()
														.primaryKey()
														.generator(sequence("petstore.category_seq"))
														.name("categoryid"),
										Category.NAME.as()
														.column()
														.caption(Category.NAME.name())
														.name("name")
														.maximumLength(25)
														.nullable(false),
										Category.DESCRIPTION.as()
														.column()
														.caption(Category.DESCRIPTION.name())
														.name("description")
														.maximumLength(255)
														.nullable(false),
										Category.IMAGE_URL.as()
														.column()
														.caption(Category.IMAGE_URL.name())
														.name("imageurl")
														.hidden(true))
						.table("petstore.category")
						.orderBy(ascending(Category.NAME))
						.formatter(Category.NAME)
						.caption("Categories")
						.build();
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

	EntityDefinition product() {
		return Product.TYPE.as(
										Product.ID.as()
														.primaryKey()
														.generator(sequence("petstore.product_seq"))
														.name("productid"),
										Product.CATEGORY_ID.as()
														.column()
														.name("categoryid")
														.nullable(false),
										Product.CATEGORY_FK.as()
														.foreignKey()
														.caption(Product.CATEGORY_FK.name()),
										Product.NAME.as()
														.column()
														.caption(Product.NAME.name())
														.name("name")
														.maximumLength(25)
														.nullable(false),
										Product.DESCRIPTION.as()
														.column()
														.caption(Product.DESCRIPTION.name())
														.name("description")
														.maximumLength(255)
														.nullable(false),
										Product.IMAGE_URL.as()
														.column()
														.caption(Product.IMAGE_URL.name())
														.name("imageurl")
														.maximumLength(55)
														.hidden(true))
						.table("petstore.product")
						.orderBy(ascending(Product.NAME))
						.formatter(EntityFormatter.builder()
										.value(Product.CATEGORY_FK)
										.text(" - ")
										.value(Product.NAME)
										.build())
						.caption("Products")
						.build();
	}

	public interface SellerContactInfo {
		EntityType TYPE = DOMAIN.entityType("sellercontactinfo");

		Column<Integer> ID = TYPE.integerColumn("Contactinfo id");
		Column<String> FIRST_NAME = TYPE.stringColumn("First name");
		Column<String> LAST_NAME = TYPE.stringColumn("Last name");
		Column<String> EMAIL = TYPE.stringColumn("Email");
	}

	EntityDefinition sellerContactInfo() {
		return SellerContactInfo.TYPE.as(
										SellerContactInfo.ID.as()
														.primaryKey()
														.generator(sequence("petstore.sellercontactinfo_seq"))
														.name("contactinfoid"),
										SellerContactInfo.FIRST_NAME.as()
														.column()
														.caption(SellerContactInfo.FIRST_NAME.name())
														.searchable(true)
														.name("firstname")
														.maximumLength(24)
														.nullable(false),
										SellerContactInfo.LAST_NAME.as()
														.column()
														.caption(SellerContactInfo.LAST_NAME.name())
														.searchable(true)
														.name("lastname")
														.maximumLength(24)
														.nullable(false),
										SellerContactInfo.EMAIL.as()
														.column()
														.caption(SellerContactInfo.EMAIL.name())
														.name("email")
														.maximumLength(24)
														.nullable(false))
						.table("petstore.sellercontactinfo")
						.orderBy(ascending(SellerContactInfo.LAST_NAME, SellerContactInfo.FIRST_NAME))
						.formatter(EntityFormatter.builder()
										.value(SellerContactInfo.LAST_NAME)
										.text(", ")
										.value(SellerContactInfo.FIRST_NAME)
										.build())
						.caption("Seller info")
						.build();
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

	EntityDefinition item() {
		return Item.TYPE.as(
										Item.ID.as()
														.primaryKey()
														.generator(sequence("petstore.item_seq"))
														.name("itemid"),
										Item.PRODUCT_ID.as()
														.column()
														.name("productid")
														.nullable(false),
										Item.PRODUCT_FK.as()
														.foreignKey()
														.referenceDepth(2)
														.caption(Item.PRODUCT_FK.name()),
										Item.NAME.as()
														.column()
														.caption(Item.NAME.name())
														.name("name")
														.maximumLength(30)
														.nullable(false),
										Item.DESCRIPTION.as()
														.column()
														.caption(Item.DESCRIPTION.name())
														.name("description")
														.maximumLength(500)
														.nullable(false),
										Item.IMAGE_URL.as()
														.column()
														.caption(Item.IMAGE_URL.name())
														.name("imageurl")
														.maximumLength(55)
														.hidden(true),
										Item.IMAGE_THUMB_URL.as()
														.column()
														.caption(Item.IMAGE_THUMB_URL.name())
														.name("imagethumburl")
														.maximumLength(55)
														.hidden(true),
										Item.PRICE.as()
														.column()
														.caption(Item.PRICE.name())
														.name("price")
														.nullable(false)
														.fractionDigits(2),
										Item.CONTACT_INFO_ID.as()
														.column()
														.name("contactinfo_contactinfoid")
														.nullable(false),
										Item.CONTACT_INFO_FK.as()
														.foreignKey()
														.caption(Item.CONTACT_INFO_FK.name()),
										Item.ADDRESS_ID.as()
														.column()
														.name("address_addressid")
														.nullable(false),
										Item.ADDRESS_FK.as()
														.foreignKey()
														.caption("Address"),
										// tag::booleanColumn[]
										Item.DISABLED.as()
														.column()
														.converter(Integer.class, new BooleanConverter())
														.caption(Item.DISABLED.name())
														.name("disabled")
														.defaultValue(false)
														.nullable(false)
										// end::booleanColumn[]
						).table("petstore.item")
						.orderBy(ascending(Item.NAME))
						.formatter(EntityFormatter.builder()
										.value(Item.PRODUCT_FK)
										.text(" - ")
										.value(Item.NAME)
										.build())
						.caption("Items")
						.build();
	}

	public interface Tag {
		EntityType TYPE = DOMAIN.entityType("tag");

		Column<Integer> ID = TYPE.integerColumn("Tag id");
		Column<String> TAG = TYPE.stringColumn("Tag");
		Column<Integer> REFCOUNT = TYPE.integerColumn("Reference count");
	}

	EntityDefinition tag() {
		return Tag.TYPE.as(
										Tag.ID.as()
														.primaryKey()
														.generator(sequence("petstore.tag_seq"))
														.name("tagid"),
										Tag.TAG.as()
														.column()
														.caption(Tag.TAG.name())
														.name("tag")
														.maximumLength(30)
														.nullable(false),
										Tag.REFCOUNT.as()
														.subquery("""
																		SELECT COUNT(*)
																		FROM petstore.tag_item
																		WHERE tagid = tag.tagid""")
														.caption(Tag.REFCOUNT.name())
														.name("refcount"))
						.table("petstore.tag")
						.orderBy(ascending(Tag.TAG))
						.selectTable("petstore.tag tag")
						.formatter(Tag.TAG)
						.caption("Tags")
						.build();
	}

	public interface TagItem {
		EntityType TYPE = DOMAIN.entityType("tag_item");

		Column<Integer> ITEM_ID = TYPE.integerColumn("Item id");
		Column<Integer> TAG_ID = TYPE.integerColumn("Tag id");

		ForeignKey ITEM_FK = TYPE.foreignKey("Item", ITEM_ID, Item.ID);
		ForeignKey TAG_FK = TYPE.foreignKey("Tag", TAG_ID, Tag.ID);
	}

	EntityDefinition tagItem() {
		return TagItem.TYPE.as(
										TagItem.ITEM_ID.as()
														.primaryKey(0)
														.name("itemid"),
										TagItem.ITEM_FK.as()
														.foreignKey()
														.referenceDepth(3)
														.caption(TagItem.ITEM_FK.name()),
										TagItem.TAG_ID.as()
														.primaryKey(1)
														.name("tagid"),
										TagItem.TAG_FK.as()
														.foreignKey()
														.caption(TagItem.TAG_FK.name()))
						.table("petstore.tag_item")
						.formatter(EntityFormatter.builder()
										.value(TagItem.ITEM_FK)
										.text(" - ")
										.value(TagItem.TAG_FK)
										.build())
						.caption("Item tags")
						.build();
	}

	// tag::booleanConverter[]
	private static final class BooleanConverter implements Converter<Boolean, Integer> {

		@Override
		public Integer toColumn(Boolean value, Statement statement) throws SQLException {
			return value ? 1 : 0;
		}

		@Override
		public Boolean fromColumn(Integer columnValue) throws SQLException {
			return columnValue.intValue() == 1;
		}
	}
	// end::booleanConverter[]
}
