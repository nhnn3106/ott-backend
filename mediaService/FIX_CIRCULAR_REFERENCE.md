# Fix: ConcurrentModificationException & Circular Reference

## Vấn đề đã được sửa

1. **ConcurrentModificationException** - Xảy ra khi Jackson serialize các Hibernate lazy collections
2. **Circular Reference** - User ↔ OfficialAccount ↔ User tạo vòng lặp vô tận
3. **Lazy Loading Issues** - Collections chưa được load khi serialize

## Các thay đổi

### 1. Thêm `@JsonIgnore` cho các relationships
- `User.officialAccounts` - Không serialize collection này
- `OfficialAccount.user` - Không serialize owner
- `OfficialAccount.OAFollowers` - Không serialize followers
- `Content.user` - Không serialize owner
- `Content.mentionedUsers` - Không serialize mentioned users
- `Content.reactions` - Không serialize reactions
- `Content.comments` - Không serialize comments
- `Post.tags` - Không serialize tags

### 2. Cập nhật PostResponse
Thay vì serialize toàn bộ `User` object, chỉ serialize các field cần thiết:
```java
private String userId;
private String username;
private String avatarUrl;
private Map<String, Object> metadata; // Thêm để hiển thị images
```

### 3. Cấu hình Hibernate
Thêm vào `application.properties`:
```properties
spring.jpa.open-in-view=false
```

## Metadata Structure

Metadata chứa thông tin về images của post:
```json
{
  "images": [
    {
      "imageUrl": "https://images.unsplash.com/photo-1506905925346-21bda4d32df4",
      "caption": "Beautiful mountain landscape at sunset"
    },
    {
      "imageUrl": "https://images.unsplash.com/photo-1511593358241-7eea1f3c84e5",
      "caption": "Crystal clear lake reflection"
    }
  ]
}
```

## Response Example

```json
{
  "id": "post-001",
  "content": "Amazing weekend trip to the mountains!",
  "userId": "user-001",
  "username": "john_doe",
  "avatarUrl": "https://i.pravatar.cc/150?img=1",
  "visibility": "PUBLIC",
  "metadata": {
    "images": [
      {
        "imageUrl": "https://images.unsplash.com/photo-1506905925346-21bda4d32df4",
        "caption": "Beautiful mountain landscape at sunset"
      }
    ]
  },
  "totalReactionsCount": 45,
  "commentsCount": 5,
  "shareCount": 12,
  "createdAt": "2026-01-15T10:30:00",
  "updatedAt": "2026-01-16T14:20:00"
}
```

## Test API

```bash
curl -X GET http://localhost:8080/media/api/posts
```

Bây giờ API sẽ trả về dữ liệu đầy đủ bao gồm metadata với images!

