package com.vam.hassan.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import com.vam.hassan.security.UserInfoDetailsService;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
	
	@Autowired
	private UserInfoDetailsService userDetailsService;

	@Bean
	public UserDetailsService userDetailsService() {
	    return userDetailsService;
	}


	/*
	@Bean
	public UserDetailsService userDetailsService() {

		
		  UserDetails admin =
		  User.withUsername("hasnain").password(encoder.encode("hmm")).roles("ADMIN").
		  build();
		  
		  UserDetails user =
		  User.withUsername("hm").password(encoder.encode("hmm")).roles("USER").build()
		  ; return new InMemoryUserDetailsManager(admin, user);
		 

		return new UserInfoDetailsService();
	}*/

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.csrf().disable().authorizeRequests().requestMatchers("/save", "/user/**").permitAll()
				.requestMatchers("/employee-details").authenticated().requestMatchers("/findBy/**").authenticated()
				.anyRequest().authenticated().and().formLogin();
		return http.build();
	}

	@Bean
	public PasswordEncoder encoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationProvider authenticationProvider() {

		DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
		provider.setUserDetailsService(userDetailsService());
		provider.setPasswordEncoder(encoder());
		return provider;

	}
}
