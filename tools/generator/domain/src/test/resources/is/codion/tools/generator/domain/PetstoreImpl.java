package is.codion.petstore.domain;

import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.petstore.domain.api.Petstore.Address;
import static is.codion.petstore.domain.api.Petstore.Category;
import static is.codion.petstore.domain.api.Petstore.ContactInfo;
import static is.codion.petstore.domain.api.Petstore.DOMAIN;
import static is.codion.petstore.domain.api.Petstore.Item;
import static is.codion.petstore.domain.api.Petstore.ItemTagsView;
import static is.codion.petstore.domain.api.Petstore.Product;
import static is.codion.petstore.domain.api.Petstore.Tag;
import static is.codion.petstore.domain.api.Petstore.TagItem;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;

public final class PetstoreImpl extends DomainModel {
	public PetstoreImpl() {
		super(DOMAIN);
		add(address(), category(), contactInfo(),
				itemTagsView(), tag(), product(),
				item(), tagItem());
	}

	static EntityDefinition address() {
		return Address.TYPE.define(
				Address.ADDRESS_ID.define()
					.primaryKey()
					.generator(identity()),
				Address.STREET1.define()
					.column()
					.nullable(false)
					.maximumLength(55),
				Address.STREET2.define()
					.column()
					.maximumLength(55),
				Address.CITY.define()
					.column()
					.nullable(false)
					.maximumLength(55),
				Address.STATE.define()
					.column()
					.nullable(false)
					.maximumLength(25),
				Address.ZIP.define()
					.column()
					.nullable(false),
				Address.LATITUDE.define()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Address.LONGITUDE.define()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Address.LOCATION.define()
					.column(),
				Address.IMAGE.define()
					.column())
			.build();
	}

	static EntityDefinition category() {
		return Category.TYPE.define(
				Category.CATEGORY_ID.define()
					.primaryKey()
					.generator(identity()),
				Category.NAME.define()
					.column()
					.nullable(false)
					.maximumLength(25),
				Category.DESCRIPTION.define()
					.column()
					.nullable(false)
					.maximumLength(255),
				Category.IMAGE_URL.define()
					.column()
					.maximumLength(55))
			.build();
	}

	static EntityDefinition contactInfo() {
		return ContactInfo.TYPE.define(
				ContactInfo.CONTACT_INFO_ID.define()
					.primaryKey()
					.generator(identity()),
				ContactInfo.LAST_NAME.define()
					.column()
					.nullable(false)
					.maximumLength(24),
				ContactInfo.FIRST_NAME.define()
					.column()
					.nullable(false)
					.maximumLength(24),
				ContactInfo.EMAIL.define()
					.column()
					.nullable(false)
					.maximumLength(24))
			.build();
	}

	static EntityDefinition itemTagsView() {
		return ItemTagsView.TYPE.define(
				ItemTagsView.NAME.define()
					.column(),
				ItemTagsView.TAG.define()
					.column())
			.readOnly(true)
			.build();
	}

	static EntityDefinition tag() {
		return Tag.TYPE.define(
				Tag.TAG_ID.define()
					.primaryKey()
					.generator(identity()),
				Tag.TAG.define()
					.column()
					.nullable(false)
					.maximumLength(30))
			.build();
	}

	static EntityDefinition product() {
		return Product.TYPE.define(
				Product.PRODUCT_ID.define()
					.primaryKey()
					.generator(identity()),
				Product.CATEGORY_ID.define()
					.column()
					.nullable(false),
				Product.CATEGORY_FK.define()
					.foreignKey(),
				Product.NAME.define()
					.column()
					.nullable(false)
					.maximumLength(25),
				Product.DESCRIPTION.define()
					.column()
					.nullable(false)
					.maximumLength(255),
				Product.IMAGE_URL.define()
					.column()
					.maximumLength(55),
				Product.INSERT_TIME.define()
					.column()
					.readOnly(true),
				Product.INSERT_USER.define()
					.column()
					.readOnly(true))
			.build();
	}

	static EntityDefinition item() {
		return Item.TYPE.define(
				Item.ITEM_ID.define()
					.primaryKey()
					.generator(identity()),
				Item.PRODUCT_ID.define()
					.column()
					.nullable(false),
				Item.PRODUCT_FK.define()
					.foreignKey(),
				Item.NAME.define()
					.column()
					.nullable(false)
					.maximumLength(30),
				Item.DESCRIPTION.define()
					.column()
					.nullable(false)
					.maximumLength(500),
				Item.IMAGE_URL.define()
					.column()
					.maximumLength(55),
				Item.IMAGE_THUMB_URL.define()
					.column()
					.maximumLength(55),
				Item.PRICE.define()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Item.ADDRESS_ID.define()
					.column()
					.nullable(false),
				Item.ADDRESS_FK.define()
					.foreignKey(),
				Item.CONTACT_INFO_ID.define()
					.column()
					.nullable(false),
				Item.CONTACT_INFO_FK.define()
					.foreignKey(),
				Item.TOTAL_SCORE.define()
					.column(),
				Item.NUMBER_OF_VOTES.define()
					.column(),
				Item.DISABLED.define()
					.column()
					.nullable(false)
					.withDefault(true),
				Item.INSERT_TIME.define()
					.column()
					.readOnly(true),
				Item.INSERT_USER.define()
					.column()
					.readOnly(true))
			.build();
	}

	static EntityDefinition tagItem() {
		return TagItem.TYPE.define(
				TagItem.TAG_ID.define()
					.primaryKey(0),
				TagItem.TAG_FK.define()
					.foreignKey(),
				TagItem.ITEM_ID.define()
					.primaryKey(1),
				TagItem.ITEM_FK.define()
					.foreignKey())
			.build();
	}
}