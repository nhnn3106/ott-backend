package mediaservice.mappers;

import mediaservice.dtos.requests.PostRequest;
import mediaservice.dtos.responses.PostResponse;
import mediaservice.models.Content;
import mediaservice.models.Post;
import mediaservice.models.User;
import mediaservice.models.enums.VisibilityType;
import org.springframework.stereotype.Component;

@Component
public class PostMapper {

    public Post toEntity(PostRequest request) {
        Post post = new Post();

        post.setContent(request.getContent());

        User user = new User();
        user.setId(request.getUserId());

        post.setUser(user);

        post.setVisibility(VisibilityType.valueOf(request.getVisibility()));

        post.setMetadata(request.getMetadata());

        return post;
    }

    public PostResponse toResponse(Post post) {
        if (post == null) {
            return null;
        }

        return PostResponse.builder()
                .id(post.getId())
                .content(post.getContent())
                .user(post.getUser())
                .visibility(post.getVisibility() != null ? post.getVisibility().name() : null)
                .metadata(post.getMetadata())
                .totalReactionsCount(post.getTotalReactionsCount())
                .commentsCount(post.getCommentsCount())
                .shareCount(post.getShareCount())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
