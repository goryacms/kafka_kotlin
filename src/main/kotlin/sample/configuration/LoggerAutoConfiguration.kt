package sample.configuration

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InjectionPoint
import org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Scope

@Configuration
class LoggerAutoConfiguration {

  @Bean
  @Scope(SCOPE_PROTOTYPE)
  fun logger(ip: InjectionPoint): Logger = LoggerFactory.getLogger(ip.member.declaringClass)

}