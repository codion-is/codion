/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.rmi.server.RemoteClient;
import is.codion.common.version.Version;
import is.codion.framework.server.EntityServerAdmin;
import is.codion.swing.common.model.component.table.FilteredTableColumn;
import is.codion.swing.common.model.component.table.FilteredTableModel;

import java.rmi.RemoteException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

/**
 * A ClientMonitor
 */
public final class ClientMonitor {

  private static final int CREATION_TIME = 8;
  private static final int TIMEZONE = 7;
  private static final int LOCALE = 6;
  private static final int CLEINT_ID = 5;
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
                      .columnClass(Version.class)
                      .build(),
              FilteredTableColumn.builder(CODION_VERSION)
                      .headerValue("Framework version")
                      .columnClass(Version.class)
                      .build(),
              FilteredTableColumn.builder(CLEINT_ID)
                      .headerValue("Id")
                      .columnClass(String.class)
                      .build(),
              FilteredTableColumn.builder(LOCALE)
                      .headerValue("Locale")
                      .columnClass(Locale.class)
                      .build(),
              FilteredTableColumn.builder(TIMEZONE)
                      .headerValue("Timezone")
                      .columnClass(ZoneId.class)
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
          return row.clientVersion();
        case CODION_VERSION:
          return row.frameworkVersion();
        case CLEINT_ID:
          return row.clientId();
        case LOCALE:
          return row.clientLocale();
        case TIMEZONE:
          return row.clientTimeZone();
        case CREATION_TIME:
          return row.creationTime();
        default:
          throw new IllegalArgumentException("Unknown column");
      }
    }
  }
}