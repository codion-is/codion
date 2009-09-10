/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.framework.db.IEntityDb;

import java.rmi.Remote;

public interface IEntityDbRemote extends IEntityDb, Remote {

}
