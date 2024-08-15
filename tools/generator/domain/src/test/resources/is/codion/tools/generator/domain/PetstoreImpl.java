package is.codion.petstore.domain.impl;

import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.petstore.domain.Petstore.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.petstore.domain.Petstore.Address;
import is.codion.petstore.domain.Petstore.Category;
import is.codion.petstore.domain.Petstore.ContactInfo;
import is.codion.petstore.domain.Petstore.Item;
import is.codion.petstore.domain.Petstore.ItemTags;
import is.codion.petstore.domain.Petstore.Product;
import is.codion.petstore.domain.Petstore.Tag;
import is.codion.petstore.domain.Petstore.TagItem;

public final class PetstoreImpl extends DomainModel {
	public PetstoreImpl() {
		super(DOMAIN);
		add(address(), category(), contactInfo(),
				itemTags(), tag(), product(),
				item(), tagItem());
	}

	static EntityDefinition address() {
		return Address.TYPE.define(
				Address.ADDRESS_ID.define()
					.primaryKey(),
				Address.STREET1.define()
					.column()
					.caption("Street1")
					.nullable(false)
					.maximumLength(55),
				Address.STREET2.define()
					.column()
					.caption("Street2")
					.maximumLength(55),
				Address.CITY.define()
					.column()
					.caption("City")
					.nullable(false)
					.maximumLength(55),
				Address.STATE.define()
					.column()
					.caption("State")
					.nullable(false)
					.maximumLength(25),
				Address.ZIP.define()
					.column()
					.caption("Zip")
					.nullable(false),
				Address.LATITUDE.define()
					.column()
					.caption("Latitude")
					.nullable(false)
					.maximumFractionDigits(2),
				Address.LONGITUDE.define()
					.column()
					.caption("Longitude")
					.nullable(false)
					.maximumFractionDigits(2),
				Address.LOCATION.define()
					.column()
					.caption("Location"),
				Address.IMAGE.define()
					.column()
					.caption("Image"),
				Address.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				Address.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				Address.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				Address.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Address")
			.build();
	}

	static EntityDefinition category() {
		return Category.TYPE.define(
				Category.CATEGORY_ID.define()
					.primaryKey(),
				Category.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(25),
				Category.DESCRIPTION.define()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(255),
				Category.IMAGE_URL.define()
					.column()
					.caption("Image url")
					.maximumLength(55),
				Category.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				Category.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				Category.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				Category.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Category")
			.build();
	}

	static EntityDefinition contactInfo() {
		return ContactInfo.TYPE.define(
				ContactInfo.CONTACT_INFO_ID.define()
					.primaryKey(),
				ContactInfo.LAST_NAME.define()
					.column()
					.caption("Last name")
					.nullable(false)
					.maximumLength(24),
				ContactInfo.FIRST_NAME.define()
					.column()
					.caption("First name")
					.nullable(false)
					.maximumLength(24),
				ContactInfo.EMAIL.define()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(24),
				ContactInfo.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				ContactInfo.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				ContactInfo.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				ContactInfo.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Contact info")
			.build();
	}

	static EntityDefinition itemTags() {
		return ItemTags.TYPE.define(
				ItemTags.NAME.define()
					.column()
					.caption("Name"),
				ItemTags.TAG.define()
					.column()
					.caption("Tag"))
			.caption("Item tags")
			.readOnly(true)
			.build();
	}

	static EntityDefinition tag() {
		return Tag.TYPE.define(
				Tag.TAG_ID.define()
					.primaryKey(),
				Tag.TAG.define()
					.column()
					.caption("Tag")
					.nullable(false)
					.maximumLength(30),
				Tag.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				Tag.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				Tag.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				Tag.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Tag")
			.build();
	}

	static EntityDefinition product() {
		return Product.TYPE.define(
				Product.PRODUCT_ID.define()
					.primaryKey(),
				Product.CATEGORY_ID.define()
					.column()
					.nullable(false),
				Product.CATEGORY_FK.define()
					.foreignKey()
					.caption("Category"),
				Product.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(25)
					.description("The product name"),
				Product.DESCRIPTION.define()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(255),
				Product.IMAGE_URL.define()
					.column()
					.caption("Image url")
					.maximumLength(55),
				Product.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				Product.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				Product.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				Product.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Product")
			.description("The available products")
			.build();
	}

	static EntityDefinition item() {
		return Item.TYPE.define(
				Item.ITEM_ID.define()
					.primaryKey(),
				Item.PRODUCT_ID.define()
					.column()
					.nullable(false),
				Item.PRODUCT_FK.define()
					.foreignKey()
					.caption("Product"),
				Item.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(30),
				Item.DESCRIPTION.define()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(500),
				Item.IMAGE_URL.define()
					.column()
					.caption("Image url")
					.maximumLength(55),
				Item.IMAGE_THUMB_URL.define()
					.column()
					.caption("Image thumb url")
					.maximumLength(55),
				Item.PRICE.define()
					.column()
					.caption("Price")
					.nullable(false)
					.maximumFractionDigits(2),
				Item.ADDRESS_ID.define()
					.column()
					.nullable(false),
				Item.ADDRESS_FK.define()
					.foreignKey()
					.caption("Address"),
				Item.CONTACT_INFO_ID.define()
					.column()
					.nullable(false),
				Item.CONTACT_INFO_FK.define()
					.foreignKey()
					.caption("Contact info"),
				Item.TOTAL_SCORE.define()
					.column()
					.caption("Total score"),
				Item.NUMBER_OF_VOTES.define()
					.column()
					.caption("Number of votes"),
				Item.DISABLED.define()
					.column()
					.caption("Disabled")
					.nullable(false)
					.columnHasDefaultValue(true),
				Item.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				Item.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				Item.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				Item.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.keyGenerator(identity())
			.caption("Item")
			.build();
	}

	static EntityDefinition tagItem() {
		return TagItem.TYPE.define(
				TagItem.TAG_ID.define()
					.primaryKey(0),
				TagItem.TAG_FK.define()
					.foreignKey()
					.caption("Tag"),
				TagItem.ITEM_ID.define()
					.primaryKey(1),
				TagItem.ITEM_FK.define()
					.foreignKey()
					.caption("Item"),
				TagItem.INSERT_USER.define()
					.auditInsertUserColumn()
					.caption("Insert user"),
				TagItem.INSERT_TIME.define()
					.auditInsertTimeColumn()
					.caption("Insert time"),
				TagItem.UPDATE_USER.define()
					.auditUpdateTimeColumn()
					.caption("Update user"),
				TagItem.UPDATE_TIME.define()
					.auditUpdateUserColumn()
					.caption("Update time"))
			.caption("Tag item")
			.build();
	}
}