package com.mobilitywallet.service.impl;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.mobilitywallet.entity.Voucher;
import com.mobilitywallet.exception.BusinessException;
import com.mobilitywallet.service.QrCodeService;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class QrCodeServiceImpl implements QrCodeService {

    @Override
    public String generateQrCodeDataUri(String text, int width, int height) {
        byte[] pngBytes = generateQrCodeImage(text, width, height);
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(pngBytes);
    }

    @Override
    public byte[] generateQrCodeImage(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name());
            hints.put(EncodeHintType.MARGIN, 1); // Clean, compact border

            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height, hints);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new BusinessException("Failed to generate QR code: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateVoucherPayload(Voucher voucher) {
        Long assignedUid = voucher.getAssignedTo() != null ? voucher.getAssignedTo().getId() : null;
        return String.format(
                "{\"type\":\"VOUCHER\",\"code\":\"%s\",\"value\":%s,\"expiry\":\"%s\",\"uid\":%s}",
                voucher.getCode(),
                voucher.getInitialValue(),
                voucher.getExpiryDate(),
                assignedUid
        );
    }
}
