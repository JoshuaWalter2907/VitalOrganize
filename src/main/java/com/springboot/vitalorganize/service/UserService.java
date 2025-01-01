package com.springboot.vitalorganize.service;

import com.springboot.vitalorganize.dto.ProfileAdditionData;
import com.springboot.vitalorganize.model.*;
import com.springboot.vitalorganize.repository.*;
import com.springboot.vitalorganize.service.repositoryhelper.*;
import lombok.AllArgsConstructor;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UserService {

    private final UserRepositoryService userRepositoryService;
    private final MessageRepositoryService messageRepositoryService;
    private final DirektChatRepositoryService direktChatRepositoryService;
    private final ChatGroupRepositoryService chatGroupRepositoryService;
    private final FundRepositoryService fundRepositoryService;
    private final SubscriptionRepositoryService subscriptionRepositoryService;
    private final PaymentRepositoryService paymentRepositoryService;
    private final FriendRequestRepositoryService friendRequestRepositoryService;

    private final PaypalService paypalService;

    public UserEntity getProfileData(OAuth2User user, OAuth2AuthenticationToken authentication) {

        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com"; // Dummy-E-Mail für GitHub erstellen
        }

        UserEntity userEntity = userRepositoryService.findByEmailAndProvider(email, provider);
        if (userEntity == null) {
            throw new IllegalStateException("Benutzer nicht gefunden.");
        }
        return userEntity;

    }

    public ProfileAdditionData getProfileAdditionData(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();
        String email = user.getAttribute("email");

        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            email = username + "@github.com"; // Dummy-E-Mail erstellen
        }

        UserEntity existingUser = userRepositoryService.findByEmailAndProvider(email, provider);

        boolean isProfileComplete = existingUser != null && !existingUser.getUsername().isEmpty();
        return new ProfileAdditionData(existingUser, isProfileComplete);
    }

    public UserEntity getCurrentUser(OAuth2User user, OAuth2AuthenticationToken authentication) {
        String provider = authentication.getAuthorizedClientRegistrationId();

        String email = user.getAttribute("email");
        Long id;
        switch (provider) {
            case "google":
            case "discord":
                id = userRepositoryService.findByEmailAndProvider(email, provider).getId();
                break;

            case "github":
                String username = user.getAttribute("login");
                email = username + "@github.com";
                id = userRepositoryService.findByEmailAndProvider(email, provider).getId();
                break;

            default:
                throw new IllegalArgumentException("Unbekannter Provider: " + provider);
        }

        // Benutzer anhand der ID abrufen und zurückgeben
        return userRepositoryService.findUserById(id);
    }


    public UserEntity getUserById(Long userId) {
        return userRepositoryService.findUserById(userId);
    }

    public List<UserEntity> getPublicUsers() {
        return userRepositoryService.findPublicUsers(true);
    }

    @Transactional
    public Boolean deleteUser(UserEntity user) {

        List<FundEntity> funds = fundRepositoryService.findByAdmin(user);
        if(!funds.isEmpty()) {
            return false;
        }

        List<FundEntity> memberfunds = fundRepositoryService.findALl();
        memberfunds = memberfunds.stream()
                .filter(f -> f.getUsers().contains(user))
                .toList();

        for(FundEntity f : memberfunds) {
            f.getUsers().remove(user);
        }

        Long id = user.getId();
        List<ChatGroup> chatGroups = chatGroupRepositoryService.findAllByUserId(id);

        List<Long> chatGroupIds = chatGroups.stream()
                .map(ChatGroup::getId)
                .toList();

        if(user.getRole().equals("MEMBER"))
            paypalService.cancelSubscription(user, user.getLatestSubscription().getSubscriptionId());


        messageRepositoryService.deleteByRecipient_Id(id);
        messageRepositoryService.deleteBySender_Id(id);
        chatGroupRepositoryService.deleteAllByIdIn(chatGroupIds);
        direktChatRepositoryService.deleteById(id);
        subscriptionRepositoryService.deleteById(id);
        paymentRepositoryService.updateUserReferencesToNull(id);
        friendRequestRepositoryService.deleteById(id);
        userRepositoryService.deleteById(id);

        return true;
    }

    @Transactional
    public void blockUser(Long currentUserId, Long targetUserId) {


        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);


        if (currentUser.getBlockedUsers().contains(targetUser)) {
            throw new IllegalStateException("User is already blocked");
        }

        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        currentUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(targetUser));
        currentUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(targetUser));
        targetUser.getSentFriendRequests().removeIf(request -> request.getReceiver().equals(currentUser));
        targetUser.getReceivedFriendRequests().removeIf(request -> request.getSender().equals(currentUser));

        currentUser.getBlockedUsers().add(targetUser);

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }

    @Transactional
    public void addFriend(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        boolean existingRequestReceiver = targetUser.getReceivedFriendRequests().stream()
                .anyMatch(request -> request.getSender().equals(currentUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        boolean existingRequestSender = currentUser.getSentFriendRequests().stream()
                .anyMatch(request -> request.getReceiver().equals(targetUser) && request.getStatus() == FriendRequest.RequestStatus.PENDING);

        if (existingRequestReceiver || existingRequestSender) {
            return;
        }

        if (targetUser.isPublic()) {
            currentUser.getFriends().add(targetUser);
            targetUser.getFriends().add(currentUser);
        } else {
            FriendRequest friendRequest = new FriendRequest();
            friendRequest.setSender(currentUser);
            friendRequest.setReceiver(targetUser);
            friendRequest.setRequestDate(LocalDateTime.now());
            friendRequest.setStatus(FriendRequest.RequestStatus.PENDING); // Setze den Status auf PENDING

            currentUser.getSentFriendRequests().add(friendRequest);
        }

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }


    @Transactional
    public void unfriendUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        currentUser.getFriends().remove(targetUser);
        targetUser.getFriends().remove(currentUser);

        userRepositoryService.saveUser(currentUser);
        userRepositoryService.saveUser(targetUser);
    }

    @Transactional
    public void unblockUser(Long currentUserId, Long targetUserId) {
        UserEntity currentUser = userRepositoryService.findUserById(currentUserId);
        UserEntity targetUser = userRepositoryService.findUserById(targetUserId);

        currentUser.getBlockedUsers().remove(targetUser);

        userRepositoryService.saveUser(currentUser);
    }

    public List<UserEntity> getUsersWithFriendsOrPublic(Long userId) {
        UserEntity currentUser = userRepositoryService.findUserById(userId);

        List<UserEntity> publicUsers = userRepositoryService.findPublicUsers(true);

        List<UserEntity> friends = currentUser.getFriends();

        publicUsers.addAll(friends);

        return publicUsers.stream().distinct().collect(Collectors.toList());
    }

    public List<UserEntity> findByUsername(String query){
        return userRepositoryService.findByUsernameContaining(query);
    }

    public String getEmailForUser(OAuth2User user, String provider) {
        if ("github".equals(provider)) {
            String username = user.getAttribute("login");
            return username + "@github.com"; // Dummy-E-Mail für GitHub
        }
        return user.getAttribute("email");
    }
}