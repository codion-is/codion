package is.codion.world.domain.api;

import static is.codion.framework.domain.DomainType.domainType;

import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public interface World {
	DomainType DOMAIN = domainType(World.class);

	interface CountryCityV {
		EntityType TYPE = DOMAIN.entityType("world.country_city_v");

		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> COUNTRYNAME = TYPE.stringColumn("countryname");
		Column<String> CONTINENT = TYPE.stringColumn("continent");
		Column<String> REGION = TYPE.stringColumn("region");
		Column<Double> SURFACEAREA = TYPE.doubleColumn("surfacearea");
		Column<Short> INDEPYEAR = TYPE.shortColumn("indepyear");
		Column<Integer> COUNTRYPOPULATION = TYPE.integerColumn("countrypopulation");
		Column<Double> LIFEEXPECTANCY = TYPE.doubleColumn("lifeexpectancy");
		Column<Double> GNP = TYPE.doubleColumn("gnp");
		Column<Double> GNPOLD = TYPE.doubleColumn("gnpold");
		Column<String> LOCALNAME = TYPE.stringColumn("localname");
		Column<String> GOVERNMENTFORM = TYPE.stringColumn("governmentform");
		Column<String> HEADOFSTATE = TYPE.stringColumn("headofstate");
		Column<Integer> CAPITAL = TYPE.integerColumn("capital");
		Column<String> CODE2 = TYPE.stringColumn("code2");
		Column<byte[]> FLAG = TYPE.byteArrayColumn("flag");
		Column<Integer> CITYID = TYPE.integerColumn("cityid");
		Column<String> CITYNAME = TYPE.stringColumn("cityname");
		Column<String> DISTRICT = TYPE.stringColumn("district");
		Column<Integer> CITYPOPULATION = TYPE.integerColumn("citypopulation");
	}

	interface City {
		EntityType TYPE = DOMAIN.entityType("world.city");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> DISTRICT = TYPE.stringColumn("district");
		Column<Integer> POPULATION = TYPE.integerColumn("population");
		Column<Object> LOCATION = TYPE.column("location", Object.class);

		ForeignKey COUNTRYCODE_FK = TYPE.foreignKey("countrycode_fk", COUNTRYCODE, Country.CODE);
	}

	interface Country {
		EntityType TYPE = DOMAIN.entityType("world.country");

		Column<String> CODE = TYPE.stringColumn("code");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> CONTINENT = TYPE.stringColumn("continent");
		Column<String> REGION = TYPE.stringColumn("region");
		Column<Double> SURFACEAREA = TYPE.doubleColumn("surfacearea");
		Column<Short> INDEPYEAR = TYPE.shortColumn("indepyear");
		Column<Integer> POPULATION = TYPE.integerColumn("population");
		Column<Double> LIFEEXPECTANCY = TYPE.doubleColumn("lifeexpectancy");
		Column<Double> GNP = TYPE.doubleColumn("gnp");
		Column<Double> GNPOLD = TYPE.doubleColumn("gnpold");
		Column<String> LOCALNAME = TYPE.stringColumn("localname");
		Column<String> GOVERNMENTFORM = TYPE.stringColumn("governmentform");
		Column<String> HEADOFSTATE = TYPE.stringColumn("headofstate");
		Column<Integer> CAPITAL = TYPE.integerColumn("capital");
		Column<String> CODE2 = TYPE.stringColumn("code2");
		Column<byte[]> FLAG = TYPE.byteArrayColumn("flag");

		ForeignKey CAPITAL_FK = TYPE.foreignKey("capital_fk", CAPITAL, City.ID);
	}

	interface Countrylanguage {
		EntityType TYPE = DOMAIN.entityType("world.countrylanguage");

		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> LANGUAGE = TYPE.stringColumn("language");
		Column<Boolean> ISOFFICIAL = TYPE.booleanColumn("isofficial");
		Column<Double> PERCENTAGE = TYPE.doubleColumn("percentage");

		ForeignKey COUNTRYCODE_FK = TYPE.foreignKey("countrycode_fk", COUNTRYCODE, Country.CODE);
	}
}