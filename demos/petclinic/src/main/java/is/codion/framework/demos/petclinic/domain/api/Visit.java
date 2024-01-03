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
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.time.LocalDate;

import static is.codion.framework.demos.petclinic.domain.api.Petclinic.DOMAIN;

public interface Visit {
  EntityType TYPE = DOMAIN.entityType("petclinic.visit");

  Column<Integer> ID = TYPE.integerColumn("id");
  Column<Integer> PET_ID = TYPE.integerColumn("pet_id");
  Column<LocalDate> VISIT_DATE = TYPE.localDateColumn("visit_date");
  Column<String> DESCRIPTION = TYPE.stringColumn("description");

  ForeignKey PET_FK = TYPE.foreignKey("pet_fk", PET_ID, Pet.ID);
}
