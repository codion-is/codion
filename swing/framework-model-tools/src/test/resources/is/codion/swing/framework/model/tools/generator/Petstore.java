package is.codion.petstore.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.petstore.domain.Petstore.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.petstore.domain.Petstore.Address;
import is.codion.petstore.domain.Petstore.Category;
import is.codion.petstore.domain.Petstore.ContactInfo;
import is.codion.petstore.domain.Petstore.Item;
import is.codion.petstore.domain.Petstore.ItemTags;
import is.codion.petstore.domain.Petstore.Product;
import is.codion.petstore.domain.Petstore.Tag;
import is.codion.petstore.domain.Petstore.TagItem;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public final class Petstore extends DomainModel {
	public static final DomainType DOMAIN = domainType(Petstore.class);

	public Petstore() {
		super(DOMAIN);
		add(address(), category(), contactInfo(),
				product(), item(), itemTags(),
				tag(), tagItem());
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
					.caption("Image"))
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
					.maximumLength(55))
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
					.maximumLength(24))
			.keyGenerator(identity())
			.caption("Contact info")
			.build();
	}

	static EntityDefinition product() {
		return Product.TYPE.define(
				Product.PRODUCT_ID.define()
					.primaryKey(),
				Product.CATEGORYID.define()
					.column()
					.nullable(false),
				Product.CATEGORYID_FK.define()
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
					.maximumLength(55))
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
				Item.PRODUCT_ID_FK.define()
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
				Item.ADDRESS_ID_FK.define()
					.foreignKey()
					.caption("Address"),
				Item.CONTACT_INFO_ID.define()
					.column()
					.nullable(false),
				Item.CONTACT_INFO_ID_FK.define()
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
					.columnHasDefaultValue(true))
			.keyGenerator(identity())
			.caption("Item")
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
					.maximumLength(30))
			.keyGenerator(identity())
			.caption("Tag")
			.build();
	}

	static EntityDefinition tagItem() {
		return TagItem.TYPE.define(
				TagItem.TAG_ID.define()
					.primaryKey(0),
				TagItem.TAG_ID_FK.define()
					.foreignKey()
					.caption("Tag"),
				TagItem.ITEM_ID.define()
					.primaryKey(1),
				TagItem.ITEM_ID_FK.define()
					.foreignKey()
					.caption("Item"))
			.caption("Tag item")
			.build();
	}

	public interface Address {
		EntityType TYPE = DOMAIN.entityType("petstore.address");

		Column<Integer> ADDRESS_ID = TYPE.integerColumn("address_id");
		Column<String> STREET1 = TYPE.stringColumn("street1");
		Column<String> STREET2 = TYPE.stringColumn("street2");
		Column<String> CITY = TYPE.stringColumn("city");
		Column<String> STATE = TYPE.stringColumn("state");
		Column<Integer> ZIP = TYPE.integerColumn("zip");
		Column<Double> LATITUDE = TYPE.doubleColumn("latitude");
		Column<Double> LONGITUDE = TYPE.doubleColumn("longitude");
		Column<Object> LOCATION = TYPE.column("location", Object.class);
		Column<byte[]> IMAGE = TYPE.byteArrayColumn("image");
	}

	public interface Category {
		EntityType TYPE = DOMAIN.entityType("petstore.category");

		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
	}

	public interface ContactInfo {
		EntityType TYPE = DOMAIN.entityType("petstore.contact_info");

		Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("contact_info_id");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> EMAIL = TYPE.stringColumn("email");
	}

	public interface Product {
		EntityType TYPE = DOMAIN.entityType("petstore.product");

		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<Integer> CATEGORYID = TYPE.integerColumn("categoryid");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");

		ForeignKey CATEGORYID_FK = TYPE.foreignKey("categoryid_fk", CATEGORYID, Category.CATEGORY_ID);
	}

	public interface Item {
		EntityType TYPE = DOMAIN.entityType("petstore.item");

		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");
		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
		Column<String> IMAGE_THUMB_URL = TYPE.stringColumn("image_thumb_url");
		Column<Double> PRICE = TYPE.doubleColumn("price");
		Column<Integer> ADDRESS_ID = TYPE.integerColumn("address_id");
		Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("contact_info_id");
		Column<Integer> TOTAL_SCORE = TYPE.integerColumn("total_score");
		Column<Integer> NUMBER_OF_VOTES = TYPE.integerColumn("number_of_votes");
		Column<Integer> DISABLED = TYPE.integerColumn("disabled");

		ForeignKey PRODUCT_ID_FK = TYPE.foreignKey("product_id_fk", PRODUCT_ID, Product.PRODUCT_ID);
		ForeignKey ADDRESS_ID_FK = TYPE.foreignKey("address_id_fk", ADDRESS_ID, Address.ADDRESS_ID);
		ForeignKey CONTACT_INFO_ID_FK = TYPE.foreignKey("contact_info_id_fk", CONTACT_INFO_ID, ContactInfo.CONTACT_INFO_ID);
	}

	public interface ItemTags {
		EntityType TYPE = DOMAIN.entityType("petstore.item_tags");

		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> TAG = TYPE.stringColumn("tag");
	}

	public interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<String> TAG = TYPE.stringColumn("tag");
	}

	public interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");

		ForeignKey TAG_ID_FK = TYPE.foreignKey("tag_id_fk", TAG_ID, Tag.TAG_ID);
		ForeignKey ITEM_ID_FK = TYPE.foreignKey("item_id_fk", ITEM_ID, Item.ITEM_ID);
	}
}