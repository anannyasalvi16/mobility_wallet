package com.mobilitywallet.exception;

public class VoucherAlreadyRedeemedException extends BusinessException {

    public VoucherAlreadyRedeemedException(String message) {
        super(message);
    }
}
