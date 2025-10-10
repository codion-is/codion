package is.codion.petstore.domain.api;

import static is.codion.framework.domain.DomainType.domainType;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import java.time.LocalDateTime;

public interface Petstore {
	DomainType DOMAIN = domainType(Petstore.class);

	interface Address {
		EntityType TYPE = DOMAIN.entityType("petstore.address", Address.class.getName());

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

	interface Category {
		EntityType TYPE = DOMAIN.entityType("petstore.category", Category.class.getName());

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

	interface ContactInfo {
		EntityType TYPE = DOMAIN.entityType("petstore.contact_info", ContactInfo.class.getName());

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

	interface ItemTags {
		EntityType TYPE = DOMAIN.entityType("petstore.item_tags", ItemTags.class.getName());

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

	interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag", Tag.class.getName());

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<String> TAG = TYPE.stringColumn("tag");
	}

	interface Product {
		EntityType TYPE = DOMAIN.entityType("petstore.product", Product.class.getName());

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

	interface Item {
		EntityType TYPE = DOMAIN.entityType("petstore.item", Item.class.getName());

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

	interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item", TagItem.class.getName());

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");

		ForeignKey TAG_FK = TYPE.foreignKey("tag_fk", TAG_ID, Tag.TAG_ID);
		ForeignKey ITEM_FK = TYPE.foreignKey("item_fk", ITEM_ID, Item.ITEM_ID);
	}
}