package com.tata.com.tata;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tata.com.tata.users.UserSignIn;
import com.tata.com.tata.users.UserEntity;
import com.tata.com.tata.users.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@EnableWebSecurity
public class WebSecurity extends WebSecurityConfigurerAdapter {

    @Autowired
    UserRepository userRepository;

    private BCryptPasswordEncoder bCryptPasswordEncoder;

    public WebSecurity(BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .authorizeRequests()
                .antMatchers(HttpMethod.POST, Constants.Companion.getSignUpUrl()).permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilter(getAuthenticationFilter())
                .addFilter(new BasicAuthenticationFilter(authenticationManager()) {
                    @Override
                    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
                        String header = request.getHeader(Constants.Companion.getHeaderString());
                        if (header == null || header.startsWith(Constants.Companion.getTokenPrefix()) == false) {
                            SecurityContextHolder.getContext().setAuthentication(null);
                            chain.doFilter(request, response);
                            return;
                        }

                        UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = getAuthentication(request);
                        SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                        chain.doFilter(request, response);
                    }
                })
                .sessionManagement()
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS);
    }

    @Override
    protected void configure(AuthenticationManagerBuilder auth) throws Exception {
        auth.userDetailsService(email -> {
            UserEntity userEntity = userRepository.findByEmail(email);
            if (userEntity == null) {
                throw new UsernameNotFoundException(email);
            } else {
                return new User(userEntity.getEmail(), userEntity.getPassword(), new ArrayList<>());
            }
        }).passwordEncoder(bCryptPasswordEncoder);
    }

    private UsernamePasswordAuthenticationFilter getAuthenticationFilter() {
        UsernamePasswordAuthenticationFilter usernamePasswordAuthenticationFilter = new UsernamePasswordAuthenticationFilter() {
            @Override
            public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
                try {
                    UserSignIn userSignIn = new ObjectMapper().readValue(request.getInputStream(), UserSignIn.class);
                    return authenticationManager().authenticate(new UsernamePasswordAuthenticationToken(
                            userSignIn.getEmail(),
                            userSignIn.getPassword(),
                            new ArrayList<>()
                    ));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            @Override
            protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
                String username = ((User) authResult.getPrincipal()).getUsername();
                String token = Jwts.builder()
                        .setSubject(username)
                        .setExpiration(new Date(System.currentTimeMillis() + Constants.Companion.getExpiryTime()))
                        .signWith(SignatureAlgorithm.HS512, Constants.Companion.getTokenSecret(getApplicationContext()))
                        .compact();

                response.addHeader(Constants.Companion.getHeaderString(), Constants.Companion.getTokenPrefix() + token);
            }
        };

        usernamePasswordAuthenticationFilter.setFilterProcessesUrl("/users/login");
        return usernamePasswordAuthenticationFilter;
    }

    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest httpServletRequest) {
        String token = httpServletRequest.getHeader(Constants.Companion.getHeaderString());
        if (token != null) {
            token = token.replace(Constants.Companion.getTokenPrefix(), "");
            String user = Jwts.parser()
                    .setSigningKey(Constants.Companion.getTokenSecret(getApplicationContext()))
                    .parseClaimsJws(token)
                    .getBody()
                    .getSubject();
            if (user != null) {
                return new UsernamePasswordAuthenticationToken(user, null, new ArrayList<>());
            }
            return null;
        }
        return null;
    }
}
