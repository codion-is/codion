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
import is.codion.framework.domain.entity.attribute.ColumnTemplate;
import java.time.LocalDateTime;

public final class PetstoreImpl extends DomainModel {
	private static final ColumnTemplate<Integer> IDENTITY_KEY = column -> column.as()
		.primaryKey()
		.generator(identity());

	private static final ColumnTemplate<LocalDateTime> INSERT_TIME = column -> column.as()
		.column()
		.readOnly(true)
		.hidden(true);

	private static final ColumnTemplate<String> INSERT_USER = column -> column.as()
		.column()
		.readOnly(true)
		.hidden(true);

	public PetstoreImpl() {
		super(DOMAIN);
		add(address(), category(), contactInfo(),
				itemTagsView(), tag(), product(),
				item(), tagItem());
	}

	EntityDefinition address() {
		return Address.TYPE.as()
			.attributes(
				Address.ADDRESS_ID.as(IDENTITY_KEY),
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
					.nullable(false),
				Address.LONGITUDE.as()
					.column()
					.nullable(false),
				Address.LOCATION.as()
					.column(),
				Address.IMAGE.as()
					.column())
			.build();
	}

	EntityDefinition category() {
		return Category.TYPE.as()
			.attributes(
				Category.CATEGORY_ID.as(IDENTITY_KEY),
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

	EntityDefinition contactInfo() {
		return ContactInfo.TYPE.as()
			.attributes(
				ContactInfo.CONTACT_INFO_ID.as(IDENTITY_KEY),
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

	EntityDefinition itemTagsView() {
		return ItemTagsView.TYPE.as()
			.attributes(
				ItemTagsView.NAME.as()
					.column(),
				ItemTagsView.TAG.as()
					.column())
			.readOnly(true)
			.build();
	}

	EntityDefinition tag() {
		return Tag.TYPE.as()
			.attributes(
				Tag.TAG_ID.as(IDENTITY_KEY),
				Tag.TAG.as()
					.column()
					.nullable(false)
					.maximumLength(30))
			.build();
	}

	EntityDefinition product() {
		return Product.TYPE.as()
			.attributes(
				Product.PRODUCT_ID.as(IDENTITY_KEY),
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
				Product.INSERT_TIME.as(INSERT_TIME),
				Product.INSERT_USER.as(INSERT_USER))
			.build();
	}

	EntityDefinition item() {
		return Item.TYPE.as()
			.attributes(
				Item.ITEM_ID.as(IDENTITY_KEY),
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
				Item.INSERT_TIME.as(INSERT_TIME),
				Item.INSERT_USER.as(INSERT_USER))
			.build();
	}

	EntityDefinition tagItem() {
		return TagItem.TYPE.as()
			.attributes(
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