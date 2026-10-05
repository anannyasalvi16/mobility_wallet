package com.mobilitywallet.config;

import com.mobilitywallet.service.VoucherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Scheduled jobs for periodic background sweeps (e.g. midnight voucher expiration).
 */
@Configuration
@EnableScheduling
public class ScheduledTasksConfig {

    private static final Logger log = LoggerFactory.getLogger(ScheduledTasksConfig.class);

    private final VoucherService voucherService;

    public ScheduledTasksConfig(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    /**
     * Sweeps past-due vouchers daily at midnight and marks them EXPIRED.
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void sweepExpiredVouchers() {
        log.info("Running daily scheduled sweep for expired vouchers...");
        int expiredCount = voucherService.expirePastDueVouchers();
        if (expiredCount > 0) {
            log.info("Successfully transitioned {} past-due vouchers to EXPIRED state.", expiredCount);
        }
    }
}
