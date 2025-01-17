package com.springboot.vitalorganize.config;

import com.springboot.vitalorganize.component.ApiAuthenticationFilter;
import com.springboot.vitalorganize.entity.Profile_User.PersonalInformation;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.repository.UserRepository;
import com.springboot.vitalorganize.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.Base64;
import java.util.Locale;


@Configuration
@AllArgsConstructor
@EnableWebSecurity
public class WebConfig implements WebMvcConfigurer {

    private final UserRepository userRepository;
    private final UserService userService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(registry -> {
                    registry.requestMatchers("/", "/css/**", "/js/**", "/images/**", "/additional-registration", "/api/**", "/api", "/change-theme", "/change-lang", "/login", "/error").permitAll();

                    registry.requestMatchers("/chat/**", "/newChat", "/create-group").access((authenticationSupplier, context) -> {
                        Authentication authentication = authenticationSupplier.get();
                        if (authentication instanceof OAuth2AuthenticationToken oauthToken) {
                            OAuth2User oauthUser = oauthToken.getPrincipal();
                            UserEntity user = userService.getCurrentUser(oauthUser, oauthToken);
                            return new AuthorizationDecision(user.isMember());
                        }
                        return new AuthorizationDecision(false); // Zugriff verweigern, wenn kein OAuth2-Token vorhanden ist
                    });

                    registry.anyRequest().authenticated();
                })
                .oauth2Login(oauth2Login -> oauth2Login
                        .loginPage("/login")
                        .successHandler((_, response, authentication) -> {
                            if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
                                String provider = oauth2Token.getAuthorizedClientRegistrationId();
                                if ("discord".equals(provider)) {
                                    handleDiscordLogin(oauth2Token);
                                    response.sendRedirect("/additional-registration");
                                } else if ("google".equals(provider)) {
                                    handleGoogleLogin(oauth2Token);
                                    response.sendRedirect("/additional-registration");
                                } else if("github".equals(provider)) {
                                    handleGitHubLogin(oauth2Token);
                                    response.sendRedirect("/additional-registration");
                                } else {
                                    response.sendRedirect("/");
                                }
                            } else {
                                response.sendRedirect("/");
                            }
                }))
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessUrl("/")
                        .permitAll())
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(accessDeniedHandler()))
                .build();
    }



    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        AccessDeniedHandlerImpl accessDeniedHandler = new AccessDeniedHandlerImpl();
        accessDeniedHandler.setErrorPage("/");
        return accessDeniedHandler;
    }

    private void handleGoogleLogin(OAuth2AuthenticationToken authentication) {
        //Neuen user in Datenbank erstellen auf Grundlage der Informationen von Google, wenn er nicht bereits vorhanden ist
        String username = authentication.getPrincipal().getAttribute("username");
        String email = authentication.getPrincipal().getAttribute("email");
        String picture = authentication.getPrincipal().getAttribute("picture");
        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "google");
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setUsername(username);
            userEntity.setEmail(email);
            userEntity.setUsername("");
            userEntity.setPassword("");
            userEntity.setRole("USER");
            userEntity.setProvider("google");
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setToken(generateAccessToken());
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
        }
    }

    private void handleGitHubLogin(OAuth2AuthenticationToken authentication) {
        //Neuen user in Datenbank erstellen auf Grundlage der Informationen von Github, wenn er nicht bereits vorhanden ist
        String username = authentication.getPrincipal().getAttribute("login");
        String email = authentication.getPrincipal().getAttribute("email");
        String picture = authentication.getPrincipal().getAttribute("avatar_url");

        //Dummy Email erstellen, da Github die Email nicht immer mitliefert
        if (email == null) {
            email = username + "@github.com";
        }

        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "github");
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setEmail(email);
            userEntity.setUsername("");
            userEntity.setPassword("");
            userEntity.setRole("USER");
            userEntity.setProvider("github");
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setToken(generateAccessToken());
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
        }
    }

    private void handleDiscordLogin(OAuth2AuthenticationToken authentication) {
        //Neuen user in Datenbank erstellen auf Grundlage der Informationen von Discord, wenn er nicht bereits vorhanden ist
        OAuth2User oAuth2User = authentication.getPrincipal();
        String discordId = oAuth2User.getAttribute("id");
        String email = oAuth2User.getAttribute("email");
        String avatarHash = oAuth2User.getAttribute("avatar");
        String picture;

        // Holen der URL für das Profilebild von Discord, da der LInk nicht direkt mitgeliefert wird
        if (discordId != null && avatarHash != null) {
            picture = "https://cdn.discordapp.com/avatars/" + discordId + "/" + avatarHash + ".png";
        } else {
            // Default Avatar
            picture = "https://cdn.discordapp.com/embed/avatars/0.png";
        }

        if (discordId == null || email == null) {
            return;
        }

        UserEntity userEntity = userRepository.findByEmailAndProvider(email, "discord");
        if (userEntity == null) {
            userEntity = new UserEntity();
            userEntity.setEmail(email);
            userEntity.setUsername("");
            userEntity.setPassword("");
            userEntity.setRole("USER");
            userEntity.setProvider("discord");
            userEntity.setPublic(true);
            userEntity.setBirthday(LocalDate.of(1900, 1, 1));
            userEntity.setProfilePictureUrl(picture);
            userEntity.setToken(generateAccessToken());
            userEntity.setPersonalInformation(createnewPersonalInformation(userEntity));
            userRepository.save(userEntity);
        }
    }

    /**
     * Erstellt leere personalInformation für den gerade frisch angemeldeten User
     * @param userEntity neue User
     * @return die erstellten Informationen
     */
    private PersonalInformation createnewPersonalInformation(UserEntity userEntity) {
        PersonalInformation personalInformation = new PersonalInformation();
        personalInformation.setFirstName(null);
        personalInformation.setLastName(null);
        personalInformation.setAddress(null);
        personalInformation.setPostalCode(null);
        personalInformation.setCity(null);
        personalInformation.setRegion(null);
        personalInformation.setCountry(null);
        personalInformation.setUser(userEntity);
        return personalInformation;
    }

    /**
     * Für die Funktionalität der Sprachänderung
     * @return localResolver
     */
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

    /**
     * Generiert einen AccesToken der für die spätere Verwendung der REST API benötigt wird
     * @return AccessToken
     */
    public static String generateAccessToken() {
        SecureRandom secureRandom = new SecureRandom();
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }

    /**
     * Öffnet alle /api Endpoints, damit bei diesen die Authentifizierung via AccesToken gemacht werden kann
     * @return registrationBean
     */
    @Bean
    public FilterRegistrationBean<ApiAuthenticationFilter> loggingFilter() {
        FilterRegistrationBean<ApiAuthenticationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new ApiAuthenticationFilter(userRepository));
        registrationBean.addUrlPatterns("/api/**");
        return registrationBean;
    }

    @Bean
    public WebClient webClient() {
        return WebClient.builder().build();
    }
}
