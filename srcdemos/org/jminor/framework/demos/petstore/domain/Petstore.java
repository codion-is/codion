/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.common.model.IdSource;
import org.jminor.common.model.Version;
import org.jminor.common.model.valuemap.StringProvider;
import org.jminor.framework.domain.EntityDefinition;
import org.jminor.framework.domain.EntityRepository;
import org.jminor.framework.domain.Property;

import java.sql.Types;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 13:09:59
 */
public class Petstore {

  public static final Version version = new Version(Petstore.class.getPackage().getName(), "1");

  public static final String T_ADDRESS = "address" + version;
  public static final String ADDRESS_ID = "addressid";
  public static final String ADDRESS_STREET_1 = "street1";
  public static final String ADDRESS_STREET_2 = "street2";
  public static final String ADDRESS_CITY = "city";
  public static final String ADDRESS_STATE = "state";
  public static final String ADDRESS_ZIP = "zip";
  public static final String ADDRESS_ZIP_FK = "zip_fk";
  public static final String ADDRESS_LATITUDE = "latitude";
  public static final String ADDRESS_LONGITUDE = "longitude";

  public static final String T_CATEGORY = "category" + version;
  public static final String CATEGORY_ID = "categoryid";
  public static final String CATEGORY_NAME = "name";
  public static final String CATEGORY_DESCRIPTION = "description";
  public static final String CATEGORY_IMAGE_URL = "imageurl";

  public static final String T_ITEM = "item" + version;
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

  public static final String T_PRODUCT = "product" + version;
  public static final String PRODUCT_ID = "productid";
  public static final String PRODUCT_CATEGORY_ID = "categoryid";
  public static final String PRODUCT_CATEGORY_FK = "category_fk";
  public static final String PRODUCT_NAME = "name";
  public static final String PRODUCT_DESCRIPTION = "description";
  public static final String PRODUCT_IMAGE_URL = "imageurl";

  public static final String T_SELLER_CONTACT_INFO = "sellercontactinfo" + version;
  public static final String SELLER_CONTACT_INFO_ID = "contactinfoid";
  public static final String SELLER_CONTACT_INFO_FIRST_NAME = "firstname";
  public static final String SELLER_CONTACT_INFO_LAST_NAME = "lastname";
  public static final String SELLER_CONTACT_INFO_EMAIL = "email";

  public static final String T_TAG = "tag" + version;
  public static final String TAG_ID = "tagid";
  public static final String TAG_TAG = "tag";
  public static final String TAG_REFCOUNT = "refcount";

  public static final String T_TAG_ITEM = "tag_item" + version;
  public static final String TAG_ITEM_TAG_ID = "tagid";
  public static final String TAG_ITEM_TAG_FK = "tag_fk";
  public static final String TAG_ITEM_ITEM_ID = "itemid";
  public static final String TAG_ITEM_ITEM_FK = "item_fk";

