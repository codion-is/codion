package is.codion.petstore.domain.api;

import static is.codion.framework.domain.DomainType.domainType;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import java.time.LocalDateTime;

public interface Petstore {
	DomainType DOMAIN = domainType(Petstore.class);

	interface Address {
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
	}

	interface Category {
		EntityType TYPE = DOMAIN.entityType("petstore.category");

		Column<Integer> CATEGORY_ID = TYPE.integerColumn("category_id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		Column<String> IMAGE_URL = TYPE.stringColumn("image_url");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");
	}

	interface ContactInfo {
		EntityType TYPE = DOMAIN.entityType("petstore.contact_info");

		Column<Integer> CONTACT_INFO_ID = TYPE.integerColumn("contact_info_id");
		Column<String> LAST_NAME = TYPE.stringColumn("last_name");
		Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
		Column<String> EMAIL = TYPE.stringColumn("email");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");
	}

	interface ItemTags {
		EntityType TYPE = DOMAIN.entityType("petstore.item_tags");

		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> TAG = TYPE.stringColumn("tag");
	}

	interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<String> TAG = TYPE.stringColumn("tag");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");
	}

	interface Product {
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
	}

	interface Item {
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
	}

	interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item");

		Column<Integer> TAG_ID = TYPE.integerColumn("tag_id");
		Column<Integer> ITEM_ID = TYPE.integerColumn("item_id");
		Column<String> INSERT_USER = TYPE.stringColumn("insert_user");
		Column<LocalDateTime> INSERT_TIME = TYPE.localDateTimeColumn("insert_time");
		Column<String> UPDATE_USER = TYPE.stringColumn("update_user");
		Column<LocalDateTime> UPDATE_TIME = TYPE.localDateTimeColumn("update_time");

		ForeignKey TAG_FK = TYPE.foreignKey("tag_fk", TAG_ID, Tag.TAG_ID);
		ForeignKey ITEM_FK = TYPE.foreignKey("item_fk", ITEM_ID, Item.ITEM_ID);
	}
}