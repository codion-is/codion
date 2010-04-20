/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.client;

import org.jminor.common.model.User;
import org.jminor.framework.client.model.EntityApplicationModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.beans.ArtistModel;
import org.jminor.framework.demos.chinook.beans.CustomerModel;
import org.jminor.framework.demos.chinook.beans.PlaylistModel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:15:12
 */
public class ChinookAppModel extends EntityApplicationModel {

  public ChinookAppModel(final User user) {
    super(user, ChinookAppModel.class.getSimpleName());
  }

  @Override
  protected List<? extends EntityModel> initializeMainApplicationModels(final EntityDbProvider dbProvider) {
    return Arrays.asList(new ArtistModel(getDbProvider()), new PlaylistModel(getDbProvider()), 
            new CustomerModel(getDbProvider()));
  }

  @Override
  protected void loadDomainModel() {
    new Chinook();
  }
}
