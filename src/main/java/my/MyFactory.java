package my;

import cn.edu.sustech.cs307.factory.ServiceFactory;
import cn.edu.sustech.cs307.service.CourseService;
import my.services.MyCourseService;

import java.util.List;

public class MyFactory extends ServiceFactory {
    public MyFactory() {
        super();
        registerService(CourseService.class, new MyCourseService());
        //hhlkj
    }

    @Override
    public List<String> getUIDs() {
        return List.of("12011216", "12011412", "12011411");
    }
}
