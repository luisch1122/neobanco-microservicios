package com.banco.auth.config.filter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.banco.auth.model.Users;
import com.banco.auth.repository.UsersRepository;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import static com.banco.common_security.config.TokenJwtConfig.*;

public class JwtAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private AuthenticationManager authenticationManager;
    private final SecretKey secretKey;
    private final UsersRepository usersRepository;

    public JwtAuthenticationFilter(AuthenticationManager authenticationManager, SecretKey secretKey, 
        UsersRepository usersRepository) {

        this.authenticationManager = authenticationManager;
        this.secretKey = secretKey;
        this.usersRepository = usersRepository;
        setFilterProcessesUrl("/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {

            Users users = null;
            String email = null;
            String password = null;

            try {
                users = new ObjectMapper().readValue(request.getInputStream(), Users.class);
                email = users.getEmail();
                password = users.getPassword();
            } catch (JacksonException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(email, password);

            return authenticationManager.authenticate(authenticationToken);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain,
            Authentication authResult) throws IOException, ServletException {
        
        User users = (User) authResult.getPrincipal();
        String email = users.getUsername();
        Long userId = usersRepository.findByEmail(email)
                .map(Users::getId)
                .orElseThrow();
        
        Claims claims = Jwts
                .claims()
                .add("userId", userId)
                .build();

        String token = Jwts.builder()
                            .subject(email)
                            .claims(claims)
                            .expiration(new Date(System.currentTimeMillis() + 3600000))
                            .issuedAt(new Date())
                            .signWith(secretKey)
                            .compact();   

        response.addHeader(HEADER_AUTHORIZATION, PREFIX_TOKEN + token);

        Map<String, String> body = new HashMap<>();
        body.put("token", token);
        body.put("email", email);
        body.put("message", String.format("Hola %s has iniciado sesion con exito! ", email));

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setContentType(CONTENT_TYPE);
        response.setStatus(200);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException failed) throws IOException, ServletException {

        Map<String, String> body = new HashMap<>();
                
        body.put("message", "Error en la autenciacion email o password incorrectos!");
        body.put("error", failed.getMessage());

        response.getWriter().write(new ObjectMapper().writeValueAsString(body));
        response.setStatus(401);
        response.setContentType(CONTENT_TYPE);
    }

    
}
