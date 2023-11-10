package org.avni_integration_service.lahi.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.avni_integration_service.common.PlatformException;
import org.avni_integration_service.common.MessageUnprocessableException;
import org.avni_integration_service.glific.bigQuery.domain.FlowResult;
import org.avni_integration_service.lahi.util.DateTimeUtil;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.Period;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Student implements LahiStudentConstants {
    private static final List<String> MandatoryFields =
            Arrays.asList(
                    FIRST_NAME,
                    LAST_NAME,
                    DATE_OF_BIRTH,
                    GENDER
            );

    private static final List<String> OtherPrimitiveFields = Arrays.asList(EMAIL, OTHER_QUALIFICATION, FATHER_NAME, QUALIFICATION_STREAM);
    private static final List<String> OtherCodedFields = Arrays.asList(HIGHEST_QUALIFICATION,
            QUALIFICATION_STATUS, ACADEMIC_YEAR, VOCATIONAL, TRADE, STREAM);
    private static final List<String> Genders = Arrays.asList("Male", "Female", "Other");

    private final FlowResult flowResult;

    public Student(FlowResult flowResult) {
        this.flowResult = flowResult;
    }

    public String getContactPhone() {
        return flowResult.getContactPhone();
    }

    public String getAlternatePhoneNumber() {
        return getInput(ALTERNATE_NUMBER);
    }

    public String getFirstName() {
        return getInput(FIRST_NAME);
    }

    public String getLastName() {
        return getInput(LAST_NAME);
    }

    public String getGender() {
        return getCategory(GENDER);
    }

    public String getDateOfRegistration() {
        return getInsertedAt();
    }

    public String getDateOfBirth() {
        return getInput(DATE_OF_BIRTH);
    }

    public String getInput(String key) {
        return flowResult.getInput(key);
    }

    public String getCategory(String key) {
        return flowResult.getCategory(key);
    }

    public String getLastUpdatedAt() {
        return flowResult.getUpdatedAt();
    }

    public String getInsertedAt() {
        return flowResult.getInsertedAt();
    }

    public String getFlowResultId() {
        return flowResult.getFlowResultId();
    }

    public Map<String, Object> getObservations() {
        HashMap<String, Object> observations = new HashMap<>();

        OtherPrimitiveFields.forEach(fieldName -> observations.put(fieldName, getInput(fieldName)));
        OtherCodedFields.forEach(fieldName -> observations.put(fieldName, getCategory(fieldName)));

        return observations;
    }

    public void validate() throws PlatformException, MessageUnprocessableException {
        if (!this.flowResult.isComplete())
            throw new MessageUnprocessableException(String.format("FlowResultId: %s. Message is incomplete.", this.flowResult.getFlowResultId()));

        List<String> missingFields = MandatoryFields.stream().filter(field -> !StringUtils.hasLength(getInput(field))).collect(Collectors.toList());
        if (missingFields.size() > 0)
            throw new PlatformException(String.format("FlowResultId: %s. Missing fields: %s", this.flowResult.getFlowResultId(), String.join(",", missingFields)));
        if (!Genders.contains(getGender()))
            throw new PlatformException(String.format("FlowResultId: %s. Gender value is wrong: %s", this.flowResult.getFlowResultId(), getGender()));
        boolean ageLessThan14 = Period.between(Objects.requireNonNull(DateTimeUtil.toLocalDate(getDateOfBirth(), DateTimeUtil.DD_MM_YYYY)), LocalDate.now()).getYears() < 14;
        if (ageLessThan14)
            throw new MessageUnprocessableException(String.format("FlowResultId: %s. Age is less than 14: %s", this.flowResult.getFlowResultId(), getDateOfBirth()));
    }
}
