--选出该学生已经选好的课
select co.course_id,
       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join student_selections ss on cs.section_id = ss.section_id
where ss.student_id = ? ;

--选出所有符合条件的课(ALL)
select co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, i.first_name, i.last_name,
       cl.day_of_week, cl.week_num, cl.class_begin, cl.class_end, cl.location
from course co
    join course_section cs on co.course_id = cs.course_id
    join classes cl on cs.section_id = cl.section_id
    join instructor i on cl.instructor_id = i.instructor_id
where semester_id = ?
          and (cs.course_id like ('%'||?||'%') or ? is null)
          and (co.course_name || '[' || cs.section_name || ']' like ('%'||?||'%') or ? is null)
          and (i.first_name like (?||'%') or i.last_name like (?||'%')
              or (i.first_name || i.last_name) like (?||'%')
              or (i.first_name || ' ' || i.last_name) like (?||'%') or ? is null)
          and (cl.day_of_week = ? or ? is null)
          and (? between cl.class_begin and cl.class_end or ? is null)
          and (cl.location = Any (?) or ? is null)
group by co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.class_id, cl.instructor_id, i.first_name, i.last_name,
       cl.day_of_week, cl.class_begin, cl.class_end, cl.location
;

--专业必修和选修
select co.course_id, co.course_name, co.credit, co.class_hour, co.grading ,
       cs.section_id, cs.section_name, cs.total_capacity, cs.left_capacity,
       cl.classid, cl.instructor_id, cl.day_of_week, cl.week_num,
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
       cl.classid, cl.instructor_id, cl.day_of_week, cl.week_num,
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
       cl.classid, cl.instructor_id, cl.day_of_week, cl.week_num,
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