package com.valeshop.timesheet.infra.security;

import com.valeshop.timesheet.entities.user.User;
import com.valeshop.timesheet.entities.user.UserType;
import com.valeshop.timesheet.repositories.UserRepository;
import com.valeshop.timesheet.services.EmailService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SecurityConfiguration.class);

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(frontendUrl));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"));
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
                    .userInfoEndpoint(userInfo -> userInfo.userService(oAuth2UserService()))
                    .successHandler(oAuth2SuccessHandler())
                    .failureUrl("/login?error=true")
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> {}));
        } catch (NoClassDefFoundError e) {
            log.warn("⚠️ OAuth2 não disponível — executando em modo local sem login Microsoft");
        }

        http.addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    // ✅ Cria usuário automaticamente no primeiro login via Microsoft
    @Bean
    public OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService() {
        log.info("[OAuth2] Entrou no método oAuth2UserService");
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();

        return request -> {
            log.info("[OAuth2] Iniciando processamento do login Microsoft");
            OAuth2User oAuth2User = delegate.loadUser(request);
            Map<String, Object> attributes = oAuth2User.getAttributes();

            String email = (String) attributes.get("preferred_username");
            String name = (String) attributes.get("name");

            if (email != null) {
                log.info("[OAuth2] E-mail recebido: {}", email);
                User existingUser = userRepository.findByEmail(email).orElse(null);

                if (existingUser != null) {
                    log.info("[OAuth2] Usuário já existe: {} (enabled={})", email, existingUser.isEnabled());
                    if (!existingUser.isEnabled()) {
                        String verificationToken = existingUser.getVerificationToken();

                        if (verificationToken == null ||
                            existingUser.getVerificationTokenExpiry() == null ||
                            existingUser.getVerificationTokenExpiry().isBefore(java.time.LocalDateTime.now())) {

                            verificationToken = UUID.randomUUID().toString();
                            existingUser.setVerificationToken(verificationToken);
                            existingUser.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusDays(1));
                            userRepository.save(existingUser);
                            log.info("🔑 Novo token de verificação gerado para usuário existente: {}", verificationToken);
                        }

                        try {
                            emailService.sendVerificationEmail(existingUser.getEmail(), verificationToken);
                            log.info("📧 E-mail de ativação reenviado para: {}", existingUser.getEmail());
                        } catch (Exception e) {
                            log.error("Erro ao reenviar e-mail de ativação para {}: {}", existingUser.getEmail(), e.getMessage());
                        }
                    }
                    return oAuth2User;
                }

                // 🆕 Usuário não existe — cria e envia e-mail
                User newUser = new User();
                newUser.setEmail(email);
                try {
                    newUser.getClass().getMethod("setName", String.class).invoke(newUser, name);
                } catch (Exception ignored) {}

                newUser.setEnabled(false);
                newUser.setUserType(UserType.Normal);
                newUser.setPassword(new BCryptPasswordEncoder().encode("microsoft-login"));

                String verificationToken = UUID.randomUUID().toString();
                newUser.setVerificationToken(verificationToken);
                newUser.setVerificationTokenExpiry(java.time.LocalDateTime.now().plusDays(1));

                try {
                    userRepository.save(newUser);
                    userRepository.flush();
                    log.info("✅ Novo usuário salvo no banco: {}", email);
                } catch (Exception e) {
                    log.error("❌ Erro ao salvar novo usuário {}: {}", email, e.getMessage());
                }

                try {
                    emailService.sendVerificationEmail(newUser.getEmail(), verificationToken);
                    log.info("📧 E-mail de ativação enviado para novo usuário: {}", newUser.getEmail());
                } catch (Exception e) {
                    log.error("❌ Falha ao enviar e-mail de ativação: {}", e.getMessage());
                }

                return oAuth2User;
            }

            log.warn("[OAuth2] E-mail não encontrado nos atributos do usuário Microsoft");
            return oAuth2User; // ✅ Garante retorno em qualquer caso
        };
    }

    @Bean
    public AuthenticationSuccessHandler oAuth2SuccessHandler() {
        return (request, response, authentication) -> {
            log.info("[OAuth2] Iniciando oAuth2SuccessHandler");
            OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
            String email = (String) oAuth2User.getAttributes().get("preferred_username");
            String name = (String) oAuth2User.getAttributes().get("name");

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                log.warn("[OAuth2] Usuário não encontrado no banco: {}", email);
            } else {
                log.info("[OAuth2] Usuário encontrado: {} (enabled={})", email, user.isEnabled());
            }

            if (user == null || !user.isEnabled()) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.setContentType("text/html;charset=UTF-8");
                String message = String.format("Conta criada! Verifique seu e-mail (%s) para ativar o acesso.", email);
                String html = "<html><body><script>\n" +
                    "window.opener && window.opener.postMessage({ type: 'activation', message: '" + message + "' }, '*');\n" +
                    "window.close();\n" +
                    "</script><p>" + message + "</p></body></html>";
                response.getWriter().write(html);
                return;
            }

            String userType = user.getUserType() != null ? user.getUserType().name() : "Normal";
            String token = tokenService.generateToken(user);

            String redirectUrl = String.format(
                "https://controle-demandas.valeshop.com.br/callback#token=%s&userType=%s&name=%s&email=%s",
                URLEncoder.encode(token, StandardCharsets.UTF_8),
                URLEncoder.encode(userType, StandardCharsets.UTF_8),
                URLEncoder.encode(name, StandardCharsets.UTF_8),
                URLEncoder.encode(email, StandardCharsets.UTF_8)
            );

            log.info("[OAuth2] Login bem-sucedido — redirecionando para front-end: {}", redirectUrl);
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
