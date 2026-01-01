/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.db.rmi;

import is.codion.framework.domain.entity.Entity;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * RMI-specific iterator interface for remote entity result iteration.
 * <p>This interface exists separately from {@link is.codion.framework.db.EntityResultIterator}
 * because RMI requires all methods in a {@link Remote} interface to declare {@code throws RemoteException}.
 * Client code typically uses {@link is.codion.framework.db.EntityResultIterator} instead,
 * as remote iterators are automatically wrapped via dynamic proxy.
 * @see is.codion.framework.db.EntityResultIterator
 */
public interface RemoteEntityResultIterator extends Remote, AutoCloseable {

	boolean hasNext() throws RemoteException;

	Entity next() throws RemoteException;

	@Override
	void close() throws RemoteException;
}
