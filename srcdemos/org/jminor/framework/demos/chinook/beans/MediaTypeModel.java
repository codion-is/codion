/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans;

import org.jminor.framework.client.model.DefaultEntityModel;
import org.jminor.framework.db.provider.EntityDbProvider;
import org.jminor.framework.demos.chinook.domain.Chinook;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 19:54:35
 */
public class MediaTypeModel extends DefaultEntityModel {

  public MediaTypeModel(final EntityDbProvider dbProvider) {
    super(Chinook.T_MEDIATYPE, dbProvider);
    addDetailModel(new TrackModel(getDbProvider()));
  }
}