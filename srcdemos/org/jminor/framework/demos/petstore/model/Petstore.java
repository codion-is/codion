/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.model;

import org.jminor.common.db.IdSource;
import org.jminor.framework.model.Entity;
import org.jminor.framework.model.EntityProxy;
import org.jminor.framework.model.EntityRepository;
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
  public static final String ADDRESS_ZIP_FK = "zip_fk";
  public static final String ADDRESS_LATITUDE = "latitude";
  public static final String ADDRESS_LONGITUDE = "longitude";

  public static final String T_CATEGORY = "petstore.category";
  public static final String CATEGORY_ID = "categoryid";
  public static final String CATEGORY_NAME = "name";
  public static final String CATEGORY_DESCRIPTION = "description";
  public static final String CATEGORY_IMAGE_URL = "imageurl";

  public static final String T_ITEM = "petstore.item";
  public static final String ITEM_ID = "itemid";
  public static final String ITEM_PRODUCT_ID = "productid";
  public static final String ITEM_PRODUCT_FK = "product_fk";
  public static final String ITEM_NAME = "name";
  public static final String ITEM_DESCRIPTION = "description";
  public static final String ITEM_IMAGE_URL = "imageurl";
  public static final String ITEM_IMAGE_THUMB_URL = "imagethumburl";
  public static final String ITEM_PRICE = "price";
  public static final String ITEM_ADDRESS_ID = "address_addressid";
  public static final String ITEM_ADDRESS_FK = "address_fk";
  public static final String ITEM_C0NTACT_INFO_ID = "contactinfo_contactinfoid";
  public static final String ITEM_C0NTACT_INFO_FK = "contactinfo_fk";
  public static final String ITEM_DISABLED = "disabled";

  public static final String T_PRODUCT = "petstore.product";
  public static final String PRODUCT_ID = "productid";
  public static final String PRODUCT_CATEGORY_ID = "categoryid";
  public static final String PRODUCT_CATEGORY_FK = "category_fk";
  public static final String PRODUCT_NAME = "name";
  public static final String PRODUCT_DESCRIPTION = "description";
  public static final String PRODUCT_IMAGE_URL = "imageurl";

  public static final String T_SELLER_CONTACT_INFO = "petstore.sellercontactinfo";
  public static final String SELLER_CONTACT_INFO_ID = "contactinfoid";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "firstname";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "lastname";
  public static final String SELLER_CONTACT_INFO_EMAIL = "email";

  public static final String T_TAG = "petstore.tag";//alias used in a subquery property, see below
  public static final String TAG_ID = "tagid";
  public static final String TAG_TAG = "tag";
  public static final String TAG_REFCOUNT = "refcount";

  public static final String T_TAG_ITEM = "petstore.tag_item";
  public static final String TAG_ITEM_TAG_ID = "tagid";
  public static final String TAG_ITEM_TAG_FK = "tag_fk";
  public static final String TAG_ITEM_ITEM_ID = "itemid";
  public static final String TAG_ITEM_ITEM_FK = "item_fk";

  static {
    EntityRepository.get().initialize(T_ADDRESS, IdSource.MAX_PLUS_ONE,
            ADDRESS_CITY + ", " + ADDRESS_STREET_1 + ", " + ADDRESS_STREET_2,
            new Property.PrimaryKeyProperty(ADDRESS_ID),
            new Property(ADDRESS_STREET_1, Type.STRING, "Street 1"),
            new Property(ADDRESS_STREET_2, Type.STRING, "Street 2"),
            new Property(ADDRESS_CITY, Type.STRING, "City"),
            new Property(ADDRESS_STATE, Type.STRING, "State"),
            new Property(ADDRESS_ZIP, Type.INT, "Zip"),
            new Property(ADDRESS_LATITUDE, Type.DOUBLE, "Latitude"),
            new Property(ADDRESS_LONGITUDE, Type.DOUBLE, "Longitude"));

    EntityRepository.get().initialize(T_CATEGORY, IdSource.MAX_PLUS_ONE, CATEGORY_NAME,
            new Property.PrimaryKeyProperty(CATEGORY_ID),
            new Property(CATEGORY_NAME, Type.STRING, "Name"),
            new Property(CATEGORY_DESCRIPTION, Type.STRING, "Description"),
            new Property(CATEGORY_IMAGE_URL, Type.STRING, "Image URL", true));

    EntityRepository.get().initialize(T_ITEM, IdSource.MAX_PLUS_ONE, ITEM_NAME,
            new Property.PrimaryKeyProperty(ITEM_ID),
            new Property.ForeignKeyProperty(ITEM_PRODUCT_FK, "Product", T_PRODUCT,
                    new Property(ITEM_PRODUCT_ID)),
            new Property(ITEM_NAME, Type.STRING, "Name"),
            new Property(ITEM_DESCRIPTION, Type.STRING, "Description"),
            new Property(ITEM_IMAGE_URL, Type.STRING, "Image URL", true),
            new Property(ITEM_IMAGE_THUMB_URL, Type.STRING, "Image thumbnail URL", true),
            new Property(ITEM_PRICE, Type.DOUBLE, "Price"),
            new Property.ForeignKeyProperty(ITEM_C0NTACT_INFO_FK, "Contact info", T_SELLER_CONTACT_INFO,
                    new Property(ITEM_C0NTACT_INFO_ID)),
            new Property.ForeignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    new Property(ITEM_ADDRESS_ID)),
            new Property(ITEM_DISABLED, Type.BOOLEAN, "Disabled"));

    EntityRepository.get().initialize(T_PRODUCT, IdSource.MAX_PLUS_ONE, PRODUCT_NAME,
            new Property.PrimaryKeyProperty(PRODUCT_ID),
            new Property.ForeignKeyProperty(PRODUCT_CATEGORY_FK, "Category", T_CATEGORY,
                    new Property(PRODUCT_CATEGORY_ID)),
            new Property(PRODUCT_NAME, Type.STRING, "Name"),
            new Property(PRODUCT_DESCRIPTION, Type.STRING, "Description"),
            new Property(PRODUCT_IMAGE_URL, Type.STRING, "Image URL", true));

    EntityRepository.get().initialize(T_SELLER_CONTACT_INFO, IdSource.MAX_PLUS_ONE,
            SELLER_CONTACT_INFO_LAST_NAME + ", "+ SELLER_CONTACT_INFO_FIRST_NAME,
            new Property.PrimaryKeyProperty(SELLER_CONTACT_INFO_ID),
            new Property(SELLER_CONTACT_INFO_FIRST_NAME, Type.STRING, "First name"),
            new Property(SELLER_CONTACT_INFO_LAST_NAME, Type.STRING, "Last name"),
            new Property(SELLER_CONTACT_INFO_EMAIL, Type.STRING, "Email"));

    EntityRepository.get().initialize(T_TAG, IdSource.MAX_PLUS_ONE, TAG_TAG, null, "petstore.tag tag",
            new Property.PrimaryKeyProperty(TAG_ID),
            new Property(TAG_TAG, Type.STRING, "Tag"),
            new Property.SubqueryProperty(TAG_REFCOUNT, Type.INT, false, "Reference count",
                    "select count(*) from " + T_TAG_ITEM + "  where " + TAG_ITEM_TAG_ID + " = tag." + TAG_ID));

    EntityRepository.get().initialize(T_TAG_ITEM, IdSource.NONE,
            new Property.ForeignKeyProperty(TAG_ITEM_ITEM_FK, "Item", T_ITEM,
                    new Property.PrimaryKeyProperty(TAG_ITEM_ITEM_ID, Type.INT, null, 0)),
            new Property.ForeignKeyProperty(TAG_ITEM_TAG_FK, "Tag", T_TAG,
                    new Property.PrimaryKeyProperty(TAG_ITEM_TAG_ID, Type.INT, null, 1)));

    EntityProxy.setDefaultEntityProxy(new EntityProxy() {
      @Override
      public String toString(final Entity entity) {
        if (entity.is(T_ADDRESS))
          return entity.getStringValue(ADDRESS_STREET_1) + " " + entity.getStringValue(ADDRESS_STREET_2)
                  + ", " + entity.getStringValue(ADDRESS_CITY) + " " + entity.getValueAsString(ADDRESS_ZIP) + ", "
                  + entity.getStringValue(ADDRESS_STATE);
        else if (entity.is(T_CATEGORY))
          return entity.getStringValue(CATEGORY_NAME);
        else if (entity.is(T_ITEM))
          return entity.getValueAsString(ITEM_PRODUCT_FK) + " - " + entity.getStringValue(ITEM_NAME);
        else if (entity.is(T_PRODUCT))
          return entity.getValueAsString(PRODUCT_CATEGORY_FK) + " - " + entity.getStringValue(PRODUCT_NAME);
        else if (entity.is(T_SELLER_CONTACT_INFO))
          return entity.getStringValue(SELLER_CONTACT_INFO_LAST_NAME) + ", " + entity.getStringValue(SELLER_CONTACT_INFO_FIRST_NAME);
        else if (entity.is(T_TAG))
          return entity.getStringValue(TAG_TAG);
        else if (entity.is(T_TAG_ITEM))
          return entity.getEntityValue(TAG_ITEM_ITEM_FK) + " - " + entity.getEntityValue(TAG_ITEM_TAG_FK);

        return super.toString(entity);
      }
    });
  }
}
