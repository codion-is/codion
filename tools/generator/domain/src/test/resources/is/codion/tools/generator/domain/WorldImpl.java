package is.codion.world.domain;

import static is.codion.world.domain.api.World.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.world.domain.api.World.City;
import is.codion.world.domain.api.World.Country;
import is.codion.world.domain.api.World.CountryCity;
import is.codion.world.domain.api.World.Countrylanguage;

public final class WorldImpl extends DomainModel {
	public WorldImpl() {
		super(DOMAIN);
		validateForeignKeys(false);
		add(countryCity(), city(), country(),
				countrylanguage());
	}

	static EntityDefinition countryCity() {
		return CountryCity.TYPE.define(
				CountryCity.COUNTRYCODE.define()
					.column()
					.caption("Countrycode"),
				CountryCity.COUNTRYNAME.define()
					.column()
					.caption("Countryname"),
				CountryCity.CONTINENT.define()
					.column()
					.caption("Continent"),
				CountryCity.REGION.define()
					.column()
					.caption("Region"),
				CountryCity.SURFACEAREA.define()
					.column()
					.caption("Surfacearea")
					.fractionDigits(2),
				CountryCity.INDEPYEAR.define()
					.column()
					.caption("Indepyear"),
				CountryCity.COUNTRYPOPULATION.define()
					.column()
					.caption("Countrypopulation"),
				CountryCity.LIFEEXPECTANCY.define()
					.column()
					.caption("Lifeexpectancy")
					.fractionDigits(1),
				CountryCity.GNP.define()
					.column()
					.caption("Gnp")
					.fractionDigits(2),
				CountryCity.GNPOLD.define()
					.column()
					.caption("Gnpold")
					.fractionDigits(2),
				CountryCity.LOCALNAME.define()
					.column()
					.caption("Localname"),
				CountryCity.GOVERNMENTFORM.define()
					.column()
					.caption("Governmentform"),
				CountryCity.HEADOFSTATE.define()
					.column()
					.caption("Headofstate"),
				CountryCity.CAPITAL.define()
					.column()
					.caption("Capital"),
				CountryCity.CODE2.define()
					.column()
					.caption("Code2"),
				CountryCity.FLAG.define()
					.column()
					.caption("Flag"),
				CountryCity.CITYID.define()
					.column()
					.caption("Cityid"),
				CountryCity.CITYNAME.define()
					.column()
					.caption("Cityname"),
				CountryCity.DISTRICT.define()
					.column()
					.caption("District"),
				CountryCity.CITYPOPULATION.define()
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