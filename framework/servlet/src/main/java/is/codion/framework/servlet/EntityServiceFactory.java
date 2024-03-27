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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.servlet;

import is.codion.common.rmi.server.AuxiliaryServerFactory;
import is.codion.common.rmi.server.Server;
import is.codion.common.rmi.server.ServerAdmin;
import is.codion.framework.db.rmi.RemoteEntityConnection;

/**
 * Provides a {@link EntityService} auxiliary server instance.
 */
public final class EntityServiceFactory implements AuxiliaryServerFactory<RemoteEntityConnection, ServerAdmin, EntityService> {

	@Override
	public EntityService createServer(Server<RemoteEntityConnection, ServerAdmin> server) {
		return new EntityService(server);
	}
}
