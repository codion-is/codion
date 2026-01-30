package is.codion.petstore.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.identity;
import static is.codion.petstore.domain.Petstore.Address;
import static is.codion.petstore.domain.Petstore.Category;
import static is.codion.petstore.domain.Petstore.ContactInfo;
import static is.codion.petstore.domain.Petstore.DOMAIN;
import static is.codion.petstore.domain.Petstore.Item;
import static is.codion.petstore.domain.Petstore.ItemTagsView;
import static is.codion.petstore.domain.Petstore.Product;
import static is.codion.petstore.domain.Petstore.Tag;
import static is.codion.petstore.domain.Petstore.TagItem;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import java.time.LocalDateTime;

public final class Petstore extends DomainModel {
	public static final DomainType DOMAIN = domainType(Petstore.class);

	public Petstore() {
		super(DOMAIN);
		add(address(), category(), contactInfo(),
				itemTagsView(), tag(), product(),
				item(), tagItem());
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

	static EntityDefinition address() {
		return Address.TYPE.as(
				Address.ADDRESS_ID.as()
					.primaryKey()
					.generator(identity()),
				Address.STREET1.as()
					.column()
					.caption("Street1")
					.nullable(false)
					.maximumLength(55),
				Address.STREET2.as()
					.column()
					.caption("Street2")
					.maximumLength(55),
				Address.CITY.as()
					.column()
					.caption("City")
					.nullable(false)
					.maximumLength(55),
				Address.STATE.as()
					.column()
					.caption("State")
					.nullable(false)
					.maximumLength(25),
				Address.ZIP.as()
					.column()
					.caption("Zip")
					.nullable(false),
				Address.LATITUDE.as()
					.column()
					.caption("Latitude")
					.nullable(false)
					.fractionDigits(2),
				Address.LONGITUDE.as()
					.column()
					.caption("Longitude")
					.nullable(false)
					.fractionDigits(2),
				Address.LOCATION.as()
					.column()
					.caption("Location"),
				Address.IMAGE.as()
					.column()
					.caption("Image"))
			.caption("Address")
			.build();
	}

	public interface Category {
		EntityType TYPE = DOMAIN.entityType("petstore.category");

		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");

		static Dto dto(Entity category) {
			return category == null ? null :
				new Dto(category.get(CATEGORY_ID),
					category.get(NAME),
					category.get(DESCRIPTION),
					category.get(IMAGE_URL));
		}

		record Dto(Integer categoryId, String name, String description, String imageUrl) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CATEGORY_ID, categoryId)
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.build();
			}
		}
	}

	static EntityDefinition category() {
		return Category.TYPE.as(
				Category.CATEGORY_ID.as()
					.primaryKey()
					.generator(identity()),
				Category.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(25),
				Category.DESCRIPTION.as()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(255),
				Category.IMAGE_URL.as()
					.column()
					.caption("Image url")
					.maximumLength(55))
			.caption("Category")
			.build();
	}

	public interface ContactInfo {
		EntityType TYPE = DOMAIN.entityType("petstore.contact_info");

		Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("contact_info_id");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> EMAIL = TYPE.stringColumn("email");

		static Dto dto(Entity contactInfo) {
			return contactInfo == null ? null :
				new Dto(contactInfo.get(CONTACT_INFO_ID),
					contactInfo.get(LAST_NAME),
					contactInfo.get(FIRST_NAME),
					contactInfo.get(EMAIL));
		}

		record Dto(Integer contactInfoId, String lastName, String firstName, String email) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CONTACT_INFO_ID, contactInfoId)
					.with(LAST_NAME, lastName)
					.with(FIRST_NAME, firstName)
					.with(EMAIL, email)
					.build();
			}
		}
	}

	static EntityDefinition contactInfo() {
		return ContactInfo.TYPE.as(
				ContactInfo.CONTACT_INFO_ID.as()
					.primaryKey()
					.generator(identity()),
				ContactInfo.LAST_NAME.as()
					.column()
					.caption("Last name")
					.nullable(false)
					.maximumLength(24),
				ContactInfo.FIRST_NAME.as()
					.column()
					.caption("First name")
					.nullable(false)
					.maximumLength(24),
				ContactInfo.EMAIL.as()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(24))
			.caption("Contact info")
			.build();
	}

	public interface ItemTagsView {
		EntityType TYPE = DOMAIN.entityType("petstore.item_tags");

		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> TAG = TYPE.stringColumn("tag");

		static Dto dto(Entity itemTagsView) {
			return itemTagsView == null ? null :
				new Dto(itemTagsView.get(NAME),
					itemTagsView.get(TAG));
		}

		record Dto(String name, String tag) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(NAME, name)
					.with(TAG, tag)
					.build();
			}
		}
	}

	static EntityDefinition itemTagsView() {
		return ItemTagsView.TYPE.as(
				ItemTagsView.NAME.as()
					.column()
					.caption("Name"),
				ItemTagsView.TAG.as()
					.column()
					.caption("Tag"))
			.caption("Item tags")
			.readOnly(true)
			.build();
	}

	public interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<String> TAG = TYPE.stringColumn("tag");
	}

	static EntityDefinition tag() {
		return Tag.TYPE.as(
				Tag.TAG_ID.as()
					.primaryKey()
					.generator(identity()),
				Tag.TAG.as()
					.column()
					.caption("Tag")
					.nullable(false)
					.maximumLength(30))
			.caption("Tag")
			.build();
	}

	public interface Product {
		EntityType TYPE = DOMAIN.entityType("petstore.product");

		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");

		ForeignKey CATEGORY_FK = TYPE.foreignKey("category_fk", CATEGORY_ID, Category.CATEGORY_ID);

		static Dto dto(Entity product) {
			return product == null ? null :
				new Dto(product.get(PRODUCT_ID),
					Category.dto(product.get(CATEGORY_FK)),
					product.get(NAME),
					product.get(DESCRIPTION),
					product.get(IMAGE_URL),
					product.get(INSERT_TIME),
					product.get(INSERT_USER));
		}

		record Dto(Integer productId, Category.Dto category, String name, String description,
				String imageUrl, LocalDateTime insertTime, String insertUser) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(PRODUCT_ID, productId)
					.with(CATEGORY_FK, category.entity(entities))
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.with(INSERT_TIME, insertTime)
					.with(INSERT_USER, insertUser)
					.build();
			}
		}
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
					.foreignKey()
					.caption("Category"),
				Product.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(25)
					.description("The product name"),
				Product.DESCRIPTION.as()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(255),
				Product.IMAGE_URL.as()
					.column()
					.caption("Image url")
					.maximumLength(55),
				Product.INSERT_TIME.as()
					.column()
					.caption("Insert time")
					.readOnly(true),
				Product.INSERT_USER.as()
					.column()
					.caption("Insert user")
					.readOnly(true))
			.caption("Product")
			.description("The available products")
			.build();
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
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");

		ForeignKey PRODUCT_FK = TYPE.foreignKey("product_fk", PRODUCT_ID, Product.PRODUCT_ID);
		ForeignKey ADDRESS_FK = TYPE.foreignKey("address_fk", ADDRESS_ID, Address.ADDRESS_ID);
		ForeignKey CONTACT_INFO_FK = TYPE.foreignKey("contact_info_fk", CONTACT_INFO_ID, ContactInfo.CONTACT_INFO_ID);

		static Dto dto(Entity item) {
			return item == null ? null :
				new Dto(item.get(ITEM_ID),
					Product.dto(item.get(PRODUCT_FK)),
					item.get(NAME),
					item.get(DESCRIPTION),
					item.get(IMAGE_URL),
					item.get(IMAGE_THUMB_URL),
					item.get(PRICE),
					ContactInfo.dto(item.get(CONTACT_INFO_FK)),
					item.get(TOTAL_SCORE),
					item.get(NUMBER_OF_VOTES),
					item.get(DISABLED),
					item.get(INSERT_TIME),
					item.get(INSERT_USER));
		}

		record Dto(Integer itemId, Product.Dto product, String name, String description, String imageUrl,
				String imageThumbUrl, Double price, ContactInfo.Dto contactInfo, Integer totalScore,
				Integer numberOfVotes, Integer disabled, LocalDateTime insertTime, String insertUser) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ITEM_ID, itemId)
					.with(PRODUCT_FK, product.entity(entities))
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.with(IMAGE_THUMB_URL, imageThumbUrl)
					.with(PRICE, price)
					.with(CONTACT_INFO_FK, contactInfo.entity(entities))
					.with(TOTAL_SCORE, totalScore)
					.with(NUMBER_OF_VOTES, numberOfVotes)
					.with(DISABLED, disabled)
					.with(INSERT_TIME, insertTime)
					.with(INSERT_USER, insertUser)
					.build();
			}
		}
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
					.foreignKey()
					.caption("Product"),
				Item.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(30),
				Item.DESCRIPTION.as()
					.column()
					.caption("Description")
					.nullable(false)
					.maximumLength(500),
				Item.IMAGE_URL.as()
					.column()
					.caption("Image url")
					.maximumLength(55),
				Item.IMAGE_THUMB_URL.as()
					.column()
					.caption("Image thumb url")
					.maximumLength(55),
				Item.PRICE.as()
					.column()
					.caption("Price")
					.nullable(false)
					.fractionDigits(2),
				Item.ADDRESS_ID.as()
					.column()
					.nullable(false),
				Item.ADDRESS_FK.as()
					.foreignKey()
					.caption("Address"),
				Item.CONTACT_INFO_ID.as()
					.column()
					.nullable(false),
				Item.CONTACT_INFO_FK.as()
					.foreignKey()
					.caption("Contact info"),
				Item.TOTAL_SCORE.as()
					.column()
					.caption("Total score"),
				Item.NUMBER_OF_VOTES.as()
					.column()
					.caption("Number of votes"),
				Item.DISABLED.as()
					.column()
					.caption("Disabled")
					.nullable(false)
					.withDefault(true),
				Item.INSERT_TIME.as()
					.column()
					.caption("Insert time")
					.readOnly(true),
				Item.INSERT_USER.as()
					.column()
					.caption("Insert user")
					.readOnly(true))
			.caption("Item")
			.build();
	}

	public interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");

		ForeignKey TAG_FK = TYPE.foreignKey("tag_fk", TAG_ID, Tag.TAG_ID);
		ForeignKey ITEM_FK = TYPE.foreignKey("item_fk", ITEM_ID, Item.ITEM_ID);
	}

	static EntityDefinition tagItem() {
		return TagItem.TYPE.as(
				TagItem.TAG_ID.as()
					.primaryKey(0),
				TagItem.TAG_FK.as()
					.foreignKey()
					.caption("Tag"),
				TagItem.ITEM_ID.as()
					.primaryKey(1),
				TagItem.ITEM_FK.as()
					.foreignKey()
					.caption("Item"))
			.caption("Tag item")
			.build();
	}
}