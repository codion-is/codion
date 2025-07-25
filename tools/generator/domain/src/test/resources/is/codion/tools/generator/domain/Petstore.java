package is.codion.petstore.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.petstore.domain.Petstore.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
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
import java.time.LocalDateTime;

public final class Petstore extends DomainModel {
	public static final DomainType DOMAIN = domainType(Petstore.class);

	public Petstore() {
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				Address.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				Address.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				Address.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				Category.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				Category.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				Category.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				ContactInfo.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				ContactInfo.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				ContactInfo.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				Tag.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				Tag.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				Tag.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				Product.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				Product.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				Product.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.hasDatabaseDefault(true),
				Item.INSERT_USER.define()
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				Item.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				Item.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				Item.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
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
					.auditColumn()
					.insertUser()
					.caption("Insert user"),
				TagItem.INSERT_TIME.define()
					.auditColumn()
					.insertTime()
					.caption("Insert time"),
				TagItem.UPDATE_USER.define()
					.auditColumn()
					.updateUser()
					.caption("Update user"),
				TagItem.UPDATE_TIME.define()
					.auditColumn()
					.updateTime()
					.caption("Update time"))
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
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		static Dto dto(Entity address) {
			return address == null ? null :
				new Dto(address.get(ADDRESS_ID),
					address.get(STREET1),
					address.get(STREET2),
					address.get(CITY),
					address.get(STATE),
					address.get(ZIP),
					address.get(LATITUDE),
					address.get(LONGITUDE),
					address.get(LOCATION),
					address.get(IMAGE),
					address.get(INSERT_USER),
					address.get(INSERT_TIME),
					address.get(UPDATE_USER),
					address.get(UPDATE_TIME));
		}

