/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.UserException;
import org.jminor.common.db.User;
import org.jminor.framework.db.IEntityDb;
import org.jminor.framework.db.EntityDbProviderFactory;

import junit.framework.TestCase;

/**
 * User: Bj�rn Darri
 * Date: 24.12.2007
 * Time: 13:20:26
 */
public class TestPetstoreModel extends TestCase {

  private IEntityDb db;

  public TestPetstoreModel(String name) {
    super(name);
    new Petstore();
  }

  public void testAddress() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_ADDRESS));
  }

  public void testCategory() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_CATEGORY));
  }

  public void testItem() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_ITEM));
  }

  public void testProduct() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_PRODUCT));
  }

  public void testSellerInfo() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_SELLER_CONTACT_INFO));
  }

  public void testTag() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_TAG));
  }

  public void testTagItem() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_TAG_ITEM));
  }

  public void testZipLocation() throws Exception {
    if (petstoreSchemaAvailable())
      Util.printListContents(db.selectAll(Petstore.T_ZIP_LOCATION));
  }

  protected void setUp() throws Exception {
    db = initEntityDb();
  }

  protected void tearDown() throws Exception {
    if (db != null)
      db.logout();
  }

  private IEntityDb initEntityDb() throws UserException {
    return EntityDbProviderFactory.createEntityDbProvider(new User("scott", "tiger"), "TestPetstoreModel").getEntityDb();
  }

  private boolean petstoreSchemaAvailable() {
    try {
      db.selectAll(Petstore.T_CATEGORY);
      return true;
    }
    catch (Exception e) {
      System.out.println(e);
      return false;
    }
  }
}
