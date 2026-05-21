package com.lms.finance.enums;

/**
 * Loại giao dịch biến động số dư trong sổ cái (balance_histories).
 * <p>
 * Quy ước amount:
 *   DƯƠNG = cộng vào available_balance
 *   ÂM    = trừ khỏi available_balance
 */
public enum TransactionType {
    /** Cộng tiền sau khi học viên mua khóa học */
    DEPOSIT_FROM_ORDER,

    /** Khóa tiền khi tạo lệnh rút → trừ available, cộng blocked */
    BLOCK_FOR_PAYOUT,

    /** Trừ hẳn blocked sau khi Admin duyệt rút thành công */
    WITHDRAW_SUCCESS,

    /** Hoàn tiền khi Admin từ chối rút → trừ blocked, cộng lại available */
    WITHDRAW_REJECTED,

    /** Trừ tiền khi đơn hàng bị hoàn (refund) */
    REFUND_DEDUCTION
}
