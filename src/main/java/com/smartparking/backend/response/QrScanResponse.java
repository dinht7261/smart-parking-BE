
package com.smartparking.backend.response;

public record QrScanResponse(

        boolean valid,

        String gateCode,

        String direction,

        String message

) {
}
