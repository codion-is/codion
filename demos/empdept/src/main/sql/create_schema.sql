create user if not exists scott password 'tiger';
alter user scott admin true;

create schema scott;

CREATE TABLE scott.dept (
  deptno INT NOT NULL,
  dname VARCHAR(14) NOT NULL,
  loc VARCHAR(13),
  constraint dept_pk primary key (deptno)
);

CREATE TABLE scott.emp (
  id INT NOT NULL,
  ename VARCHAR(10) NOT NULL,
  job VARCHAR(9),
  mgr INT,
  hiredate DATE,
  sal DECIMAL(7, 2) NOT NULL,
  comm DECIMAL(7, 2),
  deptno INT NOT NULL,
  constraint emp_pk primary key (id),
  constraint emp_dept_fk foreign key (deptno) references scott.dept(deptno),
  constraint emp_mgr_fk foreign key (mgr) references scott.emp(id)
);

CREATE SEQUENCE scott.emp_seq START WITH 17;

INSERT INTO scott.dept(deptno, dname, loc)
VALUES (10, 'Accounting', 'New York'),
  (20, 'Research', 'Dallas'),
  (30, 'Sales', 'Chicaco'),
  (40, 'Operations', 'Boston');

INSERT INTO scott.emp(id, ename, job, mgr, hiredate, sal, comm, deptno)
VALUES (8, 'King', 'President', NULL, '1981-11-17', 5000, NULL, 10),
  (3, 'Jones', 'Manager', 8, '1981-04-02', 2975, NULL, 20),
  (5, 'Blake', 'Manager', 3, '1981-05-01', 2850, NULL, 10),
  (1, 'Allen', 'Salesman', 5, '1981-02-20', 1600, 300, 30),
  (2, 'Ward', 'Salesman', 8, '1981-02-22', 1250, 500, 30),
  (4, 'Martin', 'Salesman', 3, '1981-09-28', 1250, 1400, 30),
  (6, 'Clark', 'Manager', 8, '1981-06-09', 2450, 1500, 10),
  (7, 'Scott', 'Analyst', 3, '1987-04-19', 3000, 1500, 20),
  (9, 'Turner', 'Salesman', 5, '1981-09-08', 1500, 0, 30),
  (10, 'Adams', 'Clerk', 3, '1988-05-15', 1100, NULL, 20),
  (11, 'James', 'Clerk', 5, '1996-10-03', 950, 1500, 10),
  (12, 'Ford', 'Clerk', 3, '1988-12-12', 3200, 1200, 20),
  (13, 'Miller', 'Clerk', 6, '1983-01-23', 1300, 1200, 10),
  (14, 'Ben', 'Clerk', 3, '1989-12-12', 1600, 1200, 20),
  (15, 'Baker', 'Clerk', 5, '2007-01-01', 1000, NULL, 10),
  (16, 'Trevor', 'Clerk', 5, '2007-01-01', 1000, NULL, 10);

commit;