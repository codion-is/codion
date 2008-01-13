/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.model;

import org.jminor.common.db.IdSource;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityProxy;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

/**
 * User: Björn Darri
 * Date: 24.12.2007
 * Time: 13:09:59
 */
public class Petstore {

  public static final String T_ADDRESS = "petstore.address";
  public static final String ADDRESS_ID = "addressid";
  public static final String ADDRESS_STREET_1 = "street1";
  public static final String ADDRESS_STREET_2 = "street2";
  public static final String ADDRESS_CITY = "city";
  public static final String ADDRESS_STATE = "state";
  public static final String ADDRESS_ZIP = "zip";
  public static final String ADDRESS_LATITUDE = "latitude";
  public static final String ADDRESS_LONGITUDE = "longitude";

  public static final String T_CATEGORY = "petstore.category";
  public static final String CATEGORY_ID = "categoryid";
  public static final String CATEGORY_NAME = "name";
  public static final String CATEGORY_DESCRIPTION = "description";
  public static final String CATEGORY_IMAGE_URL = "imageurl";

  public static final String T_ID_GEN = "petstore.id_gen";
  public static final String ID_GEN_KEY = "gen_key";
  public static final String ID_GEN_VALUE = "gen_value";

  public static final String T_ITEM = "petstore.item";
  public static final String ITEM_ID = "itemid";
  public static final String ITEM_PRODUCT_ID = "productid";
  public static final String ITEM_PRODUCT_REF = "product_ref";
  public static final String ITEM_NAME = "name";
  public static final String ITEM_DESCRIPTION = "description";
  public static final String ITEM_IMAGE_URL = "imageurl";
  public static final String ITEM_IMAGE_THUMB_URL = "imagethumburl";
  public static final String ITEM_PRICE = "price";
  public static final String ITEM_ADDRESS_ID = "address_addressid";
  public static final String ITEM_ADDRESS_REF = "address_ref";
  public static final String ITEM_C0NTACT_INFO_ID = "contactinfo_contactinfoid";
  public static final String ITEM_C0NTACT_INFO_REF = "contactinfo_ref";

  public static final String T_PRODUCT = "petstore.product";
  public static final String PRODUCT_ID = "productid";
  public static final String PRODUCT_CATEGORY_ID = "categoryid";
  public static final String PRODUCT_CATEGORY_REF = "category_ref";
  public static final String PRODUCT_NAME = "name";
  public static final String PRODUCT_DESCRIPTION = "description";
  public static final String PRODUCT_IMAGE_URL = "imageurl";

  public static final String T_SELLER_CONTACT_INFO = "petstore.sellercontactinfo";
  public static final String SELLER_CONTACT_INFO_ID = "contactinfoid";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "firstname";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "lastname";
  public static final String SELLER_CONTACT_INFO_EMAIL = "email";

  public static final String T_TAG = "petstore.tag tag";//alias used in a subquery property, see below
  public static final String TAG_ID = "tagid";
  public static final String TAG_TAG = "tag";
  public static final String TAG_REFCOUNT = "refcount";

  public static final String T_TAG_ITEM = "petstore.tag_item";
  public static final String TAG_ITEM_TAG_ID = "tagid";
  public static final String TAG_ITEM_TAG_REF = "tag_ref";
  public static final String TAG_ITEM_ITEM_ID = "itemid";
  public static final String TAG_ITEM_ITEM_REF = "item_ref";

  public static final String T_ZIP_LOCATION = "petstore.ziplocation";
  public static final String ZIP_LOCATION_ZIP_CODE = "zipcode";
  public static final String ZIP_LOCATION_CITY = "city";
  public static final String ZIP_LOCATION_STATE = "state";

