package mediaservice.services.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mediaservice.dtos.requests.PostRequest;
import mediaservice.dtos.responses.PostResponse;
import mediaservice.mappers.PostMapper;
import mediaservice.models.Post;
import mediaservice.models.User;
import mediaservice.models.enums.VisibilityType;
import mediaservice.repositories.PostRepository;
import mediaservice.repositories.UserRepository;
import mediaservice.services.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final PostMapper postMapper;

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PostResponse> getAllPosts() {
        log.info("Fetching all posts");
        List<Post> posts = postRepository.findAll();
        log.info("Found {} posts", posts.size());

        return posts.stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    @Override
    public List<PostResponse> getAllPostsByUserId(String userId) {
        log.info("Fetching all posts by user id: " + userId);

        User user = new User();
        user.setId(userId);
        List<Post> posts = postRepository.findByUser(user);
        log.info("Found {} posts", posts.size());

        return posts.stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponse getPostById(String id) {
        log.info("Fetching post with id: {}", id);
        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        return postMapper.toResponse(post);
    }

    @Override
    @Transactional
    public PostResponse createPost(PostRequest request) {
        log.info("Creating new post for user: {}", request.getUserId());

        Post post = postMapper.toEntity(request);
        Post savedPost = postRepository.save(post);

        log.info("Post created with id: {}", savedPost.getId());

        User user = userRepository.findById(savedPost.getUser().getId()).orElse(null);
        savedPost.setUser(user);
        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    public PostResponse updatePost(String id, PostRequest request) {
        log.info("Updating post with id: {}", id);

        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        // Update fields
        existingPost.setContent(request.getContent());
        if (request.getVisibility() != null) {
            existingPost.setVisibility(
                VisibilityType.valueOf(request.getVisibility().toUpperCase())
            );
        }

        if(request.getMetadata() != null) {
            existingPost.setMetadata(request.getMetadata());
        }

        Post updatedPost = postRepository.save(existingPost);
        log.info("Post updated successfully: {}", id);

        return postMapper.toResponse(updatedPost);
    }

    @Override
    @Transactional
    public void deletePost(String id) {
        log.info("Deleting post with id: {}", id);

        Post post = postRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Post not found with id: " + id));

        postRepository.delete(post);
        log.info("Post deleted successfully: {}", id);
    }
}
