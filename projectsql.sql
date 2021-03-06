--create database project2 with encoding = 'UTF8' LC_COLLATE = 'C';

create table course
(
	course_id varchar(20) not null
		constraint course_pk
			primary key,
	course_name varchar(50) not null,
	credit integer not null,
	class_hour integer not null,
	grading varchar(20),
	prerequisite varchar(500)
);

create unique index course_course_id_uindex
    on course (course_id);

create table semester
(
	semester_id serial not null
        constraint semester_pk
            primary key,
	sem_name varchar(50) not null,
	sem_begin date not null,
	sem_end date not null
);

create unique index semester_semester_id_uindex
	on semester (semester_id);


create table course_section
(
	section_id serial not null
		constraint course_section_pk
			primary key,
	section_name varchar(50) not null,
	course_id varchar(20) not null
	    constraint  course_section_course_id_fk
	        references course
                on delete cascade,
	total_capacity integer not null,
	left_capacity integer,
	semester_id int not null
		constraint semester
			references semester
                on delete cascade
);

create unique index course_section_section_id_uindex
    on course_section (section_id);

create table department
(
	dept_id serial not null
		constraint department_pk
			primary key,
	dept_name varchar(50) not null
);

create table instructor
(
	instructor_id int not null
	    constraint instructor_pk
	        primary key,
	first_name varchar(50),
	last_name varchar(50)
);

create unique index instructor_instructor_id_uindex
	on instructor (instructor_id);

create table major
(
	major_id serial not null
        constraint major_pk
            primary key,
	major_name varchar(50) not null,
	department_id int not null
		constraint department
			references department
                on delete cascade
);

create unique index major_major_id_uindex
	on major (major_id);

create table student
(
	student_id int not null
        constraint student_pk
            primary key,
	first_name varchar(50),
	last_name varchar(50),
	enrolled_date date not null,
	major_id int
		constraint major
			references major
                on delete cascade
);

create unique index student_student_id_uindex
	on student (student_id);

create table classes
(
	class_id serial not null,
	instructor_id integer
		constraint instructor
			references instructor
	            on delete cascade,
	day_of_week integer,
	week_num integer,
	class_begin integer,
	class_end integer,
	location varchar(50),
	section_id int
		constraint classes_section_id_fk
			references course_section
                on delete cascade,
    constraint classes_pk
        primary key (class_id, week_num)
);

create index classes_class_id_index
    on classes (class_id);

create table course_majors
(
	course_id varchar(20) not null
	    constraint course_majors_course_id_fk1
	        references course
	            on delete cascade,
	major_id int not null
	    constraint course_majors_major_id_fk2
	        references major
	            on delete cascade,
	course_type varchar(20),
    constraint course_majors_pk
        primary key (course_id,major_id)
);

create table prerequisite
(
    id serial not null
        constraint prerequisite_pk
            primary key,
    course_id varchar(20) not null
        constraint prerequisite_course_id_fk
            references course
                on delete cascade
);

create table student_selections
(
    id serial not null
        constraint student_selections_pk
            primary key,
    student_id int not null
        constraint student_selections_student_fk
            references student
                on delete cascade,
    section_id int not null
        constraint student_selections_section_fk
            references course_section
                on delete cascade,
    grade int
);

create index student_course_selection_index
    on student_selections (student_id, section_id);

create index student_course_selection_index_2
    on student_selections (student_id);