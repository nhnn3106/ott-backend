# Hướng dẫn chạy OTT Backend

## Những gì đã được sửa:

### 1. **application.properties**
- ✅ Đã thêm giá trị mặc định cho tất cả biến môi trường
- ✅ Đã sửa `spring.jpa.hibernate.ddl-auto` từ `update` → `validate` để tương thích với Flyway
- ✅ Đã sửa `spring.jpa.generate-ddl` từ `true` → `false`
- ✅ Đã giảm logging level từ DEBUG → INFO để chạy nhanh hơn
- ✅ Đã cập nhật logging properties để không bị deprecated

### 2. **Flyway Migration**
- ✅ Đã tạo thư mục `src/main/resources/db/migration`
- ✅ Đã tạo file `V1__Initial_schema.sql` từ file `create.sql`

### 3. **Thư mục uploads**
- ✅ Đã tạo thư mục `uploads` cho file upload

### 4. **File .env.example**
- ✅ Đã tạo file `.env.example` để hướng dẫn cấu hình

## Cách chạy ứng dụng:

### Bước 1: Đảm bảo PostgreSQL đang chạy
```bash
# Kiểm tra PostgreSQL đang chạy
# Tạo database nếu chưa có
psql -U postgres
CREATE DATABASE ottdb;
\q
```

### Bước 2: (Tùy chọn) Cấu hình biến môi trường
Nếu muốn override các giá trị mặc định, tạo file `.env` hoặc set biến môi trường:

**Windows PowerShell:**
```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/ottdb"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your_password"
$env:JWT_SECRET="your-very-long-secret-key-at-least-256-bits"
```

**Hoặc tạo file `.env`** (cần plugin Spring Boot):
```
DB_URL=jdbc:postgresql://localhost:5432/ottdb
DB_USERNAME=postgres
DB_PASSWORD=your_password
JWT_SECRET=your-very-long-secret-key-at-least-256-bits
```

### Bước 3: Chạy ứng dụng

**Cách 1: Dùng Maven Wrapper**
```bash
cd D:\DaiHoc\CongNgheMoi\project\ott-backend\user-service
./mvnw spring-boot:run
```

**Cách 2: Build và chạy JAR**
```bash
cd D:\DaiHoc\CongNgheMoi\project\ott-backend\user-service
./mvnw clean package -DskipTests
java -jar target/SpringBoot2-0.0.1-SNAPSHOT.jar
```

### Bước 4: Kiểm tra ứng dụng
Mở browser và truy cập:
- Health check: http://localhost:8080/riff/api/actuator/health
- API base: http://localhost:8080/riff/api

## Các giá trị mặc định hiện tại:

| Biến | Giá trị mặc định |
|------|------------------|
| DB_URL | jdbc:postgresql://localhost:5432/ottdb |
| DB_USERNAME | postgres |
| DB_PASSWORD | postgres |
| JWT_SECRET | your-secret-key-at-least-256-bits-long-for-hs256-algorithm-security |
| REDIS_HOST | localhost |
| REDIS_PORT | 6379 |
| FRONTEND_URL | http://localhost:5173 |

## Lưu ý:

1. **Database**: Đảm bảo PostgreSQL đang chạy và database `ottdb` đã được tạo
2. **Redis**: Nếu dùng Redis, cần start Redis server
3. **Email**: Cần cấu hình `MAIL_USERNAME` và `MAIL_PASSWORD` nếu muốn gửi email
4. **Google OAuth2**: Cần cấu hình `GOOGLE_CLIENT_ID` và `GOOGLE_CLIENT_SECRET` để login bằng Google
5. **Cloudinary**: Cần cấu hình Cloudinary credentials để upload ảnh

## Nếu gặp lỗi:

### Lỗi kết nối database:
```
Error: Could not connect to database
```
→ Kiểm tra PostgreSQL đã chạy chưa và thông tin kết nối đúng chưa

### Lỗi Flyway migration:
```
Error: Flyway migration failed
```
→ Xóa database và tạo lại, hoặc chạy lệnh:
```sql
DROP DATABASE ottdb;
CREATE DATABASE ottdb;
```

### Lỗi Redis:
```
Error: Unable to connect to Redis
```
→ Nếu không dùng Redis, có thể comment cache configuration hoặc start Redis

### Port đã được sử dụng:
```
Error: Port 8080 already in use
```
→ Đổi port trong `application.properties`:
```properties
server.port=8081
```

## Build thành công! ✅
```
[INFO] BUILD SUCCESS
[INFO] Total time:  9.332 s
```

Ứng dụng đã sẵn sàng để chạy!
