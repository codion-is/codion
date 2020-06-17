/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.server;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface EmployeeService extends Remote {

  List<Entity> getEmployees() throws RemoteException, DatabaseException;

  List<Employee> getEmployeeBeans() throws RemoteException, DatabaseException;
}
