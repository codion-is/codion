/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.framework.db.EntityDb;

import java.rmi.Remote;

/**
 * An interface specifying a remote EntityDb implementation.
 */
public interface EntityDbRemote extends EntityDb, Remote {}
