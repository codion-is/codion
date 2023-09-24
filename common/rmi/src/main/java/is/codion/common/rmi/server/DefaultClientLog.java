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
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.common.rmi.server;

import is.codion.common.logging.MethodLogger;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

final class DefaultClientLog implements ClientLog, Serializable {

  private static final long serialVersionUID = 1;

  private final UUID clientId;
  private final List<MethodLogger.Entry> entries;

  DefaultClientLog(UUID clientId, List<MethodLogger.Entry> entries) {
    this.clientId = requireNonNull(clientId);
    this.entries = requireNonNull(entries);
  }

  @Override
  public List<MethodLogger.Entry> entries() {
    return entries;
  }

  @Override
  public UUID clientId() {
    return clientId;
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (!(object instanceof DefaultClientLog)) {
      return false;
    }
    DefaultClientLog that = (DefaultClientLog) object;

    return clientId.equals(that.clientId);
  }

  @Override
  public int hashCode() {
    return clientId.hashCode();
  }
}
