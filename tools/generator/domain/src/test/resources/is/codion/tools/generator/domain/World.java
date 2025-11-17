package is.codion.world.domain;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.world.domain.World.City;
import static is.codion.world.domain.World.Country;
import static is.codion.world.domain.World.CountryCityView;
import static is.codion.world.domain.World.Countrylanguage;
import static is.codion.world.domain.World.DOMAIN;

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

public final class World extends DomainModel {
	public static final DomainType DOMAIN = domainType(World.class);

	public World() {
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

	public interface CountryCityView {
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

		static Dto dto(Entity countryCityView) {
			return countryCityView == null ? null :
				new Dto(countryCityView.get(COUNTRYCODE),
					countryCityView.get(COUNTRYNAME),
					countryCityView.get(CONTINENT),
					countryCityView.get(REGION),
					countryCityView.get(SURFACEAREA),
					countryCityView.get(INDEPYEAR),
					countryCityView.get(COUNTRYPOPULATION),
					countryCityView.get(LIFEEXPECTANCY),
					countryCityView.get(GNP),
					countryCityView.get(GNPOLD),
					countryCityView.get(LOCALNAME),
					countryCityView.get(GOVERNMENTFORM),
					countryCityView.get(HEADOFSTATE),
					countryCityView.get(CAPITAL),
					countryCityView.get(CODE2),
					countryCityView.get(FLAG),
					countryCityView.get(CITYID),
					countryCityView.get(CITYNAME),
					countryCityView.get(DISTRICT),
					countryCityView.get(CITYPOPULATION));
		}

		record Dto(String countrycode, String countryname, String continent, String region,
				Double surfacearea, Short indepyear, Integer countrypopulation, Double lifeexpectancy,
				Double gnp, Double gnpold, String localname, String governmentform, String headofstate,
				Integer capital, String code2, byte[] flag, Integer cityid, String cityname, String district,
				Integer citypopulation) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(COUNTRYCODE, countrycode)
					.with(COUNTRYNAME, countryname)
					.with(CONTINENT, continent)
					.with(REGION, region)
					.with(SURFACEAREA, surfacearea)
					.with(INDEPYEAR, indepyear)
					.with(COUNTRYPOPULATION, countrypopulation)
					.with(LIFEEXPECTANCY, lifeexpectancy)
					.with(GNP, gnp)
					.with(GNPOLD, gnpold)
					.with(LOCALNAME, localname)
					.with(GOVERNMENTFORM, governmentform)
					.with(HEADOFSTATE, headofstate)
					.with(CAPITAL, capital)
					.with(CODE2, code2)
					.with(FLAG, flag)
					.with(CITYID, cityid)
					.with(CITYNAME, cityname)
					.with(DISTRICT, district)
					.with(CITYPOPULATION, citypopulation)
					.build();
			}
		}
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

		static Dto dto(Entity city) {
			return city == null ? null :
				new Dto(city.get(ID),
					city.get(NAME),
					Country.dto(city.get(COUNTRYCODE_FK)),
					city.get(DISTRICT),
					city.get(POPULATION),
					city.get(LOCATION));
		}

		record Dto(Integer id, String name, Country.Dto countrycode, String district, Integer population,
				Object location) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(ID, id)
					.with(NAME, name)
					.with(COUNTRYCODE_FK, countrycode.entity(entities))
					.with(DISTRICT, district)
					.with(POPULATION, population)
					.with(LOCATION, location)
					.build();
			}
		}
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

		static Dto dto(Entity country) {
			return country == null ? null :
				new Dto(country.get(CODE),
					country.get(NAME),
					country.get(CONTINENT),
					country.get(REGION),
					country.get(SURFACEAREA),
					country.get(INDEPYEAR),
					country.get(POPULATION),
					country.get(LIFEEXPECTANCY),
					country.get(GNP),
					country.get(GNPOLD),
					country.get(LOCALNAME),
					country.get(GOVERNMENTFORM),
					country.get(HEADOFSTATE),
					City.dto(country.get(CAPITAL_FK)),
					country.get(CODE2),
					country.get(FLAG));
		}

		record Dto(String code, String name, String continent, String region, Double surfacearea,
				Short indepyear, Integer population, Double lifeexpectancy, Double gnp, Double gnpold,
				String localname, String governmentform, String headofstate, City.Dto capital, String code2,
				byte[] flag) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(CODE, code)
					.with(NAME, name)
					.with(CONTINENT, continent)
					.with(REGION, region)
					.with(SURFACEAREA, surfacearea)
					.with(INDEPYEAR, indepyear)
					.with(POPULATION, population)
					.with(LIFEEXPECTANCY, lifeexpectancy)
					.with(GNP, gnp)
					.with(GNPOLD, gnpold)
					.with(LOCALNAME, localname)
					.with(GOVERNMENTFORM, governmentform)
					.with(HEADOFSTATE, headofstate)
					.with(CAPITAL_FK, capital.entity(entities))
					.with(CODE2, code2)
					.with(FLAG, flag)
					.build();
			}
		}
	}

	public interface Countrylanguage {
		EntityType TYPE = DOMAIN.entityType("world.countrylanguage");

		Column<String> COUNTRYCODE = TYPE.stringColumn("countrycode");
		Column<String> LANGUAGE = TYPE.stringColumn("language");
		Column<Boolean> ISOFFICIAL = TYPE.booleanColumn("isofficial");
		Column<Double> PERCENTAGE = TYPE.doubleColumn("percentage");

		ForeignKey COUNTRYCODE_FK = TYPE.foreignKey("countrycode_fk", COUNTRYCODE, Country.CODE);

		static Dto dto(Entity countrylanguage) {
			return countrylanguage == null ? null :
				new Dto(Country.dto(countrylanguage.get(COUNTRYCODE_FK)),
					countrylanguage.get(LANGUAGE),
					countrylanguage.get(ISOFFICIAL),
					countrylanguage.get(PERCENTAGE));
		}

		record Dto(Country.Dto countrycode, String language, Boolean isofficial, Double percentage) {
			public Entity entity(Entities entities) {
				return entities.entity(TYPE)
					.with(COUNTRYCODE_FK, countrycode.entity(entities))
					.with(LANGUAGE, language)
					.with(ISOFFICIAL, isofficial)
					.with(PERCENTAGE, percentage)
					.build();
			}
		}
	}
}