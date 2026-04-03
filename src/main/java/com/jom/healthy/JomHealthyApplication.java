package com.jom.healthy;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@SpringBootApplication
@EnableSwagger2
// 表示通过aop框架暴露该代理对象,AopContext能够访问
//@EnableAspectJAutoProxy(exposeProxy = true)
public class JomHealthyApplication {

	public static void main(String[] args) {
		// 1. 加载 .env 文件
		Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

		// 2. 将 .env 中的内容注入系统属性，方便 Spring 读取

		dotenv.entries().forEach(entry -> {
			System.setProperty(entry.getKey(), entry.getValue());
		});

		SpringApplication.run(JomHealthyApplication.class, args);
	}

}
