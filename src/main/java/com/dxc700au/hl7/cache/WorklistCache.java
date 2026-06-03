package com.dxc700au.hl7.cache;

import com.dxc700au.hl7.dto.SampleResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
public class WorklistCache {

    /*
     * ============================================================
     * CACHE STORAGE
     * ============================================================
     */
    private final Map<String, CacheEntry> cache =
            new ConcurrentHashMap<>();

    /*
     * ============================================================
     * CACHE EXPIRY
     * ============================================================
     */
    private static final long CACHE_EXPIRY_SECONDS =
            3600;

    /*
     * ============================================================
     * STORE SAMPLE
     * ============================================================
     */
    public void put(
            String barcode,
            SampleResponse sample
    ) {

        if (barcode == null
                || barcode.isBlank()
                || sample == null) {

            return;
        }

        String cleanBarcode =
                barcode.trim();

        cache.put(
                cleanBarcode,
                new CacheEntry(
                        sample,
                        Instant.now()
                )
        );

        log.info(
                """
                        
                        WORKLIST CACHE UPDATED
                        barcode={}
                        sampleNumber={}
                        totalTests={}
                        currentCacheSize={}
                        """,
                cleanBarcode,
                sample.getSampleNumber(),
                sample.getParamCodes() == null
                        ? 0
                        : sample.getParamCodes().size(),
                cache.size()
        );
    }

    /*
     * ============================================================
     * GET SAMPLE
     * ============================================================
     */
    public SampleResponse get(
            String barcode
    ) {

        cleanup();

        if (barcode == null
                || barcode.isBlank()) {

            return null;
        }

        String cleanBarcode =
                barcode.trim();

        CacheEntry entry =
                cache.get(cleanBarcode);
        log.info("""
        
        CACHE LOOKUP
        
        requestedBarcode={}
        cacheSize={}
        availableKeys={}
        
        """,
                cleanBarcode,
                cache.size(),
                cache.keySet()
        );
        if (entry == null) {

            log.warn(
                    "CACHE MISS barcode={}",
                    cleanBarcode
            );

            return null;
        }

        log.info(
                """
                        
                        CACHE HIT
                        barcode={}
                        sampleNumber={}
                        """,
                cleanBarcode,
                entry.sample().getSampleNumber()
        );

        return entry.sample();
    }

    /*
     * ============================================================
     * EXISTS
     * ============================================================
     */
    public boolean contains(
            String barcode
    ) {

        cleanup();

        return cache.containsKey(
                barcode
        );
    }

    /*
     * ============================================================
     * CACHE SIZE
     * ============================================================
     */
    public int size() {

        cleanup();

        return cache.size();
    }

    /*
     * ============================================================
     * CLEAR CACHE
     * ============================================================
     */
    public void clear() {

        cache.clear();

        log.warn(
                "WORKLIST CACHE CLEARED"
        );
    }

    /*
     * ============================================================
     * REMOVE EXPIRED
     * ============================================================
     */
    private void cleanup() {

        Instant now =
                Instant.now();

        cache.entrySet()
                .removeIf(entry -> {

                    Instant created =
                            entry.getValue()
                                    .createdAt();

                    boolean expired =
                            now.minusSeconds(
                                            CACHE_EXPIRY_SECONDS
                                    )
                                    .isAfter(created);

                    if (expired) {

                        log.warn(
                                "CACHE ENTRY EXPIRED barcode={}",
                                entry.getKey()
                        );
                    }

                    return expired;
                });
    }

    /*
     * ============================================================
     * CACHE ENTRY
     * ============================================================
     */
    private record CacheEntry(
            SampleResponse sample,
            Instant createdAt
    ) {
    }
}