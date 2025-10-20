package is.codion.world.domain;

import static is.codion.world.domain.api.World.City;
import static is.codion.world.domain.api.World.Country;
import static is.codion.world.domain.api.World.CountryCityView;
import static is.codion.world.domain.api.World.Countrylanguage;
import static is.codion.world.domain.api.World.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;

public final class WorldImpl extends DomainModel {
	public WorldImpl() {
		super(DOMAIN);
		validateForeignKeys(false);
		add(countryCityView(), city(), country(),
				countrylanguage());
	}

	static EntityDefinition countryCityView() {
		return CountryCityView.TYPE.define(
				CountryCityView.COUNTRYCODE.define()
					.column()
					.caption("Countrycode"),
				CountryCityView.COUNTRYNAME.define()
					.column()
					.caption("Countryname"),
				CountryCityView.CONTINENT.define()
					.column()
					.caption("Continent"),
				CountryCityView.REGION.define()
					.column()
					.caption("Region"),
				CountryCityView.SURFACEAREA.define()
					.column()
					.caption("Surfacearea")
					.fractionDigits(2),
				CountryCityView.INDEPYEAR.define()
					.column()
					.caption("Indepyear"),
				CountryCityView.COUNTRYPOPULATION.define()
					.column()
					.caption("Countrypopulation"),
				CountryCityView.LIFEEXPECTANCY.define()
					.column()
					.caption("Lifeexpectancy")
					.fractionDigits(1),
				CountryCityView.GNP.define()
					.column()
					.caption("Gnp")
					.fractionDigits(2),
				CountryCityView.GNPOLD.define()
					.column()
					.caption("Gnpold")
					.fractionDigits(2),
				CountryCityView.LOCALNAME.define()
					.column()
					.caption("Localname"),
				CountryCityView.GOVERNMENTFORM.define()
					.column()
					.caption("Governmentform"),
				CountryCityView.HEADOFSTATE.define()
					.column()
					.caption("Headofstate"),
				CountryCityView.CAPITAL.define()
					.column()
					.caption("Capital"),
				CountryCityView.CODE2.define()
					.column()
					.caption("Code2"),
				CountryCityView.FLAG.define()
					.column()
					.caption("Flag"),
				CountryCityView.CITYID.define()
					.column()
					.caption("Cityid"),
				CountryCityView.CITYNAME.define()
					.column()
					.caption("Cityname"),
				CountryCityView.DISTRICT.define()
					.column()
					.caption("District"),
				CountryCityView.CITYPOPULATION.define()
					.column()
					.caption("Citypopulation"))
			.caption("Country city")
			.readOnly(true)
			.build();
	}

	static EntityDefinition city() {
		return City.TYPE.define(
				City.ID.define()
					.primaryKey(),
				City.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(35),
				City.COUNTRYCODE.define()
					.column()
					.nullable(false)
					.maximumLength(3),
				City.COUNTRYCODE_FK.define()
					.foreignKey()
					.caption("Country"),
				City.DISTRICT.define()
					.column()
					.caption("District")
					.nullable(false)
					.maximumLength(20),
				City.POPULATION.define()
					.column()
					.caption("Population")
					.nullable(false),
				City.LOCATION.define()
					.column()
					.caption("Location"))
			.caption("City")
			.build();
	}

	static EntityDefinition country() {
		return Country.TYPE.define(
				Country.CODE.define()
					.primaryKey()
					.maximumLength(3),
				Country.NAME.define()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(52),
				Country.CONTINENT.define()
					.column()
					.caption("Continent")
					.nullable(false)
					.maximumLength(20),
				Country.REGION.define()
					.column()
					.caption("Region")
					.nullable(false)
					.maximumLength(26),
				Country.SURFACEAREA.define()
					.column()
					.caption("Surfacearea")
					.nullable(false)
					.fractionDigits(2),
				Country.INDEPYEAR.define()
					.column()
					.caption("Indepyear"),
				Country.POPULATION.define()
					.column()
					.caption("Population")
					.nullable(false),
				Country.LIFEEXPECTANCY.define()
					.column()
					.caption("Lifeexpectancy")
					.fractionDigits(1),
				Country.GNP.define()
					.column()
					.caption("Gnp")
					.fractionDigits(2),
				Country.GNPOLD.define()
					.column()
					.caption("Gnpold")
					.fractionDigits(2),
				Country.LOCALNAME.define()
					.column()
					.caption("Localname")
					.nullable(false)
					.maximumLength(45),
				Country.GOVERNMENTFORM.define()
					.column()
					.caption("Governmentform")
					.nullable(false)
					.maximumLength(45),
				Country.HEADOFSTATE.define()
					.column()
					.caption("Headofstate")
					.maximumLength(60),
				Country.CAPITAL.define()
					.column(),
				Country.CAPITAL_FK.define()
					.foreignKey()
					.caption("City"),
				Country.CODE2.define()
					.column()
					.caption("Code2")
					.nullable(false)
					.maximumLength(2),
				Country.FLAG.define()
					.column()
					.caption("Flag"))
			.caption("Country")
			.build();
	}

	static EntityDefinition countrylanguage() {
		return Countrylanguage.TYPE.define(
				Countrylanguage.COUNTRYCODE.define()
					.primaryKey(0)
					.maximumLength(3),
				Countrylanguage.COUNTRYCODE_FK.define()
					.foreignKey()
					.caption("Country"),
				Countrylanguage.LANGUAGE.define()
					.primaryKey(1)
					.maximumLength(30),
				Countrylanguage.ISOFFICIAL.define()
					.column()
					.caption("Isofficial")
					.nullable(false)
					.withDefault(true),
				Countrylanguage.PERCENTAGE.define()
					.column()
					.caption("Percentage")
					.nullable(false)
					.fractionDigits(1))
			.caption("Countrylanguage")
			.build();
	}
}