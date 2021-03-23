package com.dizsun.timechain.util;

import com.dizsun.timechain.component.PubPriKey;
import com.dizsun.timechain.service.PersistenceService;

public class RSAUtil {

    private PubPriKey pubPriKey;

    private PersistenceService persistenceService = PersistenceService.getInstance();

    private RSAUtil() {
    }

    private static class Holder{
        private static final RSAUtil rsaUtil = new RSAUtil();
    }

    public static RSAUtil getInstance() {
        return Holder.rsaUtil;
    }

    public PubPriKey getPubPriKey() {
        return pubPriKey;
    }

    public void setPubPriKey(PubPriKey pubPriKey) {
        this.pubPriKey = pubPriKey;
    }

    public void init(String localHost) {
        pubPriKey = persistenceService.pubPriKeysUpload(localHost);
        if (pubPriKey == null) {
            pubPriKey = new PubPriKey();
            pubPriKey.init();
            persistenceService.pubPriKeysPersistence(localHost, pubPriKey);
        }
    }
}
