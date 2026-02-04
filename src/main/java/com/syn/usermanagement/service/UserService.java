package com.syn.usermanagement.service;

import com.syn.usermanagement.entity.User;
import com.syn.usermanagement.repository.UserRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final Optional<S3Service> s3Service;

    // Constructor
    public UserService(UserRepository userRepository, Optional<S3Service> s3Service) {
        this.userRepository = userRepository;
        this.s3Service = s3Service;
    }

    /**
     * Get all users (pagination)
     */
    public Page<User> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable);
    }

    /**
     * Get all users (no pagination)
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user by id
     */
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id " + id));
    }

    /**
     * Create new user
     */
    public User createUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Update existing user
     */
    public User updateUser(Long id, User updatedUser) {
        User existingUser = getUserById(id);
        existingUser.setName(updatedUser.getName());
        existingUser.setEmail(updatedUser.getEmail());
        return userRepository.save(existingUser);
    }

    /**
     * Delete user
     */
    public void deleteUser(Long id) {
        userRepository.deleteById(id);
    }

    /**
     * Upload user photo (S3 optional)
     */
    public User uploadUserPhoto(Long userId, MultipartFile file) throws IOException {

        if (s3Service.isEmpty()) {
            throw new IllegalStateException("S3 is not configured on this server");
        }

        User user = getUserById(userId);

        // Upload new photo
        String photoUrl = s3Service.get().uploadFile(file, userId);

        // Delete old photo if exists
        if (user.getPhotoUrl() != null && !user.getPhotoUrl().isEmpty()) {
            s3Service.get().deleteFile(user.getPhotoUrl());
        }

        user.setPhotoUrl(photoUrl);
        return userRepository.save(user);
    }

    /**
     * Delete user photo (S3 optional)
     */
    public User deleteUserPhoto(Long userId) {
        User user = getUserById(userId);

        if (user.getPhotoUrl() == null || user.getPhotoUrl().isEmpty()) {
            return user;
        }

        // Delete from S3 if configured
        if (s3Service.isPresent()) {
            s3Service.get().deleteFile(user.getPhotoUrl());
        }

        user.setPhotoUrl(null);
        return userRepository.save(user);
    }
}
