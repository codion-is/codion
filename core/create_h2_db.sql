RUNSCRIPT FROM '../demos/chinook/src/main/sql/create_schema.sql';
RUNSCRIPT FROM '../demos/empdept/src/main/sql/create_schema.sql';
RUNSCRIPT FROM '../demos/petstore/src/main/sql/create_schema.sql';
RUNSCRIPT FROM '../demos/world/src/main/sql/create_schema.sql';
create user if not exists scott password 'tiger';
alter user scott admin true;