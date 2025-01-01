package com.springboot.vitalorganize.service.repositoryhelper;

import com.springboot.vitalorganize.model.DirectChat;
import com.springboot.vitalorganize.repository.DirectChatRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class DirektChatRepositoryService {

    private final DirectChatRepository directChatRepository;

    public DirectChat findDirectChatBetweenUsers(Long user1Id, Long user2Id) {
        DirectChat directChat = directChatRepository.findByUser1IdAndUser2Id(user1Id, user2Id);
        if (directChat == null) {
            directChat = directChatRepository.findByUser2IdAndUser1Id(user1Id, user2Id);
        }
        return directChat;
    }

    public List<DirectChat> findDirectChats(Long user1Id) {
        return directChatRepository.findByUser1IdOrUser2Id(user1Id, user1Id);
    }

    public void saveDirectChat(DirectChat directChat) {
        directChatRepository.save(directChat);
    }

    public Optional<DirectChat> findById(Long chatId){
        return directChatRepository.findById(chatId);
    }

    public void deleteDirectChat(DirectChat chat) {
        directChatRepository.delete(chat);
    }

    public void deleteById(Long id) {
        directChatRepository.deleteById(id);
    }
}
