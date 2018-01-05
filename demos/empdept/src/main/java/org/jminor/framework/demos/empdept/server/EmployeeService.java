/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.server;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.domain.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface EmployeeService extends Remote {

  List<Entity> getEmployees() throws RemoteException, DatabaseException;

  void disconnect() throws RemoteException;
}
