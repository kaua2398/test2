package com.valeshop.timesheet.infra.security;

import com.valeshop.timesheet.services.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        System.out.println("--- SecurityFilter INICIO ---");
        System.out.println("Request URI: " + request.getRequestURI());

        try {
            String token = this.recoverToken(request);
            System.out.println("Token Recuperado: " + token);

            if (token != null) {
                String login = tokenService.validateToken(token);
                if (login != null && !login.isEmpty()) {
                    UserDetails user = this.authorizationService.loadUserByUsername(login);

                    // --- LOG DE DIAGNÓSTICO CRÍTICO ---
                    System.out.println("Utilizador encontrado no filtro: " + user.getUsername());
                    System.out.println("Permissões (Roles) carregadas: " + user.getAuthorities());
                    // ------------------------------------

                    var authentication = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (UsernameNotFoundException e) {
            System.out.println("Token válido, mas o utilizador não foi encontrado na base de dados: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Erro ao processar o token: " + e.getMessage());
        }

        System.out.println("--- SecurityFilter FIM, passando para o próximo filtro ---");
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}

