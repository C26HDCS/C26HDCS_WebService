package kr.kwater.hdcs;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@SpringBootApplication
@MapperScan(basePackages = "kr.kwater.hdcs", annotationClass = org.apache.ibatis.annotations.Mapper.class)
@ImportResource({"classpath:egovframework/spring/context-common.xml",
                 "classpath:egovframework/spring/context-datasource.xml"})
public class HdcsWebServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(HdcsWebServiceApplication.class, args);
    }

}
