# EduTech LMS — Backend Microservices

Hệ thống Backend Microservices cho nền tảng đào tạo trực tuyến EduTech LMS.

- **Công nghệ chính:** Java 21, Spring Boot 3.2.4, Spring Cloud 2023.0.1, Maven (13 modules).
- **Hạ tầng:** MySQL, Redis, RabbitMQ, MinIO.
- **Sổ tay hướng dẫn chi tiết:** Xem file [HUONG-DAN-SU-DUNG.md](../HUONG-DAN-SU-DUNG.md) tại thư mục gốc của workspace.

---

## 1. Danh sách Module & Cổng dịch vụ

| Module | Cổng (Port) | Chức năng chính |
|---|---|---|
| `eureka-server` | 8761 | Service Discovery & Registry |
| `config-server` | 8888 | Cấu hình tập trung (`shared-configs/*.yml`) |
| `api-gateway` | 8080 | Routing, JWT Authentication, Header Injection |
| `iam-service` | 8001 | Xác thực, Quản lý tài khoản, Phân quyền, Redis Session |
| `course-service` | 8002 | Khóa học, Chương, Bài học, Kiểm duyệt khóa |
| `enrollment-service` | 8006 | Ghi danh, Tiến độ học, Chấm điểm Quiz server |
| `notification-service` | 8008 | Thông báo SSE, Cấp phát & Thẩm định chứng chỉ số |
| `order-service` | — | Đơn hàng, thanh toán VNPay |
| `finance-service` | — | Quyết toán doanh thu, số dư giảng viên |
| `media-service` | — | Tải file đa phương tiện MinIO |
| `video-worker-service` | — | Xử lý video HLS ngầm |
| `lms-common` | — | DTO dùng chung, Exception Handler, Security Filter |

---

## 2. Hướng dẫn Biên dịch & Chạy

### Biên dịch (Offline mode):
```powershell
.\mvnw.cmd -o -DskipTests compile
```

### Đóng gói kèm kiểm thử:
```powershell
.\mvnw.cmd -o package
```

### Chạy dịch vụ tối ưu RAM:
```powershell
java -Xmx300m -jar <module>/target/<module>-1.0.0.jar
```

---

## 3. Tài khoản kiểm thử mặc định
Mật khẩu chung: `KiemThu@123`
- **Học viên:** `kiemthu.gd1@lms.com`
- **Giảng viên:** `giangvien@lms.com`
- **Quản trị viên:** `admin@lms.com`
