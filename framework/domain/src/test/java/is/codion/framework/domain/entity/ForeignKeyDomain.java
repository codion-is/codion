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
 * Copyright (c) 2020 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.domain.entity;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

class ForeignKeyDomain extends DomainModel {

	static final DomainType DOMAIN = DomainType.domainType("fkDomain");

	ForeignKeyDomain() {
		super(DOMAIN);
		add(species());
		add(maturity());
		add(otolithCategory());
		add(otolith());
	}

	public interface Species {
		EntityType TYPE = DOMAIN.entityType("species");
		Column<Integer> NO = TYPE.integerColumn("no");
		Column<String> NAME = TYPE.stringColumn("name");
	}

	EntityDefinition species() {
		return Species.TYPE.as(
										Species.NO.as()
														.primaryKey()
														.caption("Number"),
										Species.NAME.as()
														.column()
														.caption("Name")
														.maximumLength(50))
						.build();
	}

	public interface Maturity {
		EntityType TYPE = DOMAIN.entityType("species_maturity");
		Column<Integer> NO = TYPE.integerColumn("no");
		Column<Integer> SPECIES_NO = TYPE.integerColumn("species_no");
		ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", Maturity.SPECIES_NO, Species.NO);
	}

	EntityDefinition maturity() {
		return Maturity.TYPE.as(
										Maturity.NO.as()
														.primaryKey(0),
										Maturity.SPECIES_NO.as()
														.primaryKey(1),
										Maturity.SPECIES_FK.as()
														.foreignKey())
						.build();
	}

	public interface OtolithCategory {
		EntityType TYPE = DOMAIN.entityType("otolith_category");
		Column<Integer> NO = TYPE.integerColumn("no");
		Column<Integer> SPECIES_NO = TYPE.integerColumn("species_no");
		ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", OtolithCategory.SPECIES_NO, Species.NO);
	}

	EntityDefinition otolithCategory() {
		return OtolithCategory.TYPE.as(
										OtolithCategory.NO.as()
														.primaryKey(0),
										OtolithCategory.SPECIES_NO.as()
														.primaryKey(1),
										OtolithCategory.SPECIES_FK.as()
														.foreignKey())
						.build();
	}

	public interface Otolith {
		EntityType TYPE = DOMAIN.entityType("otolith");
		Column<Integer> STATION_ID = TYPE.integerColumn("station_id");
		Column<Integer> SPECIES_NO = TYPE.integerColumn("species_no");
		ForeignKey SPECIES_FK = TYPE.foreignKey("species_fk", Otolith.SPECIES_NO, Species.NO);
		Column<Integer> MATURITY_NO = TYPE.integerColumn("maturity_no");
		ForeignKey MATURITY_FK = TYPE.foreignKey("maturity_fk",
						Otolith.MATURITY_NO, Maturity.NO,
						Otolith.SPECIES_NO, Maturity.SPECIES_NO);
		Column<Integer> OTOLITH_CATEGORY_NO = TYPE.integerColumn("otolith_category_no");
		ForeignKey OTOLITH_CATEGORY_FK = TYPE.foreignKey("otolith_category_fk",
						Otolith.OTOLITH_CATEGORY_NO, OtolithCategory.NO,
						Otolith.SPECIES_NO, OtolithCategory.SPECIES_NO);
	}

	EntityDefinition otolith() {
		return Otolith.TYPE.as(
										Otolith.STATION_ID.as()
														.primaryKey(0),
										Otolith.SPECIES_NO.as()
														.primaryKey(1)
														.updatable(true)
														.nullable(false),
										Otolith.SPECIES_FK.as()
														.foreignKey(),
										Otolith.MATURITY_NO.as()
														.column(),
										Otolith.MATURITY_FK.as()
														.foreignKey()
														.readOnly(Otolith.SPECIES_NO),
										Otolith.OTOLITH_CATEGORY_NO.as()
														.column(),
										Otolith.OTOLITH_CATEGORY_FK.as()
														.foreignKey()
														.readOnly(Otolith.SPECIES_NO))
						.build();
	}
}
