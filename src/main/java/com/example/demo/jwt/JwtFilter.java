package com.example.demo.jwt;

import com.example.demo.exception_handling.MyErrorResponse;
import com.example.demo.exception_handling.MyException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Enumeration;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final Logger log = LoggerFactory.getLogger(JwtFilter.class);

    private final UserDetailsService userDetailsService;
    private final JwtUtil jwtUtil;

    public JwtFilter(@Lazy UserDetailsService userDetailsService, JwtUtil jwtUtil) {
        this.userDetailsService = userDetailsService;
        this.jwtUtil = jwtUtil;
    }


    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException
    {
        log.info("jwt filter");
        final String authHeader = request.getHeader(AUTHORIZATION);
        String userEmail = null;
        final String jwtToken;

//        Enumeration<String> headers = request.getHeaderNames();
//        while(headers.hasMoreElements())
//            System.out.println(headers.nextElement());

        if(authHeader == null || !authHeader.startsWith("Bearer")){
            log.info("auth header is null");
            filterChain.doFilter(request, response);
            return;
        }
        jwtToken = authHeader.substring(7);

        try{
            userEmail = jwtUtil.extractUsername(jwtToken);
        }catch (Exception e){
            response.setContentType("application/json");
            response.setStatus(400);
            response.getWriter().write(new MyErrorResponse(400, "Invalid Token", LocalDate.now()).toString());
            response.getWriter().flush();

        }

        if(userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null){
            UserDetails userDetails = null;
            try{
               userDetails = userDetailsService.loadUserByUsername(userEmail);
            }catch (UsernameNotFoundException e){
                throw new MyException("Username is not found please sign up.");
            }
            final boolean isTokenValid = jwtUtil.validateToken(jwtToken, userDetails);
            if(isTokenValid){
                UsernamePasswordAuthenticationToken
                        authToken = new UsernamePasswordAuthenticationToken
                        (userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                filterChain.doFilter(request, response);
            }else {
                response.setContentType("application/json");
                response.setStatus(400);
                response.getWriter().write(new MyErrorResponse(400, "Invalid Token", LocalDate.now()).toString());
                response.getWriter().flush();

            }
        }

    }
}
