/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.server;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.domain.entity.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Collection;

import static is.codion.framework.demos.employees.domain.Employees.Employee;

public interface EmployeeService extends Remote {

  Collection<Entity> employees() throws RemoteException, DatabaseException;

  Collection<Employee> employeeBeans() throws RemoteException, DatabaseException;
}
