package com.mobilitywallet.service;

import com.mobilitywallet.dto.request.TaxiRedeemRequest;
import com.mobilitywallet.dto.response.RideResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service contract for taxi ride settlements via mobility vouchers,
 * verifying QR payload, preventing double-redemption, and logging audit ledger entries.
 */
public interface RideService {

    /**
     * Atomically redeems an assigned voucher for a verified taxi ride.
     * Transitions voucher to REDEEMED, records Ride entity, and logs DEBIT ledger entry.
     * Protected by @Version optimistic locking on Voucher.
     *
     * @param request the ride redemption details
     * @return the saved ride details
     */
    RideResponse redeemVoucher(TaxiRedeemRequest request);

    /**
     * Fetches ride details by unique Ride ID.
     *
     * @param id ride ID
     * @return ride response
     */
    RideResponse getRideById(Long id);

    /**
     * Fetches ride details associated with a specific voucher code.
     *
     * @param voucherCode alphanumeric voucher code
     * @return ride response
     */
    RideResponse getRideByVoucherCode(String voucherCode);

    /**
     * Retrieves all taxi rides completed by a specific user.
     *
     * @param userId user ID
     * @return list of rides
     */
    List<RideResponse> getRidesByUserId(Long userId);

    /**
     * Retrieves all platform taxi rides with pagination for administrative monitoring.
     *
     * @param pageable pagination parameters
     * @return page of rides
     */
    Page<RideResponse> getAllRides(Pageable pageable);
}
