/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.domain;

import org.jminor.common.db.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.tools.testing.EntityTestUnit;

import org.junit.Test;

import java.util.Collection;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 13:20:26
 */
public class PetstoreTest extends EntityTestUnit {

  @Test
  public void address() throws Exception {
    testEntity(Petstore.T_ADDRESS);
  }

  @Test
  public void category() throws Exception {
    testEntity(Petstore.T_CATEGORY);
  }

  @Test
  public void item() throws Exception {
    testEntity(Petstore.T_ITEM);
  }

  @Test
  public void product() throws Exception {
    testEntity(Petstore.T_PRODUCT);
  }

  @Test
  public void sellerInfo() throws Exception {
    testEntity(Petstore.T_SELLER_CONTACT_INFO);
  }

  @Test
  public void tag() throws Exception {
    testEntity(Petstore.T_TAG);
  }

  @Test
  public void tagItem() throws Exception {
    testEntity(Petstore.T_TAG_ITEM);
  }

  @Override
  protected void loadDomainModel() {
    new Petstore();
  }

  @Override
  protected User getTestUser() throws CancelException {
    return new User("scott", "tiger");
  }

  @Override
  protected Entity initializeTestEntity(final String entityID) {
    if (entityID.equals(Petstore.T_ADDRESS)) {
      final Entity address = createEntity(Petstore.T_ADDRESS);
      address.setValue(Petstore.ADDRESS_CITY, "Syracuse");
      address.setValue(Petstore.ADDRESS_STATE, "NY");
      address.setValue(Petstore.ADDRESS_STREET_1, "Street");
      address.setValue(Petstore.ADDRESS_ZIP, 36404);
      address.setValue(Petstore.ADDRESS_LATITUDE, 36.2345);
      address.setValue(Petstore.ADDRESS_LONGITUDE, 36.2345);

      return address;
    }
    if (entityID.equals(Petstore.T_CATEGORY)) {
      final Entity category = createEntity(Petstore.T_CATEGORY);
      category.setValue(Petstore.CATEGORY_DESCRIPTION, "descr");
      category.setValue(Petstore.CATEGORY_IMAGE_URL, "imageurl");
      category.setValue(Petstore.CATEGORY_NAME, "category");

      return category;
    }
    if (entityID.equals(Petstore.T_ITEM)) {
      final Entity item = createEntity(Petstore.T_ITEM);
      item.setValue(Petstore.ITEM_DESCRIPTION, "description");
      item.setValue(Petstore.ITEM_NAME, "item");
      item.setValue(Petstore.ITEM_PRICE, 34.2);
      item.setValue(Petstore.ITEM_DISABLED, false);

      return item;
    }
    if (entityID.equals(Petstore.T_PRODUCT)) {
      final Entity product = createEntity(Petstore.T_PRODUCT);
      product.setValue(Petstore.PRODUCT_DESCRIPTION, "description");
      product.setValue(Petstore.PRODUCT_IMAGE_URL, "imageurl");
      product.setValue(Petstore.PRODUCT_NAME, "product name");

      return product;
    }
    if (entityID.equals(Petstore.T_SELLER_CONTACT_INFO)) {
      final Entity info = createEntity(Petstore.T_SELLER_CONTACT_INFO);
      info.setValue(Petstore.SELLER_CONTACT_INFO_EMAIL, "email@email.com");
      info.setValue(Petstore.SELLER_CONTACT_INFO_FIRST_NAME, "Björn");
      info.setValue(Petstore.SELLER_CONTACT_INFO_LAST_NAME, "Sigurðsson");

      return info;
    }
    if (entityID.equals(Petstore.T_TAG)) {
      final Entity tag = createEntity(Petstore.T_TAG);
      tag.setValue(Petstore.TAG_TAG, "tag");

      return tag;
    }
    if (entityID.equals(Petstore.T_TAG_ITEM)) {
      return createEntity(Petstore.T_TAG_ITEM);
    }

    return null;
  }

