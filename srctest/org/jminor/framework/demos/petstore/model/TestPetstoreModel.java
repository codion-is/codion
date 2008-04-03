/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.model;

import junit.framework.TestCase;
import org.jminor.common.db.User;
import org.jminor.common.model.UserException;
import org.jminor.common.model.Util;
import org.jminor.framework.FrameworkSettings;
import org.jminor.framework.db.EntityDbProviderFactory;
import org.jminor.framework.db.IEntityDb;

/**
 * User: Bj�rn Darri
 * Date: 24.12.2007
 * Time: 13:20:26
 */
public class TestPetstoreModel extends TestCase {

  private static IEntityDb db;

  static {
    FrameworkSettings.get().setProperty(FrameworkSettings.USE_SMART_REFRESH, false);
    FrameworkSettings.get().setProperty(FrameworkSettings.USE_QUERY_RANGE, false);
    try {
      new Petstore();
      db = EntityDbProviderFactory.createEntityDbProvider(new User("scott", "tiger"), "TestPetstoreModel").getEntityDb();
    }
    catch (UserException e) {
      e.printStackTrace();
    }
  }

  public TestPetstoreModel(String name) {
    super(name);
  }

  public void testAddress() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ADDRESS));
  }

  public void testCategory() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_CATEGORY));
  }

  public void testItem() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ITEM));
  }

  public void testProduct() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_PRODUCT));
  }

  public void testSellerInfo() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_SELLER_CONTACT_INFO));
  }

  public void testTag() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_TAG));
  }

  public void testTagItem() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_TAG_ITEM));
  }

  public void testZipLocation() throws Exception {
    Util.printListContents(db.selectAll(Petstore.T_ZIP_LOCATION));
  }
}
