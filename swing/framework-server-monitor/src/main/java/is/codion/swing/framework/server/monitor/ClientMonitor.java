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
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

  private static final int CREATION_TIME = 8;
  private static final int TIMEZONE = 7;
  private static final int LOCALE = 6;
  private static final int CLIENT_ID = 5;
  private static final int CODION_VERSION = 4;
  private static final int CLIENT_VERSION = 3;
  private static final int CLIENT_TYPE = 2;
  private static final int CLIENT_HOST = 1;
  private static final int USER = 0;

  private final EntityServerAdmin server;

  private final FilteredTableModel<RemoteClient, Integer> clientInstanceTableModel =
          FilteredTableModel.builder(new RemoteClientColumnFactory(), new RemoteClientColumnValueProvider())
                  .itemSupplier(new RemoteClientItemSupplier())
                  .build();

  /**
   * Instantiates a new {@link ClientMonitor}
   * @param server the server being monitored
   */
  public ClientMonitor(EntityServerAdmin server) {
    this.server = requireNonNull(server);
    refresh();
  }

  /**
   * Refreshes the client info from the server
   */
  public void refresh() {
    clientInstanceTableModel.refresh();
  }

  /**
   * @return the TableModel for displaying the client instances
   */
  public FilteredTableModel<RemoteClient, Integer> clientInstanceTableModel() {
    return clientInstanceTableModel;
  }

  public EntityServerAdmin server() {
    return server;
  }

  private final class RemoteClientItemSupplier implements Supplier<Collection<RemoteClient>> {

    @Override
    public Collection<RemoteClient> get() {
      try {
        return server.clients();
      }
      catch (RemoteException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static final class RemoteClientColumnFactory implements FilteredTableModel.ColumnFactory<Integer> {

    @Override
    public List<FilteredTableColumn<Integer>> createColumns() {
      return Arrays.asList(
              FilteredTableColumn.builder(USER)
                      .headerValue("User")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CLIENT_HOST)
                      .headerValue("Host")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CLIENT_TYPE)
                      .headerValue("Type")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CLIENT_VERSION)
                      .headerValue("Version")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CODION_VERSION)
                      .headerValue("Framework version")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CLIENT_ID)
                      .headerValue("Id")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(LOCALE)
                      .headerValue("Locale")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(TIMEZONE)
                      .headerValue("Timezone")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(CREATION_TIME)
                      .headerValue("Created")
                      .columnClass(LocalDateTime.class)
                      .build()
      );
    }
  }

  private static final class RemoteClientColumnValueProvider implements FilteredTableModel.ColumnValueProvider<RemoteClient, Integer> {

    @Override
    public Object value(RemoteClient row, Integer columnIdentifier) {
      switch (columnIdentifier) {
        case USER:
          return row.user().username();
        case CLIENT_HOST:
          return row.clientHost();
        case CLIENT_TYPE:
          return row.clientTypeId();
        case CLIENT_VERSION:
          return row.clientVersion() == null ? null : row.clientVersion().toString();
        case CODION_VERSION:
          return row.frameworkVersion().toString();
        case CLIENT_ID:
          return row.clientId().toString();
        case LOCALE:
          return row.clientLocale().toString();
        case TIMEZONE:
          return row.clientTimeZone().toString();
        case CREATION_TIME:
          return row.creationTime();
        default:
          throw new IllegalArgumentException("Unknown column");
      }
    }
  }
}