package com.mobilitywallet.service;

import com.mobilitywallet.entity.Voucher;

public interface QrCodeService {

    /**
     * Generates a QR code image as a Base64 data URI string suitable for direct HTML rendering:
     * e.g. "data:image/png;base64,iVBORw0KGgoAAA..."
     */
    String generateQrCodeDataUri(String text, int width, int height);

    /**
     * Generates raw PNG bytes for a QR code image.
     */
    byte[] generateQrCodeImage(String text, int width, int height);

    /**
     * Formats a structured verification payload for a voucher.
     */
    String generateVoucherPayload(Voucher voucher);
}
