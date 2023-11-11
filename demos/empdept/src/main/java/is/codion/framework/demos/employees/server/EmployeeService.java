/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.server;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.domain.entity.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

public interface EmployeeService extends Remote {

  Collection<Entity> employees() throws RemoteException, DatabaseException;

  Collection<Employees.Employee> employeeBeans() throws RemoteException, DatabaseException;
}
