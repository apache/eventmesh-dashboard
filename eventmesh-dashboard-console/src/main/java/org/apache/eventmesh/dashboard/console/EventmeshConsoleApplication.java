package org.apache.eventmesh.dashboard.console;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * @author lambert
 */
@Slf4j
@SpringBootApplication
@ComponentScan({"org.apache.eventmesh.dashboard.core"})
public class EventmeshConsoleApplication {
    public static void main(String[] args) {
        try{
            SpringApplication.run(EventmeshConsoleApplication.class, args);
            log.info("{} 启动成功",EventmeshConsoleApplication.class.getSimpleName());
        }catch (Exception e){
            log.error(e.getMessage(), e);
        }
    }
}
