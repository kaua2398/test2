package com.valeshop.timesheet.infra.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.valeshop.timesheet.exceptions.InvalidTokenException;
import com.valeshop.timesheet.exceptions.UserNotFoundException;
import com.valeshop.timesheet.services.AuthorizationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private AuthorizationService authorizationService;

    private boolean isPublicRoute(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/api/users/register")
                || path.startsWith("/api/users/login")
                || path.startsWith("/api/users/verify-email")
                || path.startsWith("/api/users/reset-password")
                || path.startsWith("/api/users/resend-verification")
                || path.startsWith("/api/users/forgot-password");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String token = recoverToken(request);

            if (token != null) {
                logger.info("Token recebido e será validado."); // Apenas logue o evento
                String login = tokenService.validateToken(token);
                logger.info("Token validado com sucesso.");
                if (login != null && !login.isEmpty()) {
                    UserDetails user = authorizationService.loadUserByUsername(login);
                    var authentication = new UsernamePasswordAuthenticationToken(
                            user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (UserNotFoundException | InvalidTokenException e) {
            SecurityContextHolder.clearContext();
            logger.warn("Token inválido ou usuário não encontrado: {}", e);
        } catch (Exception e) {
            SecurityContextHolder.clearContext();
            logger.error("Erro inesperado no filtro de segurança: {}", e);
        }

        filterChain.doFilter(request, response);
    }



    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null) return null;
        return authHeader.replace("Bearer ", "");
    }
}

