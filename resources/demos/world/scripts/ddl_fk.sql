alter table world.city
add constraint city_country_fk
foreign key (countrycode)
references world.country(code);

alter table world.country
add constraint country_capital_fk
foreign key (capital)
references world.city(id);