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
		return Address.TYPE.as(
				Address.ADDRESS_ID.as()
					.primaryKey()
					.generator(identity()),
				Address.STREET1.as()
					.column()
					.nullable(false)
					.maximumLength(55),
				Address.STREET2.as()
					.column()
					.maximumLength(55),
				Address.CITY.as()
					.column()
					.nullable(false)
					.maximumLength(55),
				Address.STATE.as()
					.column()
					.nullable(false)
					.maximumLength(25),
				Address.ZIP.as()
					.column()
					.nullable(false),
				Address.LATITUDE.as()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Address.LONGITUDE.as()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Address.LOCATION.as()
					.column(),
				Address.IMAGE.as()
					.column())
			.build();
	}

	static EntityDefinition category() {
		return Category.TYPE.as(
				Category.CATEGORY_ID.as()
					.primaryKey()
					.generator(identity()),
				Category.NAME.as()
					.column()
					.nullable(false)
					.maximumLength(25),
				Category.DESCRIPTION.as()
					.column()
					.nullable(false)
					.maximumLength(255),
				Category.IMAGE_URL.as()
					.column()
					.maximumLength(55))
			.build();
	}

	static EntityDefinition contactInfo() {
		return ContactInfo.TYPE.as(
				ContactInfo.CONTACT_INFO_ID.as()
					.primaryKey()
					.generator(identity()),
				ContactInfo.LAST_NAME.as()
					.column()
					.nullable(false)
					.maximumLength(24),
				ContactInfo.FIRST_NAME.as()
					.column()
					.nullable(false)
					.maximumLength(24),
				ContactInfo.EMAIL.as()
					.column()
					.nullable(false)
					.maximumLength(24))
			.build();
	}

	static EntityDefinition itemTagsView() {
		return ItemTagsView.TYPE.as(
				ItemTagsView.NAME.as()
					.column(),
				ItemTagsView.TAG.as()
					.column())
			.readOnly(true)
			.build();
	}

	static EntityDefinition tag() {
		return Tag.TYPE.as(
				Tag.TAG_ID.as()
					.primaryKey()
					.generator(identity()),
				Tag.TAG.as()
					.column()
					.nullable(false)
					.maximumLength(30))
			.build();
	}

	static EntityDefinition product() {
		return Product.TYPE.as(
				Product.PRODUCT_ID.as()
					.primaryKey()
					.generator(identity()),
				Product.CATEGORY_ID.as()
					.column()
					.nullable(false),
				Product.CATEGORY_FK.as()
					.foreignKey(),
				Product.NAME.as()
					.column()
					.nullable(false)
					.maximumLength(25),
				Product.DESCRIPTION.as()
					.column()
					.nullable(false)
					.maximumLength(255),
				Product.IMAGE_URL.as()
					.column()
					.maximumLength(55),
				Product.INSERT_TIME.as()
					.column()
					.readOnly(true),
				Product.INSERT_USER.as()
					.column()
					.readOnly(true))
			.build();
	}

	static EntityDefinition item() {
		return Item.TYPE.as(
				Item.ITEM_ID.as()
					.primaryKey()
					.generator(identity()),
				Item.PRODUCT_ID.as()
					.column()
					.nullable(false),
				Item.PRODUCT_FK.as()
					.foreignKey(),
				Item.NAME.as()
					.column()
					.nullable(false)
					.maximumLength(30),
				Item.DESCRIPTION.as()
					.column()
					.nullable(false)
					.maximumLength(500),
				Item.IMAGE_URL.as()
					.column()
					.maximumLength(55),
				Item.IMAGE_THUMB_URL.as()
					.column()
					.maximumLength(55),
				Item.PRICE.as()
					.column()
					.nullable(false)
					.fractionDigits(2),
				Item.ADDRESS_ID.as()
					.column()
					.nullable(false),
				Item.ADDRESS_FK.as()
					.foreignKey(),
				Item.CONTACT_INFO_ID.as()
					.column()
					.nullable(false),
				Item.CONTACT_INFO_FK.as()
					.foreignKey(),
				Item.TOTAL_SCORE.as()
					.column(),
				Item.NUMBER_OF_VOTES.as()
					.column(),
				Item.DISABLED.as()
					.column()
					.nullable(false)
					.withDefault(true),
				Item.INSERT_TIME.as()
					.column()
					.readOnly(true),
				Item.INSERT_USER.as()
					.column()
					.readOnly(true))
			.build();
	}

	static EntityDefinition tagItem() {
		return TagItem.TYPE.as(
				TagItem.TAG_ID.as()
					.primaryKey(0),
				TagItem.TAG_FK.as()
					.foreignKey(),
				TagItem.ITEM_ID.as()
					.primaryKey(1),
				TagItem.ITEM_FK.as()
					.foreignKey())
			.build();
	}
}