package com.securetask.backend.document;

import org.springframework.core.io.Resource;

record DocumentDownload(
        Resource resource,
        String originalFilename,
        String contentType,
        long sizeBytes) {
}
