package com.springboot.vitalorganize.config;

import com.springboot.vitalorganize.dao.UserRepository;
import com.springboot.vitalorganize.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;
import org.springframework.web.servlet.i18n.SessionLocaleResolver;


import java.io.IOException;
import java.util.Locale;


@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserRepository userRepository;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .authorizeHttpRequests(registry -> {
                    // Öffentliche Ressourcen
                    registry.requestMatchers("/", "/css/**", "/js/**", "/images/**").permitAll();
                    registry.requestMatchers("/login", "/error").permitAll();
                    // Geschützte Ressourcen
                    registry.anyRequest().authenticated();
                })
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/login")
                        .successHandler((request, response, authentication) -> {
                            // Prüfen, ob das Authentication-Objekt ein OAuth2AuthenticationToken ist
                            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                                String provider = oauth2Token.getAuthorizedClientRegistrationId(); // z.B. "google", "discord", "github"

                                if ("discord".equals(provider)) {
                                    handleDiscordLogin(oauth2Token); // Discord-spezifische Logik
                                    response.sendRedirect("/profile");
                                } else if ("google".equals(provider)) {
                                    handleGoogleLogin(oauth2Token); // Google-spezifische Logik
                                    response.sendRedirect("/profile");
                                } else {
                                    response.sendRedirect("/profile");
                                }
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                )
                .formLogin(form -> {
                    form
                            .loginPage("/login") // Login-Seite für Formular-Login
                            .defaultSuccessUrl("/profile", true) // Nach erfolgreichem Login weiterleiten
                            .permitAll();
                })
                .logout(logout -> {
                    logout
                            .logoutUrl("/logout") // Logout-Endpunkt
                            .logoutSuccessUrl("/") // Nach Logout auf die Startseite umleiten
                            .permitAll();
                })
                .build();
    }

    private void handleGoogleLogin(OAuth2AuthenticationToken authentication) {
        String username = authentication.getPrincipal().getAttribute("username");
        String email = authentication.getPrincipal().getAttribute("email");

        UserEntity userEntity = userRepository.findByUsernameAndProvider(email, "google");
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setUsername(email);
            userEntity.setPassword(""); // Passwort leer für OAuth
            userEntity.setRole("USER"); // Standardrolle
            userEntity.setProvider("google"); // Anbieter auf "discord" setzen
            userRepository.save(userEntity);
            System.out.println("Neuer Benutzer erstellt: " + username);
        } else {
            // Falls notwendig, Attribute aktualisieren
            System.out.println("Benutzer aktualisiert: " + username);
        }
    }

    private void handleDiscordLogin(OAuth2AuthenticationToken authentication) {
        // Extrahiere Benutzerinformationen von Discord
        OAuth2User oAuth2User = authentication.getPrincipal();
        String discordId = oAuth2User.getAttribute("id"); // Benutzer-ID von Discord
        String email = oAuth2User.getAttribute("email"); // E-Mail von Discord

        if (discordId == null || email == null) {
            System.out.println("Fehler: Keine Benutzer-ID oder E-Mail von Discord erhalten.");
            return;
        }

        // Überprüfen, ob der Benutzer bereits existiert
        UserEntity userEntity = userRepository.findByUsernameAndProvider(email, "discord");
        if (userEntity == null) {
            // Benutzer existiert nicht, also neuen erstellen
            userEntity = new UserEntity();
            userEntity.setUsername(email);  // E-Mail als Benutzernamen verwenden
            userEntity.setPassword("");     // Kein Passwort für OAuth
            userEntity.setRole("USER");
            userEntity.setProvider("discord"); // Standardrolle
            userRepository.save(userEntity);
            System.out.println("Neuer Discord-Benutzer erstellt: " + email);
        } else {
            // Falls erforderlich, Benutzerinformationen aktualisieren
            System.out.println("Benutzer existiert bereits: " + email);
        }
    }

    private static String getUsername(Authentication authentication) {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String username = oAuth2User.getAttribute("login");
        if (username == null || username.isEmpty()) {
            username = oAuth2User.getAttribute("email"); // Alternative
        }
        if (username == null || username.isEmpty()) {
            throw new IllegalArgumentException("Both login and email are missing from OAuth2 response.");
        }
        return username;
    }


//    @Bean
//    public UserDetailsService userDetailsService() {
//        return username -> {
//            UserEntity userEntity = userRepository.findByUsernameAndProvider(username); // Benutzer aus DB holen
//            if (userEntity != null) {
//                return User.withUsername(userEntity.getUsername())
//                        .password(userEntity.getPassword())
//                        .roles(userEntity.getRole())
//                        .build();
//            } else {
//                throw new RuntimeException("User not found");
//            }
//        };
//    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver localeResolver = new CookieLocaleResolver();
        localeResolver.setDefaultLocale(Locale.GERMAN);
        localeResolver.setCookieName("lang");
        return localeResolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("languages/translation");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }

}
