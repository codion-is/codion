/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.demos.chinook.beans.ArtistModel;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import org.jminor.framework.demos.chinook.beans.PlaylistModel;
import org.jminor.framework.demos.chinook.domain.Chinook;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:15:12
 */
public class ChinookAppModel extends EntityApplicationModel {

  public ChinookAppModel(final User user) {
    super(user, ChinookAppModel.class.getSimpleName());
    addMainApplicationModels(new ArtistModel(getDbProvider()), new PlaylistModel(getDbProvider()),
            new CustomerModel(getDbProvider()));
  }

  @Override
  protected void loadDomainModel() {
    new Chinook();
  }
}
