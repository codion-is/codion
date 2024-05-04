package is.codion.test.petstore.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public final class Petstore extends DomainModel {
	public static final DomainType DOMAIN = domainType(Petstore.class);

	public Petstore() {
		super(DOMAIN);
		add(address(), category(), item(),
				product(), sellercontactinfo(), tag(),
				tagItem());
	}

	static EntityDefinition address() {
		return Address.TYPE.define(
				Address.ADDRESSID.define()
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
				Category.CATEGORYID.define()
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
				Category.IMAGEURL.define()
					.column()
					.caption("Imageurl")
					.maximumLength(55))
			.keyGenerator(identity())
			.caption("Category")
			.build();
	}

	static EntityDefinition item() {
		return Item.TYPE.define(
				Item.ITEMID.define()
					.primaryKey(),
				Item.PRODUCTID.define()
					.column()
					.nullable(false),
				Item.PRODUCTID_FK.define()
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
				Item.IMAGEURL.define()
					.column()
					.caption("Imageurl")
					.maximumLength(55),
				Item.IMAGETHUMBURL.define()
					.column()
					.caption("Imagethumburl")
					.maximumLength(55),
				Item.PRICE.define()
					.column()
					.caption("Price")
					.nullable(false)
					.maximumFractionDigits(2),
				Item.ADDRESS_ADDRESSID.define()
					.column()
					.nullable(false),
				Item.ADDRESS_ADDRESSID_FK.define()
					.foreignKey()
					.caption("Address"),
				Item.CONTACTINFO_CONTACTINFOID.define()
					.column()
					.nullable(false),
				Item.CONTACTINFO_CONTACTINFOID_FK.define()
					.foreignKey()
					.caption("Sellercontactinfo"),
				Item.TOTALSCORE.define()
					.column()
					.caption("Totalscore"),
				Item.NUMBEROFVOTES.define()
					.column()
					.caption("Numberofvotes"),
				Item.DISABLED.define()
					.column()
					.caption("Disabled"))
			.keyGenerator(identity())
			.caption("Item")
			.build();
	}

	static EntityDefinition product() {
		return Product.TYPE.define(
				Product.PRODUCTID.define()
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
				Product.IMAGEURL.define()
					.column()
					.caption("Imageurl")
					.maximumLength(55))
			.keyGenerator(identity())
			.caption("Product")
			.description("The available products")
			.build();
	}

	static EntityDefinition sellercontactinfo() {
		return Sellercontactinfo.TYPE.define(
				Sellercontactinfo.CONTACTINFOID.define()
					.primaryKey(),
				Sellercontactinfo.LASTNAME.define()
					.column()
					.caption("Lastname")
					.nullable(false)
					.maximumLength(24),
				Sellercontactinfo.FIRSTNAME.define()
					.column()
					.caption("Firstname")
					.nullable(false)
					.maximumLength(24),
				Sellercontactinfo.EMAIL.define()
					.column()
					.caption("Email")
					.nullable(false)
					.maximumLength(24))
			.keyGenerator(identity())
			.caption("Sellercontactinfo")
			.build();
	}

	static EntityDefinition tag() {
		return Tag.TYPE.define(
				Tag.TAGID.define()
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
				TagItem.TAGID.define()
					.primaryKey(0),
				TagItem.TAGID_FK.define()
					.foreignKey()
					.caption("Tag"),
				TagItem.ITEMID.define()
					.primaryKey(1),
				TagItem.ITEMID_FK.define()
					.foreignKey()
					.caption("Item"))
			.caption("Tag item")
			.build();
	}

	public interface Address {
		EntityType TYPE = DOMAIN.entityType("petstore.address");

		Column<Integer> ADDRESSID = TYPE.integerColumn("addressid");

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

		Column<Integer> CATEGORYID = TYPE.integerColumn("categoryid");

		Column<String> NAME = TYPE.stringColumn("name");

		Column<String> DESCRIPTION = TYPE.stringColumn("description");

		Column<String> IMAGEURL = TYPE.stringColumn("imageurl");
	}

	public interface Item {
		EntityType TYPE = DOMAIN.entityType("petstore.item");

		Column<Integer> ITEMID = TYPE.integerColumn("itemid");

		Column<Integer> PRODUCTID = TYPE.integerColumn("productid");

		Column<String> NAME = TYPE.stringColumn("name");

		Column<String> DESCRIPTION = TYPE.stringColumn("description");

		Column<String> IMAGEURL = TYPE.stringColumn("imageurl");

		Column<String> IMAGETHUMBURL = TYPE.stringColumn("imagethumburl");

		Column<Double> PRICE = TYPE.doubleColumn("price");

		Column<Integer> ADDRESS_ADDRESSID = TYPE.integerColumn("address_addressid");

		Column<Integer> CONTACTINFO_CONTACTINFOID = TYPE.integerColumn("contactinfo_contactinfoid");

		Column<Integer> TOTALSCORE = TYPE.integerColumn("totalscore");

		Column<Integer> NUMBEROFVOTES = TYPE.integerColumn("numberofvotes");

		Column<Integer> DISABLED = TYPE.integerColumn("disabled");

		ForeignKey PRODUCTID_FK = TYPE.foreignKey("productid_fk", PRODUCTID, Product.PRODUCTID);

		ForeignKey ADDRESS_ADDRESSID_FK = TYPE.foreignKey("address_addressid_fk", ADDRESS_ADDRESSID, Address.ADDRESSID);

		ForeignKey CONTACTINFO_CONTACTINFOID_FK = TYPE.foreignKey("contactinfo_contactinfoid_fk", CONTACTINFO_CONTACTINFOID, Sellercontactinfo.CONTACTINFOID);
	}

	public interface Product {
		EntityType TYPE = DOMAIN.entityType("petstore.product");

		Column<Integer> PRODUCTID = TYPE.integerColumn("productid");

		Column<Integer> CATEGORYID = TYPE.integerColumn("categoryid");

		Column<String> NAME = TYPE.stringColumn("name");

		Column<String> DESCRIPTION = TYPE.stringColumn("description");

		Column<String> IMAGEURL = TYPE.stringColumn("imageurl");

		ForeignKey CATEGORYID_FK = TYPE.foreignKey("categoryid_fk", CATEGORYID, Category.CATEGORYID);
	}

	public interface Sellercontactinfo {
		EntityType TYPE = DOMAIN.entityType("petstore.sellercontactinfo");

		Column<Integer> CONTACTINFOID = TYPE.integerColumn("contactinfoid");

		Column<String> LASTNAME = TYPE.stringColumn("lastname");

		Column<String> FIRSTNAME = TYPE.stringColumn("firstname");

		Column<String> EMAIL = TYPE.stringColumn("email");
	}

	public interface Tag {
		EntityType TYPE = DOMAIN.entityType("petstore.tag");

		Column<Integer> TAGID = TYPE.integerColumn("tagid");

		Column<String> TAG = TYPE.stringColumn("tag");
	}

	public interface TagItem {
		EntityType TYPE = DOMAIN.entityType("petstore.tag_item");

		Column<Integer> TAGID = TYPE.integerColumn("tagid");

		Column<Integer> ITEMID = TYPE.integerColumn("itemid");

		ForeignKey TAGID_FK = TYPE.foreignKey("tagid_fk", TAGID, Tag.TAGID);

		ForeignKey ITEMID_FK = TYPE.foreignKey("itemid_fk", ITEMID, Item.ITEMID);
	}
}
