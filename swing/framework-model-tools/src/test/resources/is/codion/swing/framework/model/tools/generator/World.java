package is.codion.world.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.world.domain.World.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.world.domain.World.City;
import is.codion.world.domain.World.Country;
import is.codion.world.domain.World.CountryCityV;
import is.codion.world.domain.World.Countrylanguage;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public final class World extends DomainModel {
	public static final DomainType DOMAIN = domainType(World.class);

	public World() {
		super(DOMAIN);
		setStrictForeignKeys(false);
		add(city(), country(), countrylanguage(),
				countryCityV());
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
					.maximumFractionDigits(2),
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
					.maximumFractionDigits(1),
				Country.GNP.define()
					.column()
					.caption("Gnp")
					.maximumFractionDigits(2),
				Country.GNPOLD.define()
					.column()
					.caption("Gnpold")
					.maximumFractionDigits(2),
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
					.columnHasDefaultValue(true),
				Countrylanguage.PERCENTAGE.define()
					.column()
					.caption("Percentage")
					.nullable(false)
					.maximumFractionDigits(1))
			.caption("Countrylanguage")
			.build();
	}

	static EntityDefinition countryCityV() {
		return CountryCityV.TYPE.define(
				CountryCityV.COUNTRYCODE.define()
					.column()
					.caption("Countrycode"),
				CountryCityV.COUNTRYNAME.define()
					.column()
					.caption("Countryname"),
				CountryCityV.CONTINENT.define()
					.column()
					.caption("Continent"),
				CountryCityV.REGION.define()
					.column()
					.caption("Region"),
				CountryCityV.SURFACEAREA.define()
					.column()
					.caption("Surfacearea"),
				CountryCityV.INDEPYEAR.define()
					.column()
					.caption("Indepyear"),
				CountryCityV.COUNTRYPOPULATION.define()
					.column()
					.caption("Countrypopulation"),
				CountryCityV.LIFEEXPECTANCY.define()
					.column()
					.caption("Lifeexpectancy"),
				CountryCityV.GNP.define()
					.column()
					.caption("Gnp"),
				CountryCityV.GNPOLD.define()
					.column()
					.caption("Gnpold"),
				CountryCityV.LOCALNAME.define()
					.column()
					.caption("Localname"),
				CountryCityV.GOVERNMENTFORM.define()
					.column()
					.caption("Governmentform"),
				CountryCityV.HEADOFSTATE.define()
					.column()
					.caption("Headofstate"),
				CountryCityV.CAPITAL.define()
					.column()
					.caption("Capital"),
				CountryCityV.CODE2.define()
					.column()
					.caption("Code2"),
				CountryCityV.FLAG.define()
					.column()
					.caption("Flag"),
				CountryCityV.CITYID.define()
					.column()
					.caption("Cityid"),
				CountryCityV.CITYNAME.define()
					.column()
					.caption("Cityname"),
				CountryCityV.DISTRICT.define()
					.column()
					.caption("District"),
				CountryCityV.CITYPOPULATION.define()
					.column()
					.caption("Citypopulation"))
			.caption("Country city v")
			.readOnly(true)
			.build();
	}

	public interface City {
		EntityType TYPE = DOMAIN.entityType("world.city");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> DISTRICT = TYPE.stringColumn("district");
		Column<Integer> POPULATION = TYPE.integerColumn("population");
		Column<Object> LOCATION = TYPE.column("location", Object.class);

		ForeignKey COUNTRYCODE_FK = TYPE.foreignKey("countrycode_fk", COUNTRYCODE, Country.CODE);
	}

	public interface Country {
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

	public interface Countrylanguage {
		EntityType TYPE = DOMAIN.entityType("world.countrylanguage");

		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> LANGUAGE = TYPE.stringColumn("language");
		Column<Boolean> ISOFFICIAL = TYPE.booleanColumn("isofficial");
		Column<Double> PERCENTAGE = TYPE.doubleColumn("percentage");

		ForeignKey COUNTRYCODE_FK = TYPE.foreignKey("countrycode_fk", COUNTRYCODE, Country.CODE);
	}

	public interface CountryCityV {
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
}