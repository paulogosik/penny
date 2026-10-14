package com.penny.scheduler;

import com.penny.service.RecurringEntryGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "penny.scheduling.enabled", havingValue = "true", matchIfMissing = true)
public class RecurringEntryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecurringEntryScheduler.class);

    private final RecurringEntryGenerator generator;

    public RecurringEntryScheduler(RecurringEntryGenerator generator) {
        this.generator = generator;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        run();
    }

    @Scheduled(cron = "0 0 * * * *")
    public void hourly() {
        run();
    }

    private void run() {
        try {
            int created = generator.generateDueEntries();
            if (created > 0) {
                log.info("{} lançamento(s) recorrente(s) gerado(s).", created);
            }
        } catch (RuntimeException e) {
            log.error("Falha ao gerar lançamentos recorrentes.", e);
        }
    }
}
