/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.empdept.server;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.framework.demos.empdept.domain.Employee;
import dev.codion.framework.domain.entity.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface EmployeeService extends Remote {

  List<Entity> getEmployees() throws RemoteException, DatabaseException;

  List<Employee> getEmployeeBeans() throws RemoteException, DatabaseException;
}