  static {
    EntityRepository.add(new EntityDefinition(T_ADDRESS, "petstore.address",
            new Property.PrimaryKeyProperty(ADDRESS_ID),
            new Property(ADDRESS_STREET_1, Types.VARCHAR, "Street 1").setMaxLength(55).setNullable(false),
            new Property(ADDRESS_STREET_2, Types.VARCHAR, "Street 2").setMaxLength(55),
            new Property(ADDRESS_CITY, Types.VARCHAR, "City").setMaxLength(55).setNullable(false),
            new Property(ADDRESS_STATE, Types.VARCHAR, "State").setMaxLength(25).setNullable(false),
            new Property(ADDRESS_ZIP, Types.INTEGER, "Zip").setNullable(false),
            new Property(ADDRESS_LATITUDE, Types.DOUBLE, "Latitude").setNullable(false).setMaximumFractionDigits(2),
            new Property(ADDRESS_LONGITUDE, Types.DOUBLE, "Longitude").setNullable(false).setMaximumFractionDigits(2))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(ADDRESS_CITY + ", " + ADDRESS_STREET_1 + ", " + ADDRESS_STREET_2)
            .setStringProvider(new StringProvider<String, Object>(ADDRESS_STREET_1).addText(" ")
            .addValue(ADDRESS_STREET_2).addText(", ").addValue(ADDRESS_CITY).addText(" ")
            .addValue(ADDRESS_ZIP).addText(", ").addValue(ADDRESS_STATE)));

    EntityRepository.add(new EntityDefinition(T_CATEGORY, "petstore.category",
            new Property.PrimaryKeyProperty(CATEGORY_ID),
            new Property(CATEGORY_NAME, Types.VARCHAR, "Name").setMaxLength(25).setNullable(false),
            new Property(CATEGORY_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(255).setNullable(false),
            new Property(CATEGORY_IMAGE_URL, Types.VARCHAR, "Image URL").setHidden(true))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(CATEGORY_NAME)
            .setStringProvider(new StringProvider<String, Object>(CATEGORY_NAME)));

    EntityRepository.add(new EntityDefinition(T_ITEM, "petstore.item",
            new Property.PrimaryKeyProperty(ITEM_ID),
            new Property.ForeignKeyProperty(ITEM_PRODUCT_FK, "Product", T_PRODUCT,
                    new Property(ITEM_PRODUCT_ID)).setFetchDepth(2).setNullable(false),
            new Property(ITEM_NAME, Types.VARCHAR, "Name").setMaxLength(30).setNullable(false),
            new Property(ITEM_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(500).setNullable(false),
            new Property(ITEM_IMAGE_URL, Types.VARCHAR, "Image URL").setHidden(true).setMaxLength(55),
            new Property(ITEM_IMAGE_THUMB_URL, Types.VARCHAR, "Image thumbnail URL").setMaxLength(55).setHidden(true),
            new Property(ITEM_PRICE, Types.DOUBLE, "Price").setNullable(false).setMaximumFractionDigits(2),
            new Property.ForeignKeyProperty(ITEM_C0NTACT_INFO_FK, "Contact info", T_SELLER_CONTACT_INFO,
                    new Property(ITEM_C0NTACT_INFO_ID)).setNullable(false),
            new Property.ForeignKeyProperty(ITEM_ADDRESS_FK, "Address", T_ADDRESS,
                    new Property(ITEM_ADDRESS_ID)).setNullable(false),
            new Property(ITEM_DISABLED, Types.BOOLEAN, "Disabled").setDefaultValue(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(ITEM_NAME)
            .setStringProvider(new StringProvider<String, Object>(ITEM_PRODUCT_FK).addText(" - ").addValue(ITEM_NAME)));

    EntityRepository.add(new EntityDefinition(T_PRODUCT, "petstore.product",
            new Property.PrimaryKeyProperty(PRODUCT_ID),
            new Property.ForeignKeyProperty(PRODUCT_CATEGORY_FK, "Category", T_CATEGORY,
                    new Property(PRODUCT_CATEGORY_ID)).setNullable(false),
            new Property(PRODUCT_NAME, Types.VARCHAR, "Name").setMaxLength(25).setNullable(false),
            new Property(PRODUCT_DESCRIPTION, Types.VARCHAR, "Description").setMaxLength(255).setNullable(false),
            new Property(PRODUCT_IMAGE_URL, Types.VARCHAR, "Image URL").setMaxLength(55).setHidden(true))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(PRODUCT_NAME)
            .setStringProvider(new StringProvider<String, Object>(PRODUCT_CATEGORY_FK)
            .addText(" - ").addValue(PRODUCT_NAME)));

    EntityRepository.add(new EntityDefinition(T_SELLER_CONTACT_INFO, "petstore.sellercontactinfo",
            new Property.PrimaryKeyProperty(SELLER_CONTACT_INFO_ID),
            new Property(SELLER_CONTACT_INFO_FIRST_NAME, Types.VARCHAR, "First name").setMaxLength(24).setNullable(false),
            new Property(SELLER_CONTACT_INFO_LAST_NAME, Types.VARCHAR, "Last name").setMaxLength(24).setNullable(false),
            new Property(SELLER_CONTACT_INFO_EMAIL, Types.VARCHAR, "Email").setMaxLength(24).setNullable(false))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(SELLER_CONTACT_INFO_LAST_NAME + ", "+ SELLER_CONTACT_INFO_FIRST_NAME)
            .setStringProvider(new StringProvider<String, Object>(SELLER_CONTACT_INFO_LAST_NAME)
            .addText(", ").addValue(SELLER_CONTACT_INFO_FIRST_NAME)));

    EntityRepository.add(new EntityDefinition(T_TAG, "petstore.tag",
            new Property.PrimaryKeyProperty(TAG_ID),
            new Property(TAG_TAG, Types.VARCHAR, "Tag").setMaxLength(30).setNullable(false),
            new Property.SubqueryProperty(TAG_REFCOUNT, Types.INTEGER, "Reference count",
                    "select count(*) from petstore.tag_item  where " + TAG_ITEM_TAG_ID + " = tag." + TAG_ID))
            .setIdSource(IdSource.MAX_PLUS_ONE)
            .setOrderByClause(TAG_TAG)
            .setSelectTableName("petstore.tag tag")
            .setStringProvider(new StringProvider<String, Object>(TAG_TAG)));

    EntityRepository.add(new EntityDefinition(T_TAG_ITEM, "petstore.tag_item",
            new Property.ForeignKeyProperty(TAG_ITEM_ITEM_FK, "Item", T_ITEM,
                    new Property.PrimaryKeyProperty(TAG_ITEM_ITEM_ID, Types.INTEGER).setIndex(0)).setNullable(false),
            new Property.ForeignKeyProperty(TAG_ITEM_TAG_FK, "Tag", T_TAG,
                    new Property.PrimaryKeyProperty(TAG_ITEM_TAG_ID, Types.INTEGER).setIndex(1)).setNullable(false))
            .setStringProvider(new StringProvider<String, Object>(TAG_ITEM_ITEM_FK).addText(" - ").addValue(TAG_ITEM_TAG_FK)));
  }
}
