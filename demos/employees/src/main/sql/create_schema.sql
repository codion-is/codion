create user if not exists scott password 'tiger';
alter user scott admin true;

create schema employees;

CREATE TABLE employees.department (
  department_no INT NOT NULL,
  name VARCHAR(14) NOT NULL,
  location VARCHAR(13),
  constraint department_pk primary key (department_no)
);

CREATE TABLE employees.employee (
  id INT NOT NULL,
  name VARCHAR(10) NOT NULL,
  job INT NOT NULL,
  manager_id INT,
  hiredate DATE,
  salary DECIMAL(7, 2) NOT NULL,
  commission DECIMAL(7, 2),
  department_no INT NOT NULL,
  constraint employee_pk primary key (id),
  constraint employee_department_fk foreign key (department_no) references employees.department(department_no),
  constraint employee_manager_fk foreign key (manager_id) references employees.employee(id),
  constraint employee_job_chk check (job between 1 and 5)
);

CREATE SEQUENCE employees.employee_seq START WITH 17;

INSERT INTO employees.department(department_no, name, location)
VALUES (10, 'Accounting', 'New York'),
  (20, 'Research', 'Dallas'),
  (30, 'Sales', 'Chicaco'),
  (40, 'Operations', 'Boston');

INSERT INTO employees.employee(id, name, job, manager_id, hiredate, salary, commission, department_no)
VALUES (8, 'King', 1, NULL, '1981-11-17', 5000, NULL, 10),
  (3, 'Jones', 2, 8, '1981-04-02', 2975, NULL, 20),
  (5, 'Blake', 2, 8, '1981-05-01', 2850, NULL, 30),
  (1, 'Allen', 4, 5, '1981-02-20', 1600, 300, 30),
  (2, 'Ward', 4, 5, '1981-02-22', 1250, 500, 30),
  (4, 'Martin', 4, 5, '1981-09-28', 1250, 1400, 30),
  (6, 'Clark', 2, 8, '1981-06-09', 2450, 1500, 10),
  (7, 'Scott', 3, 3, '1987-04-19', 3000, 1500, 20),
  (9, 'Turner', 4, 5, '1981-09-08', 1500, 0, 30),
  (10, 'Adams', 5, 3, '1988-05-15', 1100, NULL, 20),
  (11, 'James', 5, 6, '1996-10-03', 950, 1500, 10),
  (12, 'Ford', 5, 3, '1988-12-12', 3200, 1200, 20),
  (13, 'Miller', 5, 6, '1983-01-23', 1300, 1200, 10),
  (14, 'Ben', 5, 3, '1989-12-12', 1600, 1200, 20),
  (15, 'Baker', 5, 6, '2007-01-01', 1000, NULL, 10),
  (16, 'Trevor', 5, 6, '2007-01-01', 1000, NULL, 10);

commit;