		record Dto(Integer addressId, String street1, String street2, String city, String state,
				Integer zip, Double latitude, Double longitude, Object location, byte[] image,
				String insertUser, LocalDateTime insertTime, String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ADDRESS_ID, addressId)
					.with(STREET1, street1)
					.with(STREET2, street2)
					.with(CITY, city)
					.with(STATE, state)
					.with(ZIP, zip)
					.with(LATITUDE, latitude)
					.with(LONGITUDE, longitude)
					.with(LOCATION, location)
					.with(IMAGE, image)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}

	public interface Category {
		EntityType TYPE = DOMAIN.entityType("petstore.category");

		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		static Dto dto(Entity category) {
			return category == null ? null :
				new Dto(category.get(CATEGORY_ID),
					category.get(NAME),
					category.get(DESCRIPTION),
					category.get(IMAGE_URL),
					category.get(INSERT_USER),
					category.get(INSERT_TIME),
					category.get(UPDATE_USER),
					category.get(UPDATE_TIME));
		}

		record Dto(Integer categoryId, String name, String description, String imageUrl,
				String insertUser, LocalDateTime insertTime, String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CATEGORY_ID, categoryId)
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}

	public interface ContactInfo {
		EntityType TYPE = DOMAIN.entityType("petstore.contact_info");

		Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("contact_info_id");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		static Dto dto(Entity contactInfo) {
			return contactInfo == null ? null :
				new Dto(contactInfo.get(CONTACT_INFO_ID),
					contactInfo.get(LAST_NAME),
					contactInfo.get(FIRST_NAME),
					contactInfo.get(EMAIL),
					contactInfo.get(INSERT_USER),
					contactInfo.get(INSERT_TIME),
					contactInfo.get(UPDATE_USER),
					contactInfo.get(UPDATE_TIME));
		}

		record Dto(Integer contactInfoId, String lastName, String firstName, String email,
				String insertUser, LocalDateTime insertTime, String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CONTACT_INFO_ID, contactInfoId)
					.with(LAST_NAME, lastName)
					.with(FIRST_NAME, firstName)
					.with(EMAIL, email)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}

	public interface ItemTags {
		EntityType TYPE = DOMAIN.entityType("petstore.item_tags");

		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> TAG = TYPE.stringColumn("tag");

		static Dto dto(Entity itemTags) {
			return itemTags == null ? null :
				new Dto(itemTags.get(NAME),
					itemTags.get(TAG));
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

	public interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<String> TAG = TYPE.stringColumn("tag");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		static Dto dto(Entity tag) {
			return tag == null ? null :
				new Dto(tag.get(TAG_ID),
					tag.get(TAG),
					tag.get(INSERT_USER),
					tag.get(INSERT_TIME),
					tag.get(UPDATE_USER),
					tag.get(UPDATE_TIME));
		}

		record Dto(Integer tagId, String tag, String insertUser, LocalDateTime insertTime,
				String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(TAG_ID, tagId)
					.with(TAG, tag)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}

	public interface Product {
		EntityType TYPE = DOMAIN.entityType("petstore.product");

		Column<Integer> PRODUCT_ID = TYPE.integerColumn("product_id");
		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		ForeignKey CATEGORY_FK = TYPE.foreignKey("category_fk", CATEGORY_ID, Category.CATEGORY_ID);

		static Dto dto(Entity product) {
			return product == null ? null :
				new Dto(product.get(PRODUCT_ID),
					Category.dto(product.get(CATEGORY_FK)),
					product.get(NAME),
					product.get(DESCRIPTION),
					product.get(IMAGE_URL),
					product.get(INSERT_USER),
					product.get(INSERT_TIME),
					product.get(UPDATE_USER),
					product.get(UPDATE_TIME));
		}

		record Dto(Integer productId, Category.Dto category, String name, String description,
				String imageUrl, String insertUser, LocalDateTime insertTime, String updateUser,
				LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(PRODUCT_ID, productId)
					.with(CATEGORY_FK, category.entity(entities))
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
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
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

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
					Address.dto(item.get(ADDRESS_FK)),
					ContactInfo.dto(item.get(CONTACT_INFO_FK)),
					item.get(TOTAL_SCORE),
					item.get(NUMBER_OF_VOTES),
					item.get(DISABLED),
					item.get(INSERT_USER),
					item.get(INSERT_TIME),
					item.get(UPDATE_USER),
					item.get(UPDATE_TIME));
		}

		record Dto(Integer itemId, Product.Dto product, String name, String description, String imageUrl,
				String imageThumbUrl, Double price, Address.Dto address, ContactInfo.Dto contactInfo,
				Integer totalScore, Integer numberOfVotes, Integer disabled, String insertUser,
				LocalDateTime insertTime, String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ITEM_ID, itemId)
					.with(PRODUCT_FK, product.entity(entities))
					.with(NAME, name)
					.with(DESCRIPTION, description)
					.with(IMAGE_URL, imageUrl)
					.with(IMAGE_THUMB_URL, imageThumbUrl)
					.with(PRICE, price)
					.with(ADDRESS_FK, address.entity(entities))
					.with(CONTACT_INFO_FK, contactInfo.entity(entities))
					.with(TOTAL_SCORE, totalScore)
					.with(NUMBER_OF_VOTES, numberOfVotes)
					.with(DISABLED, disabled)
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}

	public interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		ForeignKey TAG_FK = TYPE.foreignKey("tag_fk", TAG_ID, Tag.TAG_ID);
		ForeignKey ITEM_FK = TYPE.foreignKey("item_fk", ITEM_ID, Item.ITEM_ID);

		static Dto dto(Entity tagItem) {
			return tagItem == null ? null :
				new Dto(Tag.dto(tagItem.get(TAG_FK)),
					Item.dto(tagItem.get(ITEM_FK)),
					tagItem.get(INSERT_USER),
					tagItem.get(INSERT_TIME),
					tagItem.get(UPDATE_USER),
					tagItem.get(UPDATE_TIME));
		}

		record Dto(Tag.Dto tag, Item.Dto item, String insertUser, LocalDateTime insertTime,
				String updateUser, LocalDateTime updateTime) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(TAG_FK, tag.entity(entities))
					.with(ITEM_FK, item.entity(entities))
					.with(INSERT_USER, insertUser)
					.with(INSERT_TIME, insertTime)
					.with(UPDATE_USER, updateUser)
					.with(UPDATE_TIME, updateTime)
					.build();
			}
		}
	}
}