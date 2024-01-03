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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model.tools.metadata;

import is.codion.common.db.result.ResultPacker;

import java.sql.ResultSet;
import java.sql.SQLException;

final class ForeignKeyColumnPacker implements ResultPacker<ForeignKeyColumn> {

  @Override
  public ForeignKeyColumn get(ResultSet resultSet) throws SQLException {
    String pktableSchem = resultSet.getString("PKTABLE_SCHEM");
    if (pktableSchem == null) {
      pktableSchem = resultSet.getString("PKTABLE_CAT");
    }
    String fktableSchem = resultSet.getString("FKTABLE_SCHEM");
    if (fktableSchem == null) {
      fktableSchem = resultSet.getString("FKTABLE_CAT");
    }
    return new ForeignKeyColumn(pktableSchem,
            resultSet.getString("PKTABLE_NAME"),
            resultSet.getString("PKCOLUMN_NAME"),
            resultSet.getString("FKTABLE_NAME"),
            fktableSchem,
            resultSet.getString("FKCOLUMN_NAME"),
            resultSet.getInt("KEY_SEQ"));
  }
}
