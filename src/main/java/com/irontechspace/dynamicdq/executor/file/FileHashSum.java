package com.irontechspace.dynamicdq.executor.file;

import lombok.extern.log4j.Log4j2;
import lombok.val;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static com.irontechspace.dynamicdq.exceptions.ExceptionUtils.logException;

@Log4j2
@Component
public class FileHashSum {

    private static final String MD5_ALGORITHM = "MD5";

    private MessageDigest messageDigest;

    public String getHashSum(byte[] fileContent) {
        initMessageDigest();
        return createChecksum(createHash(fileContent));
    }

    private void initMessageDigest() {
        try {
            messageDigest = MessageDigest.getInstance(MD5_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            logException(log, e);
//            log.error("Error while counting file", e);
        }
    }

    private String createChecksum(byte[] hash) {
        val checkSum = new StringBuilder();
        for (byte aHash : hash) {
            if ((0xff & aHash) < 0x10) {
                checkSum.append("0").append(Integer.toHexString((0xFF & aHash)));
            } else {
                checkSum.append(Integer.toHexString(0xFF & aHash));
            }
        }
        return checkSum.toString();
    }

    private byte[] createHash(byte[] fileContent) {
        messageDigest.update(fileContent);
        return messageDigest.digest();
    }
}
