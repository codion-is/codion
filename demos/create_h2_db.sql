RUNSCRIPT FROM 'chinook/src/main/sql/create_schema.sql';
RUNSCRIPT FROM 'empdept/src/main/sql/create_schema.sql';
RUNSCRIPT FROM 'petstore/src/main/sql/create_schema.sql';
RUNSCRIPT FROM 'world/src/main/sql/create_schema.sql';
create user if not exists scott password 'tiger';
alter user scott admin true;