## ***Xử lý lỗi thực tế từ bảng failed_sync_events***

Xây dựng API quản lý (Admin API): Tạo 2 endpoint: GET để lấy danh sách lỗi (status = 'FAILED') và POST /retry/{id} để chạy lại.

Logic hàm Retry:

Đọc bản ghi từ DB dựa vào ID.

Dùng RabbitTemplate gửi lại chính xác chuỗi payload vào exchangeName và routingKey đã lưu.

Cập nhật status = 'RESOLVED' sau khi gửi thành công.

Cơ chế tự động hóa (Tùy chọn): Viết một hàm dùng @Scheduled(cron = "0 0 2 * * ?") để quét và tự động retry các message FAILED vào lúc 2h sáng (áp dụng cho các lỗi rớt mạng tạm thời). Lỗi sai logic dữ liệu sẽ phải review tay.