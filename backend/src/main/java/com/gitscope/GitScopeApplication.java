package com.gitscope;

import com.gitscope.config.GitScopeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(GitScopeProperties.class)
public class GitScopeApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitScopeApplication.class, args);
    }
}
