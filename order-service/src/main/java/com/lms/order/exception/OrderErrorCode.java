package com.lms.order.exception;

import com.lms.common.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum OrderErrorCode implements ErrorCode {
    PROMOTION_NOT_FOUND (6001, "Promotion not found", HttpStatus.NOT_FOUND),
    PROMOTION_INACTIVE (6002, "Promotion inactive", HttpStatus.FORBIDDEN),
    PROMOTION_NOT_APPLICABLE (6003, "Promotion not applicable", HttpStatus.BAD_REQUEST),
    PROMOTION_LIMIT_USAGE_EXCEEDED (6004, "Promotion limit exceeded", HttpStatus.BAD_REQUEST),
    PROMOTION_NOT_STARTED_YET (6005, "Promotion not started yet", HttpStatus.BAD_REQUEST),
    PROMOTION_EXPIRED (6006, "Promotion has expired", HttpStatus.BAD_REQUEST),

    CURRENCY_NOT_SUPPORTED (6007, "Currency not supported", HttpStatus.BAD_REQUEST),
    PAYMENT_NOT_CONNECTED (6008, "Payment not connected", HttpStatus.BAD_REQUEST),
    ORDER_NOT_FOUND (6009, "Order not found", HttpStatus.NOT_FOUND),
    COURSE_ALREADY_PURCHASED (6010, "Bạn đã sở hữu khóa học này rồi", HttpStatus.BAD_REQUEST),
    REFUND_NOT_FOUND (6011, "Không tìm thấy yêu cầu hoàn tiền", HttpStatus.NOT_FOUND),
    REFUND_EXPIRED (6012, "Đã quá thời hạn khiếu nại hoàn tiền 3 ngày theo quy định", HttpStatus.BAD_REQUEST),
    REFUND_ALREADY_EXISTS (6013, "Đơn hàng này đã có yêu cầu hoàn tiền đang được xử lý", HttpStatus.BAD_REQUEST),
    ORDER_NOT_PAID (6014, "Chỉ đơn hàng đã thanh toán thành công mới có thể khiếu nại hoàn tiền", HttpStatus.BAD_REQUEST),
    ;
    private final int code;
    private final String message;
    private final HttpStatus httpStatus;
}
