package com.blibli.future.security;

/**
 * Created by adhikasp on 08/10/2016.
 */
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Autowired
    DataSource dataSource;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.authorizeRequests()
                .antMatchers("/catering/register")
                .permitAll()
                // URL that need special access role need to
                // declared explicitly
                .antMatchers("/admin/**")
                    .access("hasRole('ROLE_ADMIN')")
                .antMatchers("/catering/**")
                    .access("hasRole('ROLE_CATERING')")
                .antMatchers("/user/profile")
                    .access("hasRole('ROLE_USER')")
                // Any other url is accessible by everyone
                .anyRequest()
                    .permitAll()

                .and()
                .formLogin()
                    .loginPage("/user/login")
                    .permitAll()
                    .and()
                .logout()
                    .permitAll();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.jdbcAuthentication().dataSource(dataSource)
                .usersByUsernameQuery("select email, password, enabled from blusea_user where email=?")
                .authoritiesByUsernameQuery("select email, role from user_role where email=?  ");
    }
}
