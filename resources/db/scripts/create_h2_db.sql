RUNSCRIPT FROM './resources/demos/empdept/scripts/ddl.sql';
RUNSCRIPT FROM './resources/demos/empdept/scripts/dml.sql';
RUNSCRIPT FROM './resources/demos/petstore/scripts/ddl.sql';
RUNSCRIPT FROM './resources/demos/petstore/scripts/dml.sql';
RUNSCRIPT FROM './resources/demos/chinook/scripts/ddl.sql';
RUNSCRIPT FROM './resources/demos/chinook/scripts/dml.sql';
create user scott password 'tiger';
alter user scott admin true;