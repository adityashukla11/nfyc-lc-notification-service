package com.nfyc.lcnotificationservice.domain;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;

@AllArgsConstructor
public class NfycLcResponse {
    public JsonNode data;
}
