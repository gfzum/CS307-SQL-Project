CREATE DATABASE project2 WITH ENCODING='UTF8' LC_COLLATE = 'C';

select * FROM project2.public.semester

--冲突查找 v2
select course_name || '[' || section_name || ']' from(
select distinct s2.course_name, s2.section_name
from
(select co1.course_id,
       cl1.week_num, cl1.day_of_week, cl1.class_begin, cl1.class_end
from course co1
    join course_section cs1 on co1.course_id = cs1.course_id
    join classes cl1 on cs1.section_id = cl1.section_id
where cs1.section_id = ?) as s1 --当前section
join
(select co.course_id, co.course_name, cs.section_name,
       cl.week_num, cl.day_of_week, cl.class_begin, cl.class_end
    from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join student_selections ss on cs.section_id = ss.section_id
where student_id = ? and semester_id = ?) s2 --学生选的
on s2.course_id = s1.course_id
or (s2.week_num = s1.week_num and s2.day_of_week = s1.week_num
    and (  (s2.class_begin <= s1.class_begin and s2.class_end >= s1.class_end)
        or (s2.class_begin >= s1.class_begin and s2.class_begin <= s1.class_end)
        or (s2.class_end >= s1.class_begin and s2.class_end <= s1.class_end)))
) sub
order by course_name, section_name;


--课程及时间冲突课程查找
select c1.week_num, c1.day_of_week, c1.class_begin, c1.class_end from classes c1;

select co.course_name || '[' || cs.section_name || ']'
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join student_selections ss on cs.section_id = ss.section_id
where ss.student_id = ? and semester_id = ? and co.course_id = ?
  or cl.week_num = ? and cl.day_of_week = ?
  and (   (cl.class_begin <= ? and cl.class_end >= ?)
       or (cl.class_begin >= ? and cl.class_begin <= ?)
       or (cl.class_end >=? and cl.class_end <= ?))
order by course_name, section_name;

--选出该学生已经选好的课 弃掉
select co.course_id,
       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join student_selections ss on cs.section_id = ss.section_id
where ss.student_id = ? ;

--选出所有符合条件的课(ALL)
select distinct
       co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, i.first_name, i.last_name,
       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
where semester_id = ?
          and (cs.course_id like ('%'|| ? ||'%') or ? is null)
          and (co.course_name || '[' || cs.section_name || ']' like ('%'|| ? ||'%') or ? is null)
          and (i.first_name like (? ||'%') or i.last_name like (? ||'%')
              or (i.first_name || i.last_name) like (? ||'%')
              or (i.first_name || ' ' || i.last_name) like (? ||'%') or ? is null)
          and (cl.day_of_week = ? or ? is null)
          and (? between cl.class_begin and cl.class_end or ? is null)
          and (cl.location = Any (?) or ? is null)
order by co.course_id, co.course_name, cs.section_name
;

--专业必修和选修
select co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, cl.day_of_week, cl.week_num,
       cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
    join course_majors cm on co.course_id = cm.course_id
    join student s on cm.major_id = s.major_id
where s.student_id = ? and semester_id = ?
          and (cs.course_id like ('%'||?||'%') or ? is null)
          and (co.course_name || '[' || cs.section_name || ']' like ('%'||?||'%') or ? is null)
          and (i.first_name like (?||'%') or i.last_name like (?||'%')
              or (i.first_name || i.last_name) like (?||'%')
              or (i.first_name || ' ' || i.last_name) like (?||'%') or ? is null)
          and (cl.day_of_week = ? or ? is null)
          and (? between cl.class_begin and cl.class_end or ? is null)
          and (cl.location = Any (?) or ? is null)
          and cm.course_type = 'MAJOR_COMPULSORY'; -- 'MAJOR_ELECTIVE'

--跨专业
select co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, cl.day_of_week, cl.week_num,
       cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
    join course_majors cm on co.course_id = cm.course_id
    join student s on cm.major_id <> s.major_id --注意
where s.student_id = ? and semester_id = ?
          and (cs.course_id like ('%'||?||'%') or ? is null)
          and (co.course_name || '[' || cs.section_name || ']' like ('%'||?||'%') or ? is null)
          and (i.first_name like (?||'%') or i.last_name like (?||'%')
              or (i.first_name || i.last_name) like (?||'%')
              or (i.first_name || ' ' || i.last_name) like (?||'%') or ? is null)
          and (cl.day_of_week = ? or ? is null)
          and (? between cl.class_begin and cl.class_end or ? is null)
          and (cl.location = Any (?) or ? is null);

--公选
select co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, cl.day_of_week, cl.week_num,
       cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
    join course_majors cm on co.course_id <> cm.course_id --注意
where semester_id = ?
          and (cs.course_id like ('%'||?||'%') or ? is null)
          and (co.course_name || '[' || cs.section_name || ']' like ('%'||?||'%') or ? is null)
          and (i.first_name like (?||'%') or i.last_name like (?||'%')
              or (i.first_name || i.last_name) like (?||'%')
              or (i.first_name || ' ' || i.last_name) like (?||'%') or ? is null)
          and (cl.day_of_week = ? or ? is null)
          and (? between cl.class_begin and cl.class_end or ? is null)
          and (cl.location = Any (?) or ? is null);

--找课程冲突
select section_name from course_section join student_selections ss
    on course_section.section_id = ss.section_id
where course_id = ? and student_id = ?;

--找时间冲突
select course_name, section_name from course_section
    join student_selections ss on course_section.section_id = ss.section_id
    join classes cl on course_section.section_id = cl.section_id
    join course co on course_section.course_id = co.course_id
where student_id = ? and cl.week_num = ? and cl.day_of_week = ?
    and ((cl.class_begin <= ? and cl.class_end >= ?) or (cl.class_begin >= ? and cl.class_begin <= ?)
        or(cl.class_end >=? and cl.class_end <= ?));


--t
select distinct        co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, i.first_name, i.last_name,
       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
where semester_id = ?
and (cs.course_id like ('%'|| ? ||'%') or ? is null)
and (co.course_name || '[' || cs.section_name || ']' like ('%'|| ? ||'%') or ? is null)
and (i.first_name like (? ||'%') or i.last_name like (? ||'%')
    or (i.first_name || i.last_name) like (? ||'%')
    or (i.first_name || ' ' || i.last_name) like (? ||'%') or ? is null)
and (cl.day_of_week = ? or ? is null)
and (? between cl.class_begin and cl.class_end or ? is null)
and (cl.location = Any (?) or ? is null)
order by co.course_id, co.course_name, cs.section_name