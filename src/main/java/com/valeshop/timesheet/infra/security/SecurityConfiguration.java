package com.valeshop.timesheet.infra.security;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.repositories.UserRepository;
import com.valeshop.timesheet.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/users/register").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/users/verify-email").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/forgot-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/reset-password").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/users/resend-verification").permitAll()
                .requestMatchers("/login/**", "/oauth2/**", "/api/oauth2/**").permitAll()
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().authenticated()
            );

        try {
            http
                .oauth2Login(oauth2 -> oauth2
                    // Removido loginPage customizado para evitar loop de redirecionamento
                    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService()))
                    .successHandler(oAuth2SuccessHandler())
                    .failureUrl("/login?error=true")
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        } catch (NoClassDefFoundError e) {
            System.out.println("⚠️ OAuth2 não disponível — executando em modo local sem login Microsoft");
        }

        http.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ✅ Cria usuário automaticamente no primeiro login via Microsoft
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return request -> {
            OAuth2User oAuth2User = delegate.loadUser(request);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String email = (String) attributes.get("preferred_username");
            String name = (String) attributes.get("name");

            if (email != null) {
                userRepository.findByEmail(email).orElseGet(() -> {
                    User newUser = new User();
                    newUser.setEmail(email);

                    // tenta atribuir o nome se o método existir
                    try {
                        newUser.getClass().getMethod("setName", String.class).invoke(newUser, name);
                    } catch (Exception ignored) {}

                    newUser.setEnabled(false); // Agora exige ativação por e-mail
                    newUser.setUserType(UserType.Normal);
                    newUser.setPassword(new BCryptPasswordEncoder().encode("microsoft-login"));
                    userRepository.save(newUser);
                    System.out.println("✅ Usuário criado automaticamente via Microsoft Login: " + email);

                    // Envia e-mail de ativação
                    try {
                        emailService.sendActivationEmail(newUser);
                    } catch (Exception e) {
                        System.out.println("Erro ao enviar e-mail de ativação: " + e.getMessage());
                    }
                    return newUser;
                });
            }

            return oAuth2User;
        };
    }

    // ✅ Redireciona para o front-end com token e dados do usuário
    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = (String) oAuth2User.getAttributes().get("preferred_username");
            String name = (String) oAuth2User.getAttributes().get("name");

            User user = userRepository.findByEmail(email).orElse(null);
            String userType = (user != null && user.getUserType() != null)
                    ? user.getUserType().name()
                    : "Normal"; // ✅ alinhado ao enum

            String token = tokenService.generateToken(user); // ✅ recebe User, não String

            String redirectUrl = String.format(
                "https://controle-demandas.valeshop.com.br/callback#token=%s&userType=%s&name=%s&email=%s",
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(userType, StandardCharsets.UTF_8),
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(email, StandardCharsets.UTF_8)
            );

            response.setStatus(HttpServletResponse.SC_FOUND);
            response.sendRedirect(redirectUrl);
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