  @Override
  protected void modifyEntity(final Entity testEntity) {
    if (testEntity.is(Petstore.T_ADDRESS)) {
      testEntity.setValue(Petstore.ADDRESS_CITY, "new city");
      testEntity.setValue(Petstore.ADDRESS_STATE, "NM");
    }
    else if (testEntity.is(Petstore.T_CATEGORY)) {
      testEntity.setValue(Petstore.CATEGORY_DESCRIPTION, "new Descr");
      testEntity.setValue(Petstore.CATEGORY_IMAGE_URL, "new url");
    }
    else if (testEntity.is(Petstore.T_ITEM)) {
      testEntity.setValue(Petstore.ITEM_DESCRIPTION, "new description");
      testEntity.setValue(Petstore.ITEM_IMAGE_URL, "new url");
    }
    else if (testEntity.is(Petstore.T_PRODUCT)) {
      testEntity.setValue(Petstore.PRODUCT_DESCRIPTION, "new description");
      testEntity.setValue(Petstore.PRODUCT_IMAGE_URL, "new url");
    }
    else if (testEntity.is(Petstore.T_SELLER_CONTACT_INFO)) {
      testEntity.setValue(Petstore.SELLER_CONTACT_INFO_EMAIL, "new@email.com");
      testEntity.setValue(Petstore.SELLER_CONTACT_INFO_LAST_NAME, "Lastname");
    }
    else if (testEntity.is(Petstore.T_TAG)) {
      testEntity.setValue(Petstore.TAG_TAG, "new tag");
    }
  }

  @Override
  protected void initializeReferenceEntities(final Collection<String> referenceEntityIDs) throws Exception {
    if (referenceEntityIDs.contains(Petstore.T_ADDRESS)) {
      final Entity address = createEntity(Petstore.T_ADDRESS);
      address.setValue(Petstore.ADDRESS_CITY, "A city");
      address.setValue(Petstore.ADDRESS_STATE, "NY");
      address.setValue(Petstore.ADDRESS_STREET_1, "Street");
      address.setValue(Petstore.ADDRESS_ZIP, 36404);
      address.setValue(Petstore.ADDRESS_LATITUDE, 36.2345);
      address.setValue(Petstore.ADDRESS_LONGITUDE, 36.2345);

      setReferenceEntity(Petstore.T_ADDRESS, address);
    }
    if (referenceEntityIDs.contains(Petstore.T_SELLER_CONTACT_INFO)) {
      final Entity info = createEntity(Petstore.T_SELLER_CONTACT_INFO);
      info.setValue(Petstore.SELLER_CONTACT_INFO_EMAIL, "email@email.com");
      info.setValue(Petstore.SELLER_CONTACT_INFO_FIRST_NAME, "John");
      info.setValue(Petstore.SELLER_CONTACT_INFO_LAST_NAME, "Doe");

      setReferenceEntity(Petstore.T_SELLER_CONTACT_INFO, info);
    }
    if (referenceEntityIDs.contains(Petstore.T_CATEGORY)) {
      final Entity category = createEntity(Petstore.T_CATEGORY);
      category.setValue(Petstore.CATEGORY_DESCRIPTION, "descr");
      category.setValue(Petstore.CATEGORY_IMAGE_URL, "imageurl");
      category.setValue(Petstore.CATEGORY_NAME, "refcategory");

      setReferenceEntity(Petstore.T_CATEGORY, category);
    }
    if (referenceEntityIDs.contains(Petstore.T_PRODUCT)) {
      final Entity product = createEntity(Petstore.T_PRODUCT);
      product.setValue(Petstore.PRODUCT_DESCRIPTION, "description");
      product.setValue(Petstore.PRODUCT_IMAGE_URL, "imageurl");
      product.setValue(Petstore.PRODUCT_NAME, "ref product name");

      setReferenceEntity(Petstore.T_PRODUCT, product);
    }
    if (referenceEntityIDs.contains(Petstore.T_PRODUCT)) {
      final Entity product = createEntity(Petstore.T_PRODUCT);
      product.setValue(Petstore.PRODUCT_DESCRIPTION, "description");
      product.setValue(Petstore.PRODUCT_IMAGE_URL, "imageurl");
      product.setValue(Petstore.PRODUCT_NAME, "ref product name");

      setReferenceEntity(Petstore.T_PRODUCT, product);
    }
    if (referenceEntityIDs.contains(Petstore.T_TAG)) {
      final Entity tag = createEntity(Petstore.T_TAG);
      tag.setValue(Petstore.TAG_TAG, "reftag");

      setReferenceEntity(Petstore.T_TAG, tag);
    }
    if (referenceEntityIDs.contains(Petstore.T_ITEM)) {
      final Entity item = createEntity(Petstore.T_ITEM);
      item.setValue(Petstore.ITEM_DESCRIPTION, "description");
      item.setValue(Petstore.ITEM_NAME, "refitem");
      item.setValue(Petstore.ITEM_PRICE, 34.2);
      item.setValue(Petstore.ITEM_DISABLED, true);

      setReferenceEntity(Petstore.T_ITEM, item);
    }
  }
}
