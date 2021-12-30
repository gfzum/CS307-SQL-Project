package my;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.*;
import my.services.*;

import java.util.List;

public class MyFactory extends ServiceFactory {
    public MyFactory() {
        registerService(CourseService.class, new MyCourseService());
        registerService(DepartmentService.class, new MyDepartmentService());
        registerService(InstructorService.class, new MyInstructorService());
        registerService(MajorService.class, new MyMajorService());
        registerService(SemesterService.class, new MySemesterService());
        registerService(StudentService.class, new MyHolyStudentService());
        registerService(UserService.class, new MyUserService());
    }

    @Override
    public List<String> getUIDs() {
        return List.of("12011216", "12011412", "12011411");
    }
}
