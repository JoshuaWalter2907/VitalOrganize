package com.springboot.vitalorganize.config;

import com.springboot.vitalorganize.component.UsernameInterceptor;
import com.springboot.vitalorganize.model.PersonalInformation;
import com.springboot.vitalorganize.model.UserRepository;
import com.springboot.vitalorganize.model.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;


import java.time.LocalDate;
import java.util.Locale;


@Configuration
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UsernameInterceptor usernameInterceptor;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> {
                    // Öffentliche Ressourcen
                    registry.requestMatchers("/", "/css/**", "/js/**", "/images/**", "/profileaddition", "/paypal-webhook").permitAll();
                    registry.requestMatchers("/login", "/error", "/perform_login").permitAll();
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
                                    response.sendRedirect("/profileaddition");
                                } else if ("google".equals(provider)) {
                                    handleGoogleLogin(oauth2Token); // Google-spezifische Logik
                                    response.sendRedirect("/profileaddition");
                                }else if("github".equals(provider)) {
                                    handleGitHubLogin(oauth2Token);
                                    response.sendRedirect("/profileaddition");

                                } else {
                                    response.sendRedirect("/profile");
                                }
                            } else {
                                response.sendRedirect("/");
                            }
                        })
                )
                .logout(logout -> {
                    logout
                            .logoutUrl("/logout") // Definiert den Logout-Endpunkt
                            .invalidateHttpSession(true) // Session wird invalidiert
                            .clearAuthentication(true) // Authentifizierung wird gelöscht
                            .deleteCookies("JSESSIONID") // Cookies (z. B. Session-Cookie) werden entfernt
                            .logoutSuccessUrl("/") // Nach dem Logout auf die Startseite umleiten
                            .permitAll();
                })
                .build();
    }


        private void handleFormLogin(String email, String password, Authentication authentication) {

        authentication.setAuthenticated(true);
        // Prüfen, ob Benutzer bereits existiert
        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "local");

        if (userEntity == null) {
            // Benutzer erstellen, falls nicht vorhanden
            userEntity = new UserEntity();
            userEntity.setEmail(email);
            userEntity.setPassword(passwordEncoder().encode(password)); // Passwort verschlüsseln
            userEntity.setRole("USER"); // Standardrolle
            userEntity.setProvider("local"); // Anbieter auf "local" setzen
            userEntity.setPublic(false);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userRepository.save(userEntity);

            System.out.println("Neuer Benutzer erstellt: " + email);
        } else {
            System.out.println("Benutzer existiert bereits: " + email);
        }

        // Hinweis: Zusätzliche Logik kann hier hinzugefügt werden, wie z.B. Logging oder Auditing
    }



    private void handleGoogleLogin(OAuth2AuthenticationToken authentication) {
        String username = authentication.getPrincipal().getAttribute("username");
        String email = authentication.getPrincipal().getAttribute("email");
        String picture = authentication.getPrincipal().getAttribute("picture");

        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "google");
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setEmail(email);
            userEntity.setUsername("");
            userEntity.setPassword(""); // Passwort leer für OAuth
            userEntity.setRole("USER"); // Standardrolle
            userEntity.setProvider("google"); // Anbieter auf "discord" setzen
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
            System.out.println("Neuer Benutzer erstellt: " + email);
        } else {
            // Falls notwendig, Attribute aktualisieren
            System.out.println("Benutzer aktualisiert: " + email);
        }
    }

    private void handleGitHubLogin(OAuth2AuthenticationToken authentication) {
        // GitHub-spezifische Attribute abrufen
        System.out.println(authentication);
        String username = authentication.getPrincipal().getAttribute("login"); // GitHub-Benutzername
        String email = authentication.getPrincipal().getAttribute("email"); // GitHub-E-Mail-Adresse
        String picture = authentication.getPrincipal().getAttribute("avatar_url");
        System.out.println(picture);

        if (email == null) {
            email = username + "@github.com"; // Dummy-E-Mail erstellen
        }

        // Benutzer in der Datenbank suchen
        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "github");
        if (userEntity == null) {
            // Benutzer erstellen, wenn er nicht existiert
            userEntity = new UserEntity();
            userEntity.setEmail(email); // Als Benutzername die E-Mail verwenden
            userEntity.setUsername("");
            userEntity.setPassword(""); // Passwort leer für OAuth
            userEntity.setRole("USER"); // Standardrolle
            userEntity.setProvider("github"); // Anbieter auf "github" setzen
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
            System.out.println("Neuer Benutzer erstellt: " + email);
        } else {
            // Optional: Bestehende Benutzerattribute aktualisieren
            System.out.println("Benutzer aktualisiert: " + email);
        }
    }

    private void handleDiscordLogin(OAuth2AuthenticationToken authentication) {
        // Extrahiere Benutzerinformationen von Discord
        OAuth2User oAuth2User = authentication.getPrincipal();
        String discordId = oAuth2User.getAttribute("id"); // Benutzer-ID von Discord
        String email = oAuth2User.getAttribute("email"); // E-Mail von Discord
        String picture;
        String avatarHash = oAuth2User.getAttribute("avatar"); // Discord Avatar-Hash

        if (discordId != null && avatarHash != null) {
            picture = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png";
        } else {
            picture = "https://cdn.discordapp.com/embed/avatars/0.png"; // Standardavatar
        }

        if (discordId == null || email == null) {
            System.out.println("Fehler: Keine Benutzer-ID oder E-Mail von Discord erhalten.");
            return;
        }

        // Überprüfen, ob der Benutzer bereits existiert
        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "discord");
        if (userEntity == null) {
            // Benutzer existiert nicht, also neuen erstellen
            userEntity = new UserEntity();
            userEntity.setEmail(email);  // E-Mail als Benutzernamen verwenden
            userEntity.setUsername("");
            userEntity.setPassword("");     // Kein Passwort für OAuth
            userEntity.setRole("USER");
            userEntity.setProvider("discord"); // Standardrolle
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
            System.out.println("Neuer Discord-Benutzer erstellt: " + email);
        } else {
            // Falls erforderlich, Benutzerinformationen aktualisieren
            System.out.println("Benutzer existiert bereits: " + email);
        }
    }

    private PersonalInformation createnewPersonalInformation(UserEntity userEntity) {

        PersonalInformation personalInformation = new PersonalInformation();
        personalInformation.setFirstName(null); // Setze auf null, bis der Benutzer diese Information angibt
        personalInformation.setLastName(null); // Setze auf null
        personalInformation.setAddress(null); // Setze auf null
        personalInformation.setPostalCode(null); // Setze auf null
        personalInformation.setCity(null); // Setze auf null
        personalInformation.setRegion(null); // Setze auf null
        personalInformation.setCountry(null); // Setze auf null
        personalInformation.setUser(userEntity);
        return personalInformation;
    }

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
        registry.addInterceptor(usernameInterceptor)
                .addPathPatterns("/**") // Überall anwenden
                .excludePathPatterns("/profileaddition","/css/**", "/js/**", "/images/**", "/", "/login", "/verify-2fa", "/send-2fa-code", "/logout")
                .excludePathPatterns("/login/**", "/login**");; // Ausnahmen
    }

    @Bean
    public MessageSource messageSource() {
        ResourceBundleMessageSource messageSource = new ResourceBundleMessageSource();
        messageSource.setBasename("languages/translation");
        messageSource.setDefaultEncoding("UTF-8");
        return messageSource;
    }


}
