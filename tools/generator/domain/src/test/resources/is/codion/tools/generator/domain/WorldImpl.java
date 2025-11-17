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
		return CountryCityView.TYPE.as(
				CountryCityView.COUNTRYCODE.as()
					.column()
					.caption("Countrycode"),
				CountryCityView.COUNTRYNAME.as()
					.column()
					.caption("Countryname"),
				CountryCityView.CONTINENT.as()
					.column()
					.caption("Continent"),
				CountryCityView.REGION.as()
					.column()
					.caption("Region"),
				CountryCityView.SURFACEAREA.as()
					.column()
					.caption("Surfacearea")
					.fractionDigits(2),
				CountryCityView.INDEPYEAR.as()
					.column()
					.caption("Indepyear"),
				CountryCityView.COUNTRYPOPULATION.as()
					.column()
					.caption("Countrypopulation"),
				CountryCityView.LIFEEXPECTANCY.as()
					.column()
					.caption("Lifeexpectancy")
					.fractionDigits(1),
				CountryCityView.GNP.as()
					.column()
					.caption("Gnp")
					.fractionDigits(2),
				CountryCityView.GNPOLD.as()
					.column()
					.caption("Gnpold")
					.fractionDigits(2),
				CountryCityView.LOCALNAME.as()
					.column()
					.caption("Localname"),
				CountryCityView.GOVERNMENTFORM.as()
					.column()
					.caption("Governmentform"),
				CountryCityView.HEADOFSTATE.as()
					.column()
					.caption("Headofstate"),
				CountryCityView.CAPITAL.as()
					.column()
					.caption("Capital"),
				CountryCityView.CODE2.as()
					.column()
					.caption("Code2"),
				CountryCityView.FLAG.as()
					.column()
					.caption("Flag"),
				CountryCityView.CITYID.as()
					.column()
					.caption("Cityid"),
				CountryCityView.CITYNAME.as()
					.column()
					.caption("Cityname"),
				CountryCityView.DISTRICT.as()
					.column()
					.caption("District"),
				CountryCityView.CITYPOPULATION.as()
					.column()
					.caption("Citypopulation"))
			.caption("Country city")
			.readOnly(true)
			.build();
	}

	static EntityDefinition city() {
		return City.TYPE.as(
				City.ID.as()
					.primaryKey(),
				City.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(35),
				City.COUNTRYCODE.as()
					.column()
					.nullable(false)
					.maximumLength(3),
				City.COUNTRYCODE_FK.as()
					.foreignKey()
					.caption("Country"),
				City.DISTRICT.as()
					.column()
					.caption("District")
					.nullable(false)
					.maximumLength(20),
				City.POPULATION.as()
					.column()
					.caption("Population")
					.nullable(false),
				City.LOCATION.as()
					.column()
					.caption("Location"))
			.caption("City")
			.build();
	}

	static EntityDefinition country() {
		return Country.TYPE.as(
				Country.CODE.as()
					.primaryKey()
					.maximumLength(3),
				Country.NAME.as()
					.column()
					.caption("Name")
					.nullable(false)
					.maximumLength(52),
				Country.CONTINENT.as()
					.column()
					.caption("Continent")
					.nullable(false)
					.maximumLength(20),
				Country.REGION.as()
					.column()
					.caption("Region")
					.nullable(false)
					.maximumLength(26),
				Country.SURFACEAREA.as()
					.column()
					.caption("Surfacearea")
					.nullable(false)
					.fractionDigits(2),
				Country.INDEPYEAR.as()
					.column()
					.caption("Indepyear"),
				Country.POPULATION.as()
					.column()
					.caption("Population")
					.nullable(false),
				Country.LIFEEXPECTANCY.as()
					.column()
					.caption("Lifeexpectancy")
					.fractionDigits(1),
				Country.GNP.as()
					.column()
					.caption("Gnp")
					.fractionDigits(2),
				Country.GNPOLD.as()
					.column()
					.caption("Gnpold")
					.fractionDigits(2),
				Country.LOCALNAME.as()
					.column()
					.caption("Localname")
					.nullable(false)
					.maximumLength(45),
				Country.GOVERNMENTFORM.as()
					.column()
					.caption("Governmentform")
					.nullable(false)
					.maximumLength(45),
				Country.HEADOFSTATE.as()
					.column()
					.caption("Headofstate")
					.maximumLength(60),
				Country.CAPITAL.as()
					.column(),
				Country.CAPITAL_FK.as()
					.foreignKey()
					.caption("City"),
				Country.CODE2.as()
					.column()
					.caption("Code2")
					.nullable(false)
					.maximumLength(2),
				Country.FLAG.as()
					.column()
					.caption("Flag"))
			.caption("Country")
			.build();
	}

	static EntityDefinition countrylanguage() {
		return Countrylanguage.TYPE.as(
				Countrylanguage.COUNTRYCODE.as()
					.primaryKey(0)
					.maximumLength(3),
				Countrylanguage.COUNTRYCODE_FK.as()
					.foreignKey()
					.caption("Country"),
				Countrylanguage.LANGUAGE.as()
					.primaryKey(1)
					.maximumLength(30),
				Countrylanguage.ISOFFICIAL.as()
					.column()
					.caption("Isofficial")
					.nullable(false)
					.withDefault(true),
				Countrylanguage.PERCENTAGE.as()
					.column()
					.caption("Percentage")
					.nullable(false)
					.fractionDigits(1))
			.caption("Countrylanguage")
			.build();
	}
}