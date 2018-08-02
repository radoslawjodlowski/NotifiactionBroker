package pl.raaadziu.nb1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import java.util.concurrent.Executors;

@SpringBootApplication
public class Nb1Application {

    public static void main(String[] args) {
        SpringApplication.run(Nb1Application.class, args);
    }

    @Bean //Walk-around for together exists spring websockets and taskScheduler
    public TaskScheduler taskScheduler() {
        return new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor());
    }
}
