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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Owner {
  EntityType TYPE = DOMAIN.entityType("petclinic.owner");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<String> FIRST_NAME = TYPE.stringColumn("first_name");
  Column<String> LAST_NAME = TYPE.stringColumn("last_name");
  Column<String> ADDRESS = TYPE.stringColumn("address");
  Column<String> CITY = TYPE.stringColumn("city");
  Column<String> TELEPHONE = TYPE.stringColumn("telephone");
  Column<PhoneType> PHONE_TYPE = TYPE.column("phone_type", PhoneType.class);

  enum PhoneType {
    MOBILE, HOME, WORK
  }
}
