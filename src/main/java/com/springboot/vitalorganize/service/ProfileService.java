package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.entity.Fund_Payments.PaymentEntity;
import com.springboot.vitalorganize.entity.Fund_Payments.TransactionSubscriptionEntity;
import com.springboot.vitalorganize.entity.Profile_User.FriendRequestEntity;
import com.springboot.vitalorganize.entity.Profile_User.PersonalInformation;
import com.springboot.vitalorganize.entity.Profile_User.UserEntity;
import com.springboot.vitalorganize.entity.*;
import com.springboot.vitalorganize.model.Profile.*;
import com.springboot.vitalorganize.repository.PaymentRepository;
import com.springboot.vitalorganize.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Service-Klasse zur Handhabung von Benutzerprofilen.
 * Diese Klasse bietet Methoden zur Verwaltung und Anzeige von Benutzerprofilen,
 * zur Verwaltung von Freundschaftsanfragen, Blockierungen sowie Abonnements und Zahlungsverläufen.
 */
@Service
@AllArgsConstructor
public class ProfileService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final UserService userService;
    private final PaypalService paypalService;


    /**
     * Holt eine Liste potentieller Freunde für den aktuellen Benutzer.
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste potentieller Freunde
     */
    public List<UserEntity> getPotentialFriends(UserEntity currentUser) {
        List<UserEntity> blockedUsers = currentUser.getBlockedUsers();
        List<UserEntity> allUsers = userRepository.findAll();

        return allUsers.stream()
                .filter(u -> !u.equals(currentUser))
                .filter(u -> !currentUser.getFriends().contains(u))
                .filter(u -> !blockedUsers.contains(u))
                .filter(u -> !u.getBlockedUsers().contains(currentUser))
                .filter(u -> currentUser.getSentFriendRequests().stream()
                        .noneMatch(fr -> fr.getReceiver().equals(u) && fr.getStatus() == FriendRequestEntity.RequestStatus.PENDING))
                .collect(Collectors.toList());
    }

    /**
     * Holt die Freundschaftsanfragen, die der Benutzer empfangen hat.
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste von Freundschaftsanfragen
     */
    public List<FriendRequestEntity> getFriendRequests(UserEntity currentUser) {
        return currentUser.getReceivedFriendRequests();
    }

    /**
     * Holt die Freundschaftsanfragen, die der Benutzer gesendet hat und noch offen sind.
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste offener gesendeter Freundschaftsanfragen
     */
    public List<FriendRequestEntity> getSentRequests(UserEntity currentUser) {
        return currentUser.getSentFriendRequests().stream()
                .filter(fr -> fr.getStatus() == FriendRequestEntity.RequestStatus.PENDING)
                .collect(Collectors.toList());
    }

    /**
     * Holt die blockierten Benutzer des aktuellen Benutzers.
     * @param currentUser Das Benutzerobjekt des aktuellen Benutzers
     * @return Eine Liste blockierter Benutzer
     */
    public List<UserEntity> getBlockedUsers(UserEntity currentUser) {
        return currentUser.getBlockedUsers();
    }


    /**
     * Holt eine Liste der Abonnements eines Benutzers und kehrt die Reihenfolge um.
     * @param profileData Das Benutzerobjekt des angeforderten Profils
     * @return Eine Liste der Abonnements
     */
    public List<SubscriptionEntity> getSubscriptions(UserEntity profileData) {
        List<SubscriptionEntity> subscriptions = profileData.getSubscriptions();
        Collections.reverse(subscriptions);
        return subscriptions;
    }

    /**
     * Holt die Transaktionshistorie für das Abonnement eines Benutzers, wenn das Abonnement existiert.
     * Filtert die Transaktionen nach den angegebenen Kriterien wie Username, Datum und Betrag.
     * @param profileData Das Benutzerobjekt
     * @param kind        Der Abonnementtyp
     * @param username    Der Benutzername
     * @param datefrom    Das Startdatum
     * @param dateto      Das Enddatum
     * @param amount      Der Betrag
     * @return Eine Liste von Transaktionen oder null
     */
    public List<TransactionSubscriptionEntity> getTransactionHistory(UserEntity profileData, String kind, String username, LocalDate datefrom, LocalDate dateto, Long amount) {
        if ("premium".equals(kind) && profileData.getLatestSubscription() != null) {
            List<TransactionSubscriptionEntity> transactions = paypalService.getTransactionsForSubscription(
                    profileData.getLatestSubscription().getSubscriptionId()
            );
            return paypalService.filterTransactions(transactions, username, datefrom, dateto, amount);
        }
        return null;
    }

    /**
     * Holt gefilterte Zahlungen basierend auf den angegebenen Kriterien
     * @param profileData Das Benutzerobjekt
     * @param username    Der Benutzername
     * @param reason      Der Grund
     * @param datefrom    Das Startdatum
     * @param dateto      Das Enddatum
     * @param amount      Der Betrag
     * @return Eine Liste gefilterter Zahlungen
     */
    public List<PaymentEntity> getFilteredPayments(UserEntity profileData, String username, String reason, LocalDate datefrom, LocalDate dateto, Long amount) {
        List<PaymentEntity> payments = paymentRepository.findAllByUser(profileData);
        return paypalService.filterPayments(payments, username, reason, datefrom, dateto, amount);
    }

    /**
     * Bestimmt den aktiven Tab basierend auf dem übergebenen Tab-Namen.
     * @param tab Der Name des Tabs
     * @return Der Name des aktiven Tabs
     */
    public String determineTab(String tab) {
        if ("subscription".equals(tab)) {
            return "subscription";
        } else if ("paymenthistory".equals(tab)) {
            return "paymenthistory";
        }
        return "general";
    }

    /**
     * Updated ein Benutzerprofil direkt nach der Registrierung
     * @param registrationAdditionResponseDTO benötigte Informationen
     */
    public void updateUserProfile(RegistrationAdditionResponseDTO registrationAdditionResponseDTO) {
        UserEntity currentUser = userService.getCurrentUser();

        currentUser.setUsername(registrationAdditionResponseDTO.getUsername());
        currentUser.setBirthday(LocalDate.parse(registrationAdditionResponseDTO.getBirthday()));
        if(registrationAdditionResponseDTO.getEmail() != null)
            currentUser.setSendToEmail(registrationAdditionResponseDTO.getEmail());
        userRepository.save(currentUser);
    }

    /**
     * Aktualisiert das Profil eines Benutzers in einem späteren Schritt
     * @param profileEditRequestDTO Das Profil-Request-Objekt mit den neuen Daten
     */
    public void updateUserProfile( ProfileEditRequestDTO profileEditRequestDTO) {
        UserEntity userEntity = userService.getCurrentUser();
        PersonalInformation personalInformation = userEntity.getPersonalInformation();

        System.out.println(profileEditRequestDTO.getPublicPrivateToggle());

        userEntity.setPublic("on".equals(profileEditRequestDTO.getPublicPrivateToggle()));

        personalInformation.setAddress(profileEditRequestDTO.getAddress());
        personalInformation.setCity(profileEditRequestDTO.getCity());
        personalInformation.setRegion(profileEditRequestDTO.getRegion());
        personalInformation.setPostalCode(profileEditRequestDTO.getPostalCode());
        personalInformation.setFirstName(profileEditRequestDTO.getSurname());
        personalInformation.setLastName(profileEditRequestDTO.getName());
        personalInformation.setUser(userEntity);

        userEntity.setPersonalInformation(personalInformation);
        userRepository.save(userEntity);
    }

    /**
     * Erstellt alle Informationen um die ProfilePage anzuzeigen
     * @param profileRequestDTO benötigte Informationen
     * @return DTO für den Controller
     */
    public ProfileResponseDTO prepareProfilePage(
            ProfileRequestDTO profileRequestDTO
    ) {
        ProfileResponseDTO profileResponseDTO = new ProfileResponseDTO();
        UserEntity userEntity = profileRequestDTO.getProfileId() != null ?
                userRepository.findUserEntityById(profileRequestDTO.getProfileId()) : userService.getCurrentUser();

        profileResponseDTO.setUserEntity(userEntity);
        if(profileRequestDTO.getProfileId() == null){
            profileResponseDTO.setBlockedUsers(getBlockedUsers(userEntity));
            profileResponseDTO.setPotentialFriends(getPotentialFriends(userEntity));
            profileResponseDTO.setFriendRequests(getFriendRequests(userEntity));
            profileResponseDTO.setOutgoingFriendRequests(getSentRequests(userEntity));
        }
        profileResponseDTO.setFriends(userEntity.getFriends());
        return profileResponseDTO;
    }

    /**
     * Erstellt alle Informationen um die ProfileEditPage anzuzeigen
     * @param profileEditRequestDTO benötigte Informationen
     * @return DTO für den Controller
     */
    public ProfileEditResponseDTO prepareProfileEditPage(ProfileEditRequestDTO profileEditRequestDTO, HttpServletRequest request, HttpSession session) {

        ProfileEditResponseDTO profileEditResponseDTO = new ProfileEditResponseDTO();

        session.setAttribute("uri", request.getRequestURI());
        profileEditResponseDTO.setUrl(request.getRequestURI());

        UserEntity userEntity = userService.getUser(profileEditRequestDTO.getProfileId());
        List<SubscriptionEntity> subscriptions = getSubscriptions(userEntity);

        profileEditResponseDTO.setSubscriptions(subscriptions);
        profileEditResponseDTO.setProfile(userEntity);
        profileEditResponseDTO.setProfilePublic(userEntity.isPublic());
        profileEditResponseDTO.setAuth(profileEditRequestDTO.isFa());
        profileEditResponseDTO.setKind(profileEditRequestDTO.getKind());

        int pageNumber = profileEditRequestDTO.getPage();
        int pageSize = profileEditRequestDTO.getSize();
        profileEditResponseDTO.setPageSize(pageSize);

        if("premium".equals(profileEditRequestDTO.getKind())){
            List<TransactionSubscriptionEntity> transactionSubscriptions = getTransactionHistory(
                    userEntity,
                    profileEditRequestDTO.getKind(),
                    profileEditRequestDTO.getUsername(),
                    profileEditRequestDTO.getDatefrom(),
                    profileEditRequestDTO.getDateto(),
                    profileEditRequestDTO.getAmount()
            );
            int totalSubscriptions = transactionSubscriptions.size();
            int totalSubscriptionPages = (int) Math.ceil((double) totalSubscriptions / pageSize);

            int startIndexSubscriptions = pageNumber * pageSize;
            int endIndexSubscriptions = Math.min(startIndexSubscriptions + pageSize, totalSubscriptions);

            List<TransactionSubscriptionEntity> pagedSubscriptions = transactionSubscriptions.subList(startIndexSubscriptions, endIndexSubscriptions);

            profileEditResponseDTO.setTotalSubscriptions(totalSubscriptions);
            profileEditResponseDTO.setTotalSubscriptionPages(totalSubscriptionPages);
            profileEditResponseDTO.setStartIndexSubscriptions(startIndexSubscriptions);
            profileEditResponseDTO.setEndIndexSubscriptions(endIndexSubscriptions);
            profileEditResponseDTO.setHistorysubscription(pagedSubscriptions);

        }else{
            List<PaymentEntity> payments = getFilteredPayments(
                    userEntity,
                    profileEditRequestDTO.getUsername(),
                    profileEditRequestDTO.getReason(),
                    profileEditRequestDTO.getDatefrom(),
                    profileEditRequestDTO.getDateto(),
                    profileEditRequestDTO.getAmount()
            );

            int totalPayments = payments.size();
            int totalPaymentsPages = (int) Math.ceil((double) totalPayments / pageSize);

            int startIndexPayments = pageNumber * pageSize;
            int endIndexPayments = Math.min(startIndexPayments + pageSize, totalPayments);

            List<PaymentEntity> pagedPayments = payments.subList(startIndexPayments, endIndexPayments);

            profileEditResponseDTO.setTotalPayments(totalPayments);
            profileEditResponseDTO.setTotalPaymentsPages(totalPaymentsPages);
            profileEditResponseDTO.setStartIndexPayments(startIndexPayments);
            profileEditResponseDTO.setEndIndexPayments(endIndexPayments);
            profileEditResponseDTO.setHistorysingle(pagedPayments);
            profileEditResponseDTO.setPageNumber(pageNumber);
            profileEditResponseDTO.setUsername(profileEditRequestDTO.getUsername());
            profileEditResponseDTO.setReason(profileEditRequestDTO.getReason());
            profileEditResponseDTO.setDatefrom(profileEditRequestDTO.getDatefrom());
            profileEditResponseDTO.setDateto(profileEditRequestDTO.getDateto());
            profileEditResponseDTO.setAmount(profileEditRequestDTO.getAmount());
        }
        profileEditResponseDTO.setShowSubscription(determineTab(profileEditRequestDTO.getTab()));
        return profileEditResponseDTO;
    }

    /**
     * Erstellt alle Informationen um die zusätzliche Registierungsseite anzuzeigen
     * @param registrationAdditionRequestDTO benötigte Informationen
     * @param request für die Url
     * @param session um Informationen in der Session zu speichern
     * @return DTO für den Controller
     */
    public RegistrationAdditionResponseDTO prepareRegistrationAdditionPage(RegistrationAdditionRequestDTO registrationAdditionRequestDTO, HttpServletRequest request, HttpSession session) {
        RegistrationAdditionResponseDTO registrationAdditionResponseDTO = new RegistrationAdditionResponseDTO();

        if(registrationAdditionRequestDTO.isFa()){
            registrationAdditionResponseDTO.setEmail(session.getAttribute("email").toString());
            registrationAdditionResponseDTO.setUsername(session.getAttribute("username").toString());
            registrationAdditionResponseDTO.setBirthday(session.getAttribute("birthday").toString());
        }

        session.setAttribute("uri", request.getRequestURI());
        if(userService.getCurrentUser().getProvider().equals("github"))
            registrationAdditionResponseDTO.setProvider(userService.getCurrentUser().getProvider());
        registrationAdditionResponseDTO.setUser(userService.getCurrentUser());
        registrationAdditionResponseDTO.setAuth(registrationAdditionRequestDTO.isFa());
        if(userService.getCurrentUser() != null && userService.getCurrentUser().getUsername().isEmpty())
            registrationAdditionResponseDTO.setProfileComplete(false);
        return registrationAdditionResponseDTO;
    }
}
