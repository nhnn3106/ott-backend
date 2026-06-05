package mediaservice.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Formula;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Table(name = "posts")

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(callSuper = true)
@DiscriminatorValue("POST")

public class Post extends Content{
    private String caption;

    @jakarta.persistence.ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @jakarta.persistence.JoinColumn(name = "shared_post_id")
    private Post sharedPost;

    @Formula("(" +
        "(" +
            "(SELECT COUNT(r.id) FROM reactions r WHERE r.target_id = id AND r.target_type = 'POST') + " +
            "(SELECT COUNT(c.id) FROM comments c WHERE c.content_id = id AND c.is_deleted = false) * 2 + " +
            "(SELECT COUNT(p2.id) FROM posts p2 WHERE p2.shared_post_id = id) * 3" +
        ")" +
        " / " +
        "POWER(GREATEST((EXTRACT(EPOCH FROM (CURRENT_TIMESTAMP - (SELECT c.created_at FROM contents c WHERE c.id = id))) / 3600.0), 0) + 2.0, 1.5)" +
    ")")
    private double viralScore = 0.0;
}
