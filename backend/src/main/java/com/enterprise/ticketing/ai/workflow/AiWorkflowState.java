package com.enterprise.ticketing.ai.workflow;

import com.enterprise.ticketing.ai.dto.AiCitation;
import com.enterprise.ticketing.ai.provider.AiClassificationOutput;
import com.enterprise.ticketing.ai.provider.AiResolutionOutput;
import com.enterprise.ticketing.ticket.dto.TicketResponse;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AiWorkflowState {

    private final String workflowId;
    private final TicketResponse ticket;
    private AiClassificationOutput classification;
    private Map<String, String> extractedFields = new LinkedHashMap<>();
    private List<AiCitation> citations = new ArrayList<>();
    private AiResolutionOutput resolution;

    public AiWorkflowState(String workflowId, TicketResponse ticket) {
        this.workflowId = workflowId;
        this.ticket = ticket;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public TicketResponse getTicket() {
        return ticket;
    }

    public AiClassificationOutput getClassification() {
        return classification;
    }

    public void setClassification(AiClassificationOutput classification) {
        this.classification = classification;
    }

    public Map<String, String> getExtractedFields() {
        return extractedFields;
    }

    public void setExtractedFields(Map<String, String> extractedFields) {
        this.extractedFields = extractedFields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(extractedFields);
    }

    public List<AiCitation> getCitations() {
        return citations;
    }

    public void setCitations(List<AiCitation> citations) {
        this.citations = citations == null ? new ArrayList<>() : new ArrayList<>(citations);
    }

    public AiResolutionOutput getResolution() {
        return resolution;
    }

    public void setResolution(AiResolutionOutput resolution) {
        this.resolution = resolution;
    }
}