  static {
    Entity.repository.initialize(T_ADDRESS, IdSource.ID_MAX_PLUS_ONE,
            ADDRESS_CITY + ", " + ADDRESS_STREET_1 + ", " + ADDRESS_STREET_2,
            new Property.PrimaryKeyProperty(ADDRESS_ID),
            new Property(ADDRESS_STREET_1, Type.STRING, "Street 1"),
            new Property(ADDRESS_STREET_2, Type.STRING, "Street 2"),
            new Property(ADDRESS_CITY, Type.STRING, "City"),
            new Property(ADDRESS_STATE, Type.STRING, "State"),
            new Property(ADDRESS_ZIP, Type.STRING, "Zip"),
            new Property(ADDRESS_LATITUDE, Type.DOUBLE, "Latitude"),
            new Property(ADDRESS_LONGITUDE, Type.DOUBLE, "Longitude"));

    Entity.repository.initialize(T_CATEGORY, IdSource.ID_NONE, CATEGORY_NAME,
            new Property.PrimaryKeyProperty(CATEGORY_ID, Type.STRING, "Id"),
            new Property(CATEGORY_NAME, Type.STRING, "Name"),
            new Property(CATEGORY_DESCRIPTION, Type.STRING, "Description"),
            new Property(CATEGORY_IMAGE_URL, Type.STRING, "Image URL", true));

    Entity.repository.initialize(T_ITEM, IdSource.ID_MAX_PLUS_ONE, ITEM_NAME,
            new Property.PrimaryKeyProperty(ITEM_ID),
            new Property.EntityProperty(ITEM_PRODUCT_REF, "Product", T_PRODUCT,
                    new Property(ITEM_PRODUCT_ID, Type.STRING)),
            new Property(ITEM_NAME, Type.STRING, "Name"),
            new Property(ITEM_DESCRIPTION, Type.STRING, "Description"),
            new Property(ITEM_IMAGE_URL, Type.STRING, "Image URL", true),
            new Property(ITEM_IMAGE_THUMB_URL, Type.STRING, "Image thumbnail URL", true),
            new Property(ITEM_PRICE, Type.DOUBLE, "Price"),
            new Property.EntityProperty(ITEM_C0NTACT_INFO_REF, "Contact info", T_SELLER_CONTACT_INFO,
                    new Property(ITEM_C0NTACT_INFO_ID)),
            new Property.EntityProperty(ITEM_ADDRESS_REF, "Address", T_ADDRESS,
                    new Property(ITEM_ADDRESS_ID)));

    Entity.repository.initialize(T_PRODUCT, IdSource.ID_NONE, PRODUCT_NAME,
            new Property.PrimaryKeyProperty(PRODUCT_ID, Type.STRING),
            new Property.EntityProperty(PRODUCT_CATEGORY_REF, "Category", T_CATEGORY,
                    new Property(PRODUCT_CATEGORY_ID, Type.STRING)),
            new Property(PRODUCT_NAME, Type.STRING, "Name"),
            new Property(PRODUCT_DESCRIPTION, Type.STRING, "Description"),
            new Property(PRODUCT_IMAGE_URL, Type.STRING, "Image URL", true));

    Entity.repository.initialize(T_SELLER_CONTACT_INFO, IdSource.ID_MAX_PLUS_ONE,
            SELLER_CONTACT_INFO_LAST_NAME + ", "+ SELLER_CONTACT_INFO_FIRST_NAME,
            new Property.PrimaryKeyProperty(SELLER_CONTACT_INFO_ID),
            new Property(SELLER_CONTACT_INFO_FIRST_NAME, Type.STRING, "First name"),
            new Property(SELLER_CONTACT_INFO_LAST_NAME, Type.STRING, "Last name"),
            new Property(SELLER_CONTACT_INFO_EMAIL, Type.STRING, "Email"));

    Entity.repository.initialize(T_TAG, IdSource.ID_MAX_PLUS_ONE, TAG_TAG,
            new Property.PrimaryKeyProperty(TAG_ID),
            new Property(TAG_TAG, Type.STRING, "Tag"),
            new Property.SubQueryProperty(TAG_REFCOUNT, Type.INT, false, "Reference count",
                    "select count(*) from " + T_TAG_ITEM + "  where " + TAG_ITEM_TAG_ID + " = tag." + TAG_ID));

    Entity.repository.initialize(T_TAG_ITEM, IdSource.ID_NONE,
            new Property.EntityProperty(TAG_ITEM_ITEM_REF, "Item", T_ITEM,
                    new Property.PrimaryKeyProperty(TAG_ITEM_ITEM_ID, Type.INT, null, 0)),
            new Property.EntityProperty(TAG_ITEM_TAG_REF, "Tag", T_TAG,
                    new Property.PrimaryKeyProperty(TAG_ITEM_TAG_ID, Type.INT, null, 1)));

    Entity.repository.initialize(T_ZIP_LOCATION, IdSource.ID_NONE, ZIP_LOCATION_ZIP_CODE,
            new Property.PrimaryKeyProperty(ZIP_LOCATION_ZIP_CODE, Type.STRING, "Zip code"),
            new Property(ZIP_LOCATION_CITY, Type.STRING, "City"),
            new Property(ZIP_LOCATION_STATE, Type.STRING, "State"));

    Entity.repository.setDefaultEntityProxy(new EntityProxy() {
      public String toString(final Entity entity) {
        if (entity.getEntityID().equals(T_ADDRESS))
          return entity.getStringValue(ADDRESS_STREET_1) + " " + entity.getStringValue(ADDRESS_STREET_2)
                  + ", " + entity.getStringValue(ADDRESS_CITY) + " " + entity.getStringValue(ADDRESS_ZIP) + ", "
                  + entity.getStringValue(ADDRESS_STATE);
        else if (entity.getEntityID().equals(T_CATEGORY))
          return entity.getStringValue(CATEGORY_NAME);
        else if (entity.getEntityID().equals(T_ITEM))
          return entity.getValueAsString(ITEM_PRODUCT_REF) + " - " + entity.getStringValue(ITEM_NAME);
        else if (entity.getEntityID().equals(T_PRODUCT))
          return entity.getValueAsString(PRODUCT_CATEGORY_REF) + " - " + entity.getStringValue(PRODUCT_NAME);
        else if (entity.getEntityID().equals(T_SELLER_CONTACT_INFO))
          return entity.getStringValue(SELLER_CONTACT_INFO_LAST_NAME) + ", " + entity.getStringValue(SELLER_CONTACT_INFO_FIRST_NAME);
        else if (entity.getEntityID().equals(T_TAG))
          return entity.getStringValue(TAG_TAG);
        else if (entity.getEntityID().equals(T_TAG_ITEM))
          return entity.getEntityValue(TAG_ITEM_ITEM_REF) + " - " + entity.getEntityValue(TAG_ITEM_TAG_REF);

        return super.toString(entity);
      }
    });
  }
}